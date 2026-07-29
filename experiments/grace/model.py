"""
Core Syn-ABAC data model.

Implements the formal objects of the paper's Section 3.1 (User/Resource/
Environment/Policy as attribute name-value-pair sets) and a *generalization*
of Algorithm 6 (pol_eval).

Why generalized: Algorithm 6, as published, only supports single-valued
exact-match or wildcard ('*') conditions. The real benchmark policy sets used
here (Xu & Stoller; ABAC Lab) rely on multi-valued attributes and cross-entity
relational constraints (e.g. "user's teams contains resource's treatingTeam").
The flat exact-match/wildcard model is exactly the special case of this
generalization where every condition is a single-element 'in' test and no
`cons` (relational) constraints are used -- see `make_flat_rule` and
tests/test_paper_example.py, which reproduces the paper's own Tables 1-5 and
Figure 1 through this generalized evaluator to confirm the two coincide.

Two evaluation entry points, matching how the paper's algorithms actually use
pol_eval:

- `resource_matches(rule, resource)`: resource-condition-only test. This is
  exactly what Algorithms 4, 5, 9 and 10 call `pol_eval(Pi(RCi), ri)` for --
  they never pass a user or check `cons`, because Phase-I/Phase-II reason
  about which resources a policy covers, independent of a specific requester.

- `full_request_allowed(rule, user, resource, op)`: subject condition AND
  resource condition AND relational constraints AND operation membership --
  this is `f(p, <u,r,e,op>)` from Section 3.1, used only for the SoD/BoD
  Auth(u,o) verification (Algorithm 6 alone is not sufficient for that; the
  paper never actually says otherwise, but never says how the two connect
  either -- see Review_and_Revision_Notes.md item A2).
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, FrozenSet, List, Optional, Union

Value = Union[str, FrozenSet[str]]

WILDCARD = "*"


@dataclass
class Entity:
    """A user or a resource: an id plus a bag of attribute name -> value."""
    id: str
    attrs: Dict[str, Value] = field(default_factory=dict)

    def __hash__(self):
        return hash(self.id)


@dataclass(frozen=True)
class LiteralConjunct:
    """attr OP rhs, evaluated against ONE entity's own attributes.

    op in {'in', 'contains', 'supseteq'}:
      - 'in'       : entity.attrs[attr] (atomic) must be a member of rhs (set)
      - 'contains' : entity.attrs[attr] (set) must contain rhs (atomic)
      - 'supseteq' : entity.attrs[attr] (set) must be a superset of rhs (set)
    """
    attr: str
    op: str
    rhs: Value


@dataclass(frozen=True)
class RelConjunct:
    """Cross-entity constraint from a rule's `cons` field: user_attr OP res_attr.

    op in {'eq', 'superset_eq', 'user_val_in_res_set', 'res_val_in_user_set'}
    corresponding to the paper's `aus=ars`, `aum>arm`, `aus[arm`, `aum]ars`.
    """
    user_attr: str
    op: str
    res_attr: str


@dataclass
class Rule:
    """A Syn-ABAC / ABAC policy: subject cond, resource cond, actions, constraints."""
    id: str
    sub_cond: List[LiteralConjunct]
    res_cond: List[LiteralConjunct]
    acts: FrozenSet[str]
    cons: List[RelConjunct]
    raw: str = ""

    def __hash__(self):
        return hash(self.id)

    def __eq__(self, other):
        return isinstance(other, Rule) and self.id == other.id


def _as_set(val: Optional[Value]) -> Optional[FrozenSet[str]]:
    if val is None:
        return None
    if isinstance(val, frozenset):
        return val
    return frozenset({val})


def _literal_ok(conj: LiteralConjunct, entity: Entity) -> bool:
    val = entity.attrs.get(conj.attr)
    if val is None:
        return False
    if conj.op == "in":
        rhs = conj.rhs if isinstance(conj.rhs, frozenset) else frozenset({conj.rhs})
        return len(_as_set(val) & rhs) > 0
    if conj.op == "contains":
        if not isinstance(val, frozenset):
            return False
        return conj.rhs in val
    if conj.op == "supseteq":
        if not isinstance(val, frozenset):
            return False
        rhs = conj.rhs if isinstance(conj.rhs, frozenset) else frozenset({conj.rhs})
        return val >= rhs
    raise ValueError(f"unknown literal op {conj.op!r}")


def _rel_ok(conj: RelConjunct, user: Entity, res: Entity) -> bool:
    uval = user.attrs.get(conj.user_attr)
    rval = res.attrs.get(conj.res_attr)
    if uval is None or rval is None:
        return False
    if conj.op == "eq":
        return uval == rval
    if conj.op == "superset_eq":  # aum > arm
        return isinstance(uval, frozenset) and isinstance(rval, frozenset) and uval >= rval
    if conj.op == "user_val_in_res_set":  # aus [ arm
        return isinstance(rval, frozenset) and uval in rval
    if conj.op == "res_val_in_user_set":  # aum ] ars
        return isinstance(uval, frozenset) and rval in uval
    raise ValueError(f"unknown relational op {conj.op!r}")


def resource_matches(rule: Rule, resource: Entity) -> bool:
    """Algorithm 6 (pol_eval), generalized: resource-condition-only test."""
    return all(_literal_ok(c, resource) for c in rule.res_cond)


def full_request_allowed(rule: Rule, user: Entity, resource: Entity, op: str) -> bool:
    """f(p, <u,r,e,op>) from Section 3.1 -- full evaluation including subject
    condition and relational constraints. Environment attributes are not used
    by any released dataset here, so `e` is omitted (see README caveats)."""
    if op not in rule.acts:
        return False
    if not resource_matches(rule, resource):
        return False
    if not all(_literal_ok(c, user) for c in rule.sub_cond):
        return False
    if not all(_rel_ok(c, user, resource) for c in rule.cons):
        return False
    return True


def auth(user: Entity, op: str, resources: List[Entity], rules: List[Rule]) -> bool:
    """Auth(u,o) := exists r, exists p in P : f(p, <u,r,e,op>) = Allow.
    Ties SoD/BoD's Auth(u,o) predicate (Section 3.1) back to f(.)."""
    return any(
        full_request_allowed(rule, user, r, op)
        for rule in rules
        for r in resources
        if op in rule.acts
    )


def make_flat_rule(rule_id: str, user_cond: Dict[str, str], res_cond: Dict[str, str],
                    acts: List[str], wildcard: str = WILDCARD) -> Rule:
    """Builder for the paper's own flat Table-5-style policies: attr=value or
    attr='*' (don't care). This is the degenerate case of the generalized
    model -- every non-wildcard condition becomes a single-element 'in' test,
    with no relational constraints, exactly matching Algorithm 6 as published.
    """
    sub = [LiteralConjunct(a, "in", frozenset({v})) for a, v in user_cond.items() if v != wildcard]
    res = [LiteralConjunct(a, "in", frozenset({v})) for a, v in res_cond.items() if v != wildcard]
    return Rule(id=rule_id, sub_cond=sub, res_cond=res, acts=frozenset(acts), cons=[])
