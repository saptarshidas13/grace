"""
Fair, apples-to-apples comparison between Syn-ABAC's Phase-I and Das et
al.'s PolAP [33]: both are greedy set-cover-style heuristics that prune
any candidate rule/policy reaching outside an authorized target set, then
minimize the count of rules/policies selected. The two papers solve
differently-framed problems (Syn-ABAC: minimal policy set for an org's own
shared resource set; PolAP: minimal rule set for one subject's desired
access list) -- so the comparison is only fair if both are pointed at the
*same underlying access target*, and the target must be derived
independently of either algorithm's own solution.

Two comparisons are run, reporting different (both genuine) things:

  1. Bulk: build ONE desired-access list L for a single synthetic subject
     covering every resource in the shared set R' at its dataset's most
     common operation, then run PolAP once for that whole L. PolAP's own
     Algorithm 1 (lines 8-10, "if RS == Null: return 0; exit") is
     literally all-or-nothing: a single unreachable (resource, operation)
     pair anywhere in L aborts the *entire* subject's adaptation, with
     zero rules returned. This is not a bug in this reimplementation --
     it is what the published algorithm does, and it is precisely why
     naively applying a single-subject/few-object algorithm to bulk
     cross-org resource sharing (many resources at once) tends to fail
     outright, unlike Syn-ABAC's Phase-I, which delivers whatever subset
     it safely can per-resource and defers the rest to Phase-II.
  2. Decomposed: run PolAP separately per resource (L = a single pair),
     to get a fine-grained, resource-by-resource minimization comparison
     that isn't swamped by the all-or-nothing behavior in (1).
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, FrozenSet, List, Sequence, Set, Tuple

from .baseline_polap import polap
from .model import Entity, Rule
from .phase1 import PhaseIResult, find_policy


def _canonical_op_for_resource(resource: Entity, policies: Sequence[Rule]) -> str:
    """One operation this specific resource structurally supports (the
    alphabetically-first operation among all policies whose resource
    condition matches it, regardless of leak-safety). A single global
    "dominant" operation across the whole policy set is wrong whenever
    operation applicability is resource-type-dependent, as it is in every
    dataset tested here (e.g. healthcare's HR-type resources only ever
    take addItem/addNote, never read, while HRitem-type resources only
    take read) -- an earlier version used one global op and manufactured
    a 0%-deliverable result purely from that structural mismatch, not from
    anything about PolAP's minimization quality."""
    ops: Set[str] = set()
    for p in find_policy(resource, policies):
        ops |= set(p.acts)
    return min(ops) if ops else "read"


def desired_accesses_from_shared(shared: FrozenSet[str], all_resources: Sequence[Entity],
                                  policies: Sequence[Rule]) -> List[Tuple[str, str]]:
    by_id = {r.id: r for r in all_resources}
    pairs: List[Tuple[str, str]] = []
    for rid in shared:
        r = by_id.get(rid)
        if r is None:
            continue
        pairs.append((rid, _canonical_op_for_resource(r, policies)))
    return sorted(pairs)


@dataclass
class ComparisonResult:
    dataset: str
    shared_fraction: float
    trial_seed: int
    target_pairs: int
    synabac_policy_count: int
    synabac_deliverable_count: int
    # bulk (single-subject, all-or-nothing) PolAP result
    polap_bulk_rule_count: int
    polap_bulk_undeliverable: bool
    # decomposed (per-resource) PolAP result
    polap_decomposed_deliverable_count: int
    polap_decomposed_rule_count: int  # sum of rules used across individually-deliverable resources


def compare_on_target(
    dataset: str,
    fraction: float,
    seed: int,
    shared: FrozenSet[str],
    phase1_result: PhaseIResult,
    all_resources: Sequence[Entity],
    policies: Sequence[Rule],
) -> ComparisonResult:
    desired = desired_accesses_from_shared(shared, all_resources, policies)
    bulk = polap(desired, all_resources, policies)

    by_id = {r.id: r for r in all_resources}
    deliverable_count = 0
    rule_ids: Set[str] = set()
    for rid, op in desired:
        r = by_id[rid]
        # authorized_objects=shared (not just {rid}): a candidate rule is only
        # a leak if it reaches outside the *whole* shared set, matching what
        # Syn-ABAC's own RePo-tree pruning checks against for this resource.
        single = polap([(rid, op)], all_resources, policies, authorized_objects=shared)
        if not single.undeliverable:
            deliverable_count += 1
            rule_ids.update(single.minimal_rules)

    return ComparisonResult(
        dataset=dataset,
        shared_fraction=fraction,
        trial_seed=seed,
        target_pairs=len(desired),
        synabac_policy_count=len(phase1_result.policy_ids),
        synabac_deliverable_count=len(shared) - len(phase1_result.undeliverable),
        polap_bulk_rule_count=len(bulk.minimal_rules) if not bulk.undeliverable else -1,
        polap_bulk_undeliverable=bulk.undeliverable,
        polap_decomposed_deliverable_count=deliverable_count,
        polap_decomposed_rule_count=len(rule_ids),
    )
