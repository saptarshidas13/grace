"""
Das, Sural, Vaidya, Atluri -- "Policy Adaptation in Attribute-Based Access
Control for Inter-organizational Collaboration" (CIC 2017) [ref 33 in the
Syn-ABAC paper], Algorithm 1 (PolAP): SORO-tree construction + pruning +
greedy minimum-hitting-set, computing a minimal set of rules for ONE
subject with a desired access list L = {(object_id, op), ...}.

Reuses this project's Rule/Entity/resource_matches: both [33] and its
hierarchical extension [34] use the identical flat "attr=value or
don't-care" rule grammar as Syn-ABAC's own Section 3.1 model (see
model.py's make_flat_rule), so no new evaluator is needed -- only the
different selection algorithm (SORO tree + min hitting set, vs Syn-ABAC's
RePo tree + MSCP).

One clarification versus the literal pseudocode (Algorithm 5 in [33]): the
paper's op-match filter mutates a *shared* per-object rule cache
(Sigma_a[m] <- Sigma_a[m] - {rd}) while iterating per (object, op) pair.
Read literally, processing (o, read) before (o, write) would strip
write-only rules from the shared cache and make them unavailable when (o,
write) is processed later -- an evident bug, not a deliberate design
choice (nothing in the text justifies cross-pair cache mutation). This
implementation builds each (object, op) pair's candidate set
independently, avoiding that cross-contamination, which is the more
charitable reading and avoids artificially handicapping the baseline.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, FrozenSet, List, Sequence, Set, Tuple

from .model import Entity, Rule, resource_matches
from .phase1 import find_resources


@dataclass
class PolAPResult:
    minimal_rules: List[str]
    subject_attr_value_pairs: Dict[str, object]
    undeliverable: bool
    undeliverable_pair: Tuple[str, str] = None


def polap(
    desired_accesses: Sequence[Tuple[str, str]],
    all_resources: Sequence[Entity],
    policies: Sequence[Rule],
    authorized_objects: FrozenSet[str] = None,
) -> PolAPResult:
    """Algorithm 1 (PolAP) for a single subject.

    `authorized_objects` is the paper's S' (Algorithm 5, line 17): the
    subject's *entire* authorized object set, against which a candidate
    rule's full coverage is checked for leakage. Defaults to the objects
    named in `desired_accesses` itself -- correct for the paper's own
    framing (a subject's authorization boundary *is* its desired access
    list), but callers evaluating one (object, op) pair at a time against
    a larger authorization boundary (e.g. comparing per-resource against
    Syn-ABAC's shared set R') must pass that larger boundary explicitly,
    or every multi-resource-covering rule will look like it leaks purely
    because the single-pair boundary is too narrow.
    """
    by_id = {r.id: r for r in all_resources}
    desired_objects = authorized_objects if authorized_objects is not None else frozenset(o for o, _ in desired_accesses)

    rule_sets: List[Set[str]] = []
    for (o, op) in desired_accesses:
        obj = by_id.get(o)
        if obj is None:
            return PolAPResult([], {}, True, (o, op))
        applicable = [p for p in policies if op in p.acts and resource_matches(p, obj)]
        safe = [p.id for p in applicable if find_resources(p, all_resources) <= desired_objects]
        if not safe:
            return PolAPResult([], {}, True, (o, op))
        rule_sets.append(set(safe))

    # Algorithm 6: greedy minimum hitting set.
    remaining = list(range(len(rule_sets)))
    chosen: List[str] = []
    while remaining:
        counts: Dict[str, int] = {}
        for i in remaining:
            for rid in rule_sets[i]:
                counts[rid] = counts.get(rid, 0) + 1
        best_rid = max(counts, key=lambda k: (counts[k], k))
        chosen.append(best_rid)
        remaining = [i for i in remaining if best_rid not in rule_sets[i]]

    by_rule_id = {p.id: p for p in policies}
    attrs: Dict[str, object] = {}
    for rid in sorted(set(chosen)):
        for c in by_rule_id[rid].sub_cond:
            attrs[c.attr] = c.rhs

    return PolAPResult(minimal_rules=sorted(set(chosen)), subject_attr_value_pairs=attrs, undeliverable=False)
