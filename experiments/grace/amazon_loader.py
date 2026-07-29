"""
Derive an ABAC (Entity/Rule) instance from the real Amazon Employee Access
dataset (datasets/amazon-employee-access/amazon_employee_access.csv).

The raw data has no ABAC structure by itself -- it's a flat access log with
columns RESOURCE, MGR_ID, ROLE_ROLLUP_1, ROLE_ROLLUP_2, ROLE_DEPTNAME,
ROLE_TITLE, ROLE_FAMILY_DESC, ROLE_FAMILY, ROLE_CODE, target (1=approved).
Notably there is no persistent per-employee ID column at all -- identity in
this dataset *is* the role-attribute tuple, which is actually a natural fit
for ABAC's own philosophy (Section 3.1: a subject is nothing but a set of
attribute name-value pairs).

Derivation (documented, so results can be audited against this choice):
  - One synthetic "role-class" user per distinct 7-tuple of
    (ROLE_ROLLUP_1, ROLE_ROLLUP_2, ROLE_DEPTNAME, ROLE_TITLE,
     ROLE_FAMILY_DESC, ROLE_FAMILY, ROLE_CODE). MGR_ID is dropped from the
    role tuple: it is near-unique per manager and would fragment the policy
    set into near-singletons rather than generalizable role classes.
  - One resource entity per distinct RESOURCE id. The dataset gives no
    resource-side attributes at all, so each resource's only attribute is
    its own id (`rid`) -- policies can only condition on resource identity
    directly (`rid in {...}`), not on resource properties. This is a real
    expressiveness gap in the source data, not an artifact of the parser;
    flagged here and in datasets/README.md.
  - One policy per distinct role-tuple that has >=1 approved (target==1)
    row: subject condition = exact match on all 7 role columns; resource
    condition = `rid in {resource ids that role-tuple was approved for}`;
    single generic operation "access" (the dataset is binary
    approved/not-approved, with no read/write/etc. granularity).
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List

import pandas as pd

from .model import Entity, LiteralConjunct, Rule

ROLE_COLS = [
    "ROLE_ROLLUP_1", "ROLE_ROLLUP_2", "ROLE_DEPTNAME",
    "ROLE_TITLE", "ROLE_FAMILY_DESC", "ROLE_FAMILY", "ROLE_CODE",
]


@dataclass
class AmazonPolicy:
    users: Dict[str, Entity]
    resources: Dict[str, Entity]
    rules: List[Rule]


def load_amazon_as_abac(csv_path: str | Path, approved_only: bool = True) -> AmazonPolicy:
    df = pd.read_csv(csv_path)
    target_col = "target" if "target" in df.columns else "ACTION"
    if approved_only:
        df = df[df[target_col] == 1]

    resources: Dict[str, Entity] = {}
    for rid in df["RESOURCE"].astype(str).unique():
        resources[rid] = Entity(id=rid, attrs={"rid": rid})

    users: Dict[str, Entity] = {}
    rules: List[Rule] = []
    grouped = df.groupby(ROLE_COLS, dropna=False)
    for i, (role_key, group) in enumerate(grouped):
        uid = f"role_{i}"
        attrs = {col: str(val) for col, val in zip(ROLE_COLS, role_key)}
        attrs["uid"] = uid
        users[uid] = Entity(id=uid, attrs=attrs)

        res_ids = frozenset(group["RESOURCE"].astype(str).unique())
        sub_cond = [LiteralConjunct(attr=col, op="in", rhs=frozenset({str(val)}))
                    for col, val in zip(ROLE_COLS, role_key)]
        res_cond = [LiteralConjunct(attr="rid", op="in", rhs=res_ids)]
        rules.append(Rule(id=f"P_{uid}", sub_cond=sub_cond, res_cond=res_cond,
                           acts=frozenset({"access"}), cons=[]))

    return AmazonPolicy(users=users, resources=resources, rules=rules)
