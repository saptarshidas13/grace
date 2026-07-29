"""
Comparators for the review gap "no baseline / comparator anywhere" (C1):

  - naive_baseline: no minimization at all -- grant each shared resource via
    the first applicable, resource-condition-matching policy found (this is
    "ABAC with no cross-org policy-selection layer", i.e. what you get by
    skipping Syn-ABAC's Phase-I optimization).
  - exact_min_cover: optimal minimum set cover via ILP (PuLP/CBC), to report
    Syn-ABAC's greedy Phase-I optimality gap -- the paper asserts the greedy
    heuristic is used "since exact solutions... are computationally
    expensive" but never actually measures how far from optimal it lands.
    Falls back to a reported "skipped (too large)" beyond a size cutoff,
    since ILP itself is not guaranteed polynomial.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional, Sequence

import pulp

from .model import Entity, Rule
from .phase1 import RePoLeaf, find_policy


@dataclass
class NaiveResult:
    policy_ids: List[str]
    undeliverable: List[str]


def naive_baseline(shared, all_resources: Sequence[Entity], policies: Sequence[Rule]) -> NaiveResult:
    """Pick the first resource-condition-matching policy per shared resource,
    with no attempt to minimize the policy count or avoid over-permissioning
    onto non-shared resources."""
    by_id = {r.id: r for r in all_resources}
    chosen = []
    undeliverable = []
    for rid in shared:
        r = by_id.get(rid)
        if r is None:
            continue
        applicable = find_policy(r, policies)
        if not applicable:
            undeliverable.append(rid)
        else:
            chosen.append(applicable[0].id)
    return NaiveResult(policy_ids=sorted(set(chosen)), undeliverable=undeliverable)


@dataclass
class ExactCoverResult:
    size: Optional[int]        # None if skipped
    solved: bool
    skipped_reason: Optional[str] = None


def exact_min_cover(
    universe: frozenset,
    candidates: Sequence[RePoLeaf],
    max_candidates: int = 400,
    time_limit_s: int = 30,
) -> ExactCoverResult:
    """Exact minimum set cover via ILP: minimize sum(x_i) s.t. for every
    element e, sum of x_i over sets containing e >= 1, x_i in {0,1}."""
    if not universe:
        return ExactCoverResult(size=0, solved=True)
    if len(candidates) > max_candidates:
        return ExactCoverResult(size=None, solved=False,
                                 skipped_reason=f">{max_candidates} candidate sets, skipped for tractability")

    prob = pulp.LpProblem("min_set_cover", pulp.LpMinimize)
    x = [pulp.LpVariable(f"x{i}", cat="Binary") for i in range(len(candidates))]
    prob += pulp.lpSum(x)

    covering = {e: [] for e in universe}
    for i, leaf in enumerate(candidates):
        for e in leaf.resource_set:
            if e in covering:
                covering[e].append(i)

    for e, idxs in covering.items():
        if not idxs:
            return ExactCoverResult(size=None, solved=False, skipped_reason=f"element {e} uncoverable")
        prob += pulp.lpSum(x[i] for i in idxs) >= 1

    solver = pulp.PULP_CBC_CMD(msg=False, timeLimit=time_limit_s)
    status = prob.solve(solver)
    if pulp.LpStatus[status] != "Optimal":
        return ExactCoverResult(size=None, solved=False, skipped_reason=f"solver status={pulp.LpStatus[status]}")
    size = int(round(pulp.value(prob.objective)))
    return ExactCoverResult(size=size, solved=True)
