"""
Empirical check of the paper's unproven claim (Section 4.2.4): that mapping
each policy selected by Phase-I to one representative user "ensures SoD" and
"ensures BoD" (flagged as Review_and_Revision_Notes.md item A3 -- asserted
narratively, never proven).

Methodology (new experiment, additive -- does not change any paper algorithm):
  1. For each policy in Phase-I's selected pol_list, synthesize one
     representative user whose attributes satisfy that policy's subject
     condition ("induced user") -- operationalizing the paper's own phrase
     "these attribute values can be mapped to individual users."
  2. None of the downloaded datasets ship an explicit SoD/BoD conflict
     specification, so conflict pairs (for SoD) and related-task pairs (for
     BoD) are synthesized by sampling random pairs of the operation labels
     actually present in the dataset -- a generic, domain-agnostic stress
     test rather than a curated business-rule set.
  3. Auth(u,o) (Section 3.1) decides membership; report the violation rate
     over many random pairs/trials.
"""
from __future__ import annotations

import random
from dataclasses import dataclass
from typing import Dict, FrozenSet, List, Sequence, Set, Tuple

from .model import Entity, Rule, auth
from .phase1 import PhaseIResult


def induce_user_for_policy(policy: Rule, prefix: str = "induced") -> Entity:
    """One representative user satisfying `policy`'s subject condition exactly."""
    attrs: Dict[str, object] = {}
    for c in policy.sub_cond:
        if c.op == "in":
            rhs = c.rhs if isinstance(c.rhs, frozenset) else frozenset({c.rhs})
            attrs[c.attr] = sorted(rhs)[0] if rhs else None
        elif c.op == "contains":
            attrs[c.attr] = frozenset({c.rhs})
        elif c.op == "supseteq":
            attrs[c.attr] = c.rhs if isinstance(c.rhs, frozenset) else frozenset({c.rhs})
    return Entity(id=f"{prefix}_{policy.id}", attrs=attrs)


def induced_users(phase1_result: PhaseIResult, policies: Sequence[Rule]) -> List[Entity]:
    by_id = {p.id: p for p in policies}
    return [induce_user_for_policy(by_id[pid]) for pid in phase1_result.policy_ids if pid in by_id]


def all_operations(policies: Sequence[Rule]) -> FrozenSet[str]:
    ops: Set[str] = set()
    for p in policies:
        ops |= set(p.acts)
    return frozenset(ops)


def random_pairs(ops: FrozenSet[str], k: int, seed: int) -> Set[Tuple[str, str]]:
    rng = random.Random(seed)
    ops_list = sorted(ops)
    pairs: Set[Tuple[str, str]] = set()
    attempts = 0
    while len(pairs) < k and attempts < 20 * k + 100 and len(ops_list) >= 2:
        attempts += 1
        a, b = rng.sample(ops_list, 2)
        pairs.add(tuple(sorted((a, b))))
    return pairs


@dataclass
class SoDBoDReport:
    n_induced_users: int
    n_sod_pairs_tested: int
    sod_violations: List[Tuple[str, str, str]]   # (user_id, op1, op2)
    n_bod_pairs_tested: int
    bod_unsatisfied: List[Tuple[str, str]]        # (op1, op2) with no single authorized user


def verify_sod_bod(
    phase1_result: PhaseIResult,
    all_resources: Sequence[Entity],
    policies: Sequence[Rule],
    n_conflict_pairs: int = 30,
    seed: int = 0,
) -> SoDBoDReport:
    users = induced_users(phase1_result, policies)
    ops = all_operations(policies)
    conflict_pairs = random_pairs(ops, n_conflict_pairs, seed)
    related_pairs = random_pairs(ops, n_conflict_pairs, seed + 1)

    sod_violations = []
    for u in users:
        for (o1, o2) in conflict_pairs:
            if auth(u, o1, all_resources, policies) and auth(u, o2, all_resources, policies):
                sod_violations.append((u.id, o1, o2))

    bod_unsatisfied = []
    for (o1, o2) in related_pairs:
        ok = any(auth(u, o1, all_resources, policies) and auth(u, o2, all_resources, policies) for u in users)
        if not ok:
            bod_unsatisfied.append((o1, o2))

    return SoDBoDReport(
        n_induced_users=len(users),
        n_sod_pairs_tested=len(conflict_pairs),
        sod_violations=sod_violations,
        n_bod_pairs_tested=len(related_pairs),
        bod_unsatisfied=bod_unsatisfied,
    )
