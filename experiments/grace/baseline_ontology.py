"""
Gupta & Sural -- "Ontology-based Evaluation of ABAC Policies for
Inter-Organizational Resource Sharing" (IWSPA 2023) [ref 35 in the
Syn-ABAC paper].

Implements the two matching modes from their Section 4.2/4.3:
  - hierarchy_ok: a rule's required value V is satisfied by a user's value
    V' if V' == V or V is a strict ancestor of V' in the attribute's
    ontology (a rule written for the general case -- e.g. "School" --
    is satisfied by anyone in a more specific subclass -- e.g.
    "SchoolOfBasicSciences" -- underneath it; direction verified against
    their own worked example, r2/School/SchoolOfBasicSciences, below).
  - relaxed_ok: additionally satisfied if the ontology *distance* between
    V and V' (via their lowest common ancestor) is <= a caller-supplied
    relaxation distance D -- their "controlled relaxation" mechanism
    (Section 4.3), which is exactly what none of Syn-ABAC's Algorithm 6
    (pol_eval) supports: Syn-ABAC requires literal string equality (or a
    wildcard), with no notion of attribute-value proximity at all.

This is the one prior work among [33]/[34]/[35] that actually implements
the "attribute mismatch" resolution Syn-ABAC's own abstract names as
motivation (see Review_and_Revision_Notes.md / the conversation's earlier
finding that Syn-ABAC's Phase-I/Phase-II never touch attribute-name
translation at all) -- used here for a direct, quantified comparison of
that specific capability gap, not for a rule-count/minimization comparison
(different problem entirely).

Deliberately a *tree*, not a general DAG: every worked example and figure
in the paper (Figures 4-7) is a tree (each value has exactly one parent),
and a tree admits a simpler, unambiguous LCA-based distance -- a full DAG
generalization (multiple inheritance) is not exercised anywhere in the
paper and is out of scope here.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, Optional


@dataclass
class Ontology:
    """One attribute's value hierarchy: a tree of value names, with an
    optional equivalence map for semantically-identical siblings (e.g.
    Section 4.4's College/Faculty/School: "Two values A and B having an
    edge from A to B as well as from B to A are essentially equivalent").
    Equivalences are resolved at every step of ancestor-chain traversal so
    they act as if merged into one node -- not just at the top level --
    matching how the paper's own r5 example (distance 3 collapsing to 2
    because College is equivalent to School) actually works out.
    """
    parent: Dict[str, Optional[str]] = field(default_factory=dict)
    equivalent: Dict[str, str] = field(default_factory=dict)  # value -> canonical representative

    def add(self, value: str, parent: Optional[str] = None) -> None:
        self.parent[value] = parent

    def add_equivalence(self, value: str, canonical: str) -> None:
        self.equivalent[value] = canonical

    def _canon(self, value: str) -> str:
        return self.equivalent.get(value, value)

    def level(self, value: str) -> int:
        """Distance from the root (root = level 0)."""
        d = 0
        v = self._canon(value)
        while self.parent.get(v) is not None:
            v = self._canon(self.parent[v])
            d += 1
        return d

    def ancestors(self, value: str) -> List[str]:
        chain = [self._canon(value)]
        v = chain[0]
        while self.parent.get(v) is not None:
            v = self._canon(self.parent[v])
            chain.append(v)
        return chain

    def is_ancestor_or_equal(self, candidate_ancestor: str, value: str) -> bool:
        return self._canon(candidate_ancestor) in self.ancestors(value)

    def distance(self, a: str, b: str) -> int:
        """Tree distance via lowest common ancestor."""
        anc_a = self.ancestors(a)
        anc_b = self.ancestors(b)
        set_a = set(anc_a)
        for i, node in enumerate(anc_b):
            if node in set_a:
                return anc_a.index(node) + i
        return max(self.level(a), self.level(b)) + 1  # disconnected fallback


def hierarchy_ok(ontology: Optional[Ontology], required_value: str, user_value: str) -> bool:
    """Section 4.2: satisfied if equal, or `required_value` is a strict
    ancestor of `user_value` (the rule is written at a more general level
    than the user's specific value)."""
    if required_value == user_value:
        return True
    if ontology is None:
        return False
    return ontology.is_ancestor_or_equal(required_value, user_value) and required_value != user_value


def relaxed_ok(ontology: Optional[Ontology], required_value: str, user_value: str, max_distance: int) -> bool:
    """Section 4.3: hierarchy_ok, OR ontology distance <= max_distance."""
    if hierarchy_ok(ontology, required_value, user_value):
        return True
    if ontology is None or max_distance <= 0:
        return False
    return ontology.distance(required_value, user_value) <= max_distance
