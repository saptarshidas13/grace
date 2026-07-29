"""
Parser for the .abac policy file format used by Xu & Stoller's ABAC-Mining
and by ABAC Lab (SACMAT 2025), per DATASETS/README.md in the downloaded
datasets:

    userAttrib(uid, attr1=value1, attr2={a b c}, ...)
    resourceAttrib(rid, attr1=value1, ...)
    rule(subCond; resCond; acts; cons)
    # comments

Grammar notes derived from inspecting the actual corpus (not just the spec
doc, which undersells the variants in the older Xu-Stoller files):
  - subCond/resCond conjuncts: `attr [ {v1 v2}` or `attr in {v1 v2}` (synonyms,
    literal set-membership test), `attr ] value` (literal 'contains' test),
    `attr supseteqln {{v1 v2}}` (literal superset test; braces may be nested,
    stripped uniformly).
  - cons conjuncts (always relate a user attribute name to a resource
    attribute name, never a literal): `aus=ars`, `aum>arm`, `aus[arm`, `aum]ars`.
  - Fields are separated by ';'; a trailing empty field (from a trailing ';')
    is dropped.
  - Top-level commas (inside attr declarations, and inside subCond/resCond/
    cons) never appear inside a `{...}` value, since set elements are
    space-separated -- so a plain split(',') at each field/declaration level
    is safe.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Tuple

from .model import Entity, LiteralConjunct, RelConjunct, Rule

_LITERAL_OP_RE = re.compile(r"^\s*(\S+)\s+(in|supseteqln|\[|\])\s+(.+?)\s*$")
_REL_OP_RE = re.compile(r"^\s*(\S+)\s*(=|>|\[|\])\s*(\S+)\s*$")

_LITERAL_OP_MAP = {"[": "in", "in": "in", "]": "contains", "supseteqln": "supseteq"}
_REL_OP_MAP = {"=": "eq", ">": "superset_eq", "[": "user_val_in_res_set", "]": "res_val_in_user_set"}


@dataclass
class ParsedPolicy:
    users: Dict[str, Entity]
    resources: Dict[str, Entity]
    rules: List[Rule]


def _strip_braces(token: str) -> str:
    return token.strip().lstrip("{").rstrip("}").strip()


def _parse_value(raw: str) -> str | frozenset:
    raw = raw.strip()
    if raw.startswith("{"):
        inner = _strip_braces(raw)
        elems = inner.split() if inner else []
        return frozenset(elems)
    return raw


def _split_top_level_commas(s: str) -> List[str]:
    # Safe: '{...}' sets use space separation, never commas, in this grammar.
    return [p for p in (part.strip() for part in s.split(",")) if p != ""]


def _parse_entity_decl(kind: str, body: str) -> Entity:
    parts = _split_top_level_commas(body)
    if not parts:
        raise ValueError(f"empty {kind} declaration: {body!r}")
    entity_id = parts[0].strip()
    id_attr = "uid" if kind == "userAttrib" else "rid"
    attrs: Dict[str, str | frozenset] = {id_attr: entity_id}
    for p in parts[1:]:
        if "=" not in p:
            continue
        name, _, val = p.partition("=")
        attrs[name.strip()] = _parse_value(val)
    return Entity(id=entity_id, attrs=attrs)


def _parse_literal_conjuncts(field_text: str) -> List[LiteralConjunct]:
    conjuncts = []
    for conj_text in _split_top_level_commas(field_text):
        m = _LITERAL_OP_RE.match(conj_text)
        if not m:
            raise ValueError(f"unparseable literal conjunct: {conj_text!r}")
        attr, op_tok, rhs_text = m.groups()
        op = _LITERAL_OP_MAP[op_tok]
        rhs = _parse_value(rhs_text)
        conjuncts.append(LiteralConjunct(attr=attr, op=op, rhs=rhs))
    return conjuncts


def _parse_rel_conjuncts(field_text: str) -> List[RelConjunct]:
    conjuncts = []
    for conj_text in _split_top_level_commas(field_text):
        m = _REL_OP_RE.match(conj_text)
        if not m:
            raise ValueError(f"unparseable cons conjunct: {conj_text!r}")
        user_attr, op_tok, res_attr = m.groups()
        conjuncts.append(RelConjunct(user_attr=user_attr, op=_REL_OP_MAP[op_tok], res_attr=res_attr))
    return conjuncts


def _parse_rule_decl(rule_id: str, body: str) -> Rule:
    fields = body.split(";")
    # A rule with an empty cons is legitimately written as a trailing ';'
    # with nothing after it (4 fields, last one empty) -- keep that. Some
    # files additionally have a stray trailing ';' *after* a non-empty cons
    # (5 fields, 5th blank) -- drop only that genuinely-extra field.
    if len(fields) == 5 and fields[4].strip() == "":
        fields = fields[:4]
    if len(fields) != 4:
        raise ValueError(f"rule does not have 4 fields (sub;res;acts;cons): {body!r}")
    sub_text, res_text, acts_text, cons_text = (f.strip() for f in fields)
    sub_cond = _parse_literal_conjuncts(sub_text) if sub_text else []
    res_cond = _parse_literal_conjuncts(res_text) if res_text else []
    acts = frozenset(_strip_braces(acts_text).split()) if acts_text else frozenset()
    cons = _parse_rel_conjuncts(cons_text) if cons_text else []
    return Rule(id=rule_id, sub_cond=sub_cond, res_cond=res_cond, acts=acts, cons=cons, raw=body)


_DECL_RE = re.compile(r"^(userAttrib|resourceAttrib|rule)\((.*)\)\s*$")


def parse_abac_file(path: str | Path) -> ParsedPolicy:
    path = Path(path)
    users: Dict[str, Entity] = {}
    resources: Dict[str, Entity] = {}
    rules: List[Rule] = []
    rule_counter = 0

    with open(path, "r", encoding="utf-8", errors="replace") as f:
        for lineno, raw_line in enumerate(f, start=1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            m = _DECL_RE.match(line)
            if not m:
                continue  # skip blank/unsupported lines rather than hard-fail
            kind, body = m.groups()
            try:
                if kind == "userAttrib":
                    e = _parse_entity_decl(kind, body)
                    users[e.id] = e
                elif kind == "resourceAttrib":
                    e = _parse_entity_decl(kind, body)
                    resources[e.id] = e
                else:  # rule
                    rule_counter += 1
                    rules.append(_parse_rule_decl(f"P{rule_counter}", body))
            except ValueError as exc:
                raise ValueError(f"{path}:{lineno}: {exc}") from exc

    return ParsedPolicy(users=users, resources=resources, rules=rules)
