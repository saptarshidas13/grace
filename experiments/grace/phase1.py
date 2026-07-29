"""
Syn-ABAC Phase-I: Algorithms 2-7 from the paper.

Deviations from the literally-published pseudocode are documented inline and
summarized in ../IMPLEMENTATION_NOTES.md -- each one was needed to make the
algorithm actually executable / correct, not a change of the paper's intent.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, FrozenSet, List, Sequence, Tuple

from .model import Entity, Rule, resource_matches


def find_policy(resource: Entity, policies: Sequence[Rule]) -> List[Rule]:
    """Algorithm 4: policies whose resource condition matches this resource."""
    return [p for p in policies if resource_matches(p, resource)]


def find_resources(policy: Rule, resources: Sequence[Entity]) -> FrozenSet[str]:
    """Algorithm 5: ids of all resources covered by this policy's resource condition."""
    return frozenset(r.id for r in resources if resource_matches(policy, r))


@dataclass
class RePoLeaf:
    policy_id: str
    resource_set: FrozenSet[str]


def construct_repo_tree(
    resource: Entity,
    all_resources: Sequence[Entity],
    policies: Sequence[Rule],
    shared: FrozenSet[str],
) -> List[RePoLeaf]:
    """Algorithm 3: RePo tree for one shared resource, pruned to branches whose
    full resource coverage stays inside the shared set `shared` (R').

    NOTE (deviation, see IMPLEMENTATION_NOTES.md #1): the published Algorithm 3
    only returns the *resource sets* (`filtered_sets`), which Algorithm 2 then
    has to map back to a policy id by re-running `find_resources` and testing
    set equality (line 15). That reverse lookup is redundant and fragile if
    read literally as a search rather than a bookkeeping step -- we already
    know which policy produced which set while building the tree, so we carry
    the (policy_id, resource_set) pair through directly. Selection semantics
    are unchanged: if two distinct policies happen to cover the exact same
    resource set, both are still legitimate and both are kept.
    """
    applicable = find_policy(resource, policies)
    leaves: List[RePoLeaf] = []
    for policy in applicable:
        covered = find_resources(policy, all_resources)
        if covered <= shared:  # prune: would grant access outside the shared set
            leaves.append(RePoLeaf(policy_id=policy.id, resource_set=covered))
    return leaves


def mscp_greedy(universe: FrozenSet[str], candidates: Sequence[RePoLeaf]) -> List[RePoLeaf]:
    """Algorithm 7: greedy minimum set cover (Chvatal 1979 greedy heuristic,
    H(n) ~ ln(n) approximation ratio -- see IMPLEMENTATION_NOTES.md #4 for the
    optimality-gap comparison against an exact solver, in baselines.py).

    Deterministic tie-breaking (smallest original index) for reproducibility;
    the paper does not specify a tie-break rule.
    """
    remaining = set(universe)
    chosen: List[RePoLeaf] = []
    pool = list(candidates)
    while remaining:
        best_idx, best_gain = -1, -1
        for i, leaf in enumerate(pool):
            gain = len(leaf.resource_set & remaining)
            if gain > best_gain:
                best_idx, best_gain = i, gain
        if best_idx == -1 or best_gain <= 0:
            break  # nothing left can cover the remainder (shouldn't happen, see Phase-I contract)
        best = pool.pop(best_idx)
        chosen.append(best)
        remaining -= best.resource_set
    return chosen


@dataclass
class PhaseIResult:
    policy_ids: List[str]                 # minimal policy set covering `shared`
    covering_sets: List[RePoLeaf]          # the (policy, resource_set) pairs chosen
    undeliverable: FrozenSet[str]          # R_D: shared resources with no safe policy at all
    per_resource_leaf_count: Dict[str, int]  # diagnostics: RePo-tree branches survived pruning, per resource
    all_candidates: List[RePoLeaf] = None  # full candidate pool (pre-greedy) -- for optimality-gap checks


def syn_abac_phase1(
    shared: FrozenSet[str],
    all_resources: Sequence[Entity],
    policies: Sequence[Rule],
) -> PhaseIResult:
    """Algorithm 2, with one corrected detail (see IMPLEMENTATION_NOTES.md #2):
    the published pseudocode loops `for all ri in R` (the *entire* resource
    universe) and then calls `MSCP(R - R_D, S)` -- but every candidate set in
    S is already pruned to be a subset of R' (the shared set), by
    construct_RePo_tree's own pruning step. Covering the full universe R with
    sets that are all subsets of R' is impossible whenever R' subset R, so a
    literal reading either loops forever or silently mis-covers. The evident
    intent -- consistent with the rest of Section 4.2 ("a minimal set of
    policies... that facilitates access to the shared resources") -- is to
    cover R', not R. We loop over `shared` and cover `shared - R_D`.
    """
    by_id = {r.id: r for r in all_resources}
    resource_pool: List[Entity] = []
    seen = set()
    for rid in shared:
        if rid in by_id:
            resource_pool.append(by_id[rid])
            seen.add(rid)

    candidates: List[RePoLeaf] = []
    undeliverable = set()
    leaf_counts: Dict[str, int] = {}

    for r in resource_pool:
        leaves = construct_repo_tree(r, all_resources, policies, shared)
        leaf_counts[r.id] = len(leaves)
        if len(leaves) == 0:
            undeliverable.add(r.id)
        else:
            candidates.extend(leaves)

    universe = frozenset(seen) - undeliverable
    chosen = mscp_greedy(universe, candidates)

    policy_ids = sorted({leaf.policy_id for leaf in chosen})
    return PhaseIResult(
        policy_ids=policy_ids,
        covering_sets=chosen,
        undeliverable=frozenset(undeliverable),
        per_resource_leaf_count=leaf_counts,
        all_candidates=candidates,
    )
