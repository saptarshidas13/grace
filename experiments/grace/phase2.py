"""
Syn-ABAC Phase-II: Algorithms 8-10, CIA-priority trade-offs for resources
Phase-I could not deliver safely (R_D).

Deviations from the literally-published pseudocode (confirmed with the user,
see ../IMPLEMENTATION_NOTES.md #3-#6):

  - Algorithm 8's `choice` is a free variable in the pseudocode (never a
    declared input) -- made an explicit parameter here.
  - Algorithm 8 iterates over Phase-I's undeliverable set R_D, not literally
    every resource in R' -- matches the surrounding prose ("shared resources
    that remain inaccessible in Phase-I"), avoiding redundant re-evaluation
    of resources Phase-I already delivered.
  - Algorithm 9 (preserve_confidentiality) had no `else` branch for
    CRS* > tau; added an explicit deny outcome.
  - Algorithm 10 (preserve_integrity) counts ALL of |PR_i| in its pseudocode,
    but the paper's own Equation 4 and the worked example in Section 4.3.2
    only count *non-modifying* unauthorized exposure, with a
    'compensatory read' fallback -- implemented per Equation 4 + prose, using
    an explicit (documented) modifying-operations vocabulary since the paper
    never defines one.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Dict, FrozenSet, List, Optional, Sequence

from .model import Entity, Rule
from .phase1 import find_resources, resource_matches

DEFAULT_MODIFYING_OPS = frozenset(
    {"write", "update", "delete", "create", "modify", "remove",
     "addItem", "addNote", "addScore", "createAppointment"}
)

DEFAULT_CONF_LEVELS = {"public": 1, "restricted": 3, "confidential": 5}


def default_conf_level(resource: Entity, attr: str = "Sensitivity") -> int:
    val = resource.attrs.get(attr) or resource.attrs.get(attr.lower())
    if isinstance(val, frozenset):
        val = next(iter(val), None)
    return DEFAULT_CONF_LEVELS.get(val, 1)


@dataclass
class ConfidentialityOutcome:
    resource_id: str
    decision: str          # "allow" | "deny"
    policy_id: Optional[str]
    crs: Optional[int]


def preserve_confidentiality(
    resource: Entity,
    all_resources: Sequence[Entity],
    shared: FrozenSet[str],
    policies: Sequence[Rule],
    tau: int,
    conf_level_fn: Callable[[Entity], int] = default_conf_level,
) -> ConfidentialityOutcome:
    """Algorithm 9, with an explicit deny branch when CRS* > tau."""
    by_id = {r.id: r for r in all_resources}
    ep = [p for p in policies if resource_matches(p, resource)]
    if not ep:
        return ConfidentialityOutcome(resource.id, "deny", None, None)

    best_policy, best_crs = None, None
    for p in ep:
        leak = find_resources(p, all_resources) - shared
        crs = sum(conf_level_fn(by_id[rid]) for rid in leak if rid in by_id)
        if best_crs is None or crs < best_crs:
            best_policy, best_crs = p, crs

    if best_crs is not None and best_crs <= tau:
        return ConfidentialityOutcome(resource.id, "allow", best_policy.id, best_crs)
    return ConfidentialityOutcome(resource.id, "deny", best_policy.id if best_policy else None, best_crs)


@dataclass
class IntegrityOutcome:
    resource_id: str
    decision: str          # "grant" | "compensatory_read" | "deny"
    policy_id: Optional[str]
    unauthorized_exposure: Optional[int]


def preserve_integrity(
    resource: Entity,
    all_resources: Sequence[Entity],
    shared: FrozenSet[str],
    policies: Sequence[Rule],
    modifying_ops: FrozenSet[str] = DEFAULT_MODIFYING_OPS,
) -> IntegrityOutcome:
    """Algorithm 10, implemented per Equation 4 + Section 4.3.2's prose:
    prefer a policy whose entire unauthorized exposure is non-modifying,
    minimizing that exposure count; if every applicable policy grants some
    modifying op reaching outside R', fall back to compensatory read (grant
    ri, downgrade the actual permission for this request to read-only)."""
    ep = [p for p in policies if resource_matches(p, resource)]
    if not ep:
        return IntegrityOutcome(resource.id, "deny", None, None)

    safe: List[tuple] = []   # (policy, leak_count) whose leaked resources are all non-modifying exposure
    all_scored: List[tuple] = []
    for p in ep:
        leak = find_resources(p, all_resources) - shared
        all_scored.append((p, len(leak)))
        is_modifying_policy = len(p.acts & modifying_ops) > 0
        if len(leak) == 0 or not is_modifying_policy:
            safe.append((p, len(leak)))

    if safe:
        safe.sort(key=lambda t: (t[1], t[0].id))
        best_policy, best_leak = safe[0]
        return IntegrityOutcome(resource.id, "grant", best_policy.id, best_leak)

    # No safe policy: compensatory read on the least-exposing available policy.
    all_scored.sort(key=lambda t: (t[1], t[0].id))
    best_policy, best_leak = all_scored[0]
    return IntegrityOutcome(resource.id, "compensatory_read", best_policy.id, best_leak)


@dataclass
class PhaseIIResult:
    confidentiality: Dict[str, ConfidentialityOutcome]
    integrity: Dict[str, IntegrityOutcome]


def syn_abac_phase2(
    undeliverable: FrozenSet[str],
    all_resources: Sequence[Entity],
    shared: FrozenSet[str],
    policies: Sequence[Rule],
    choice: str,
    tau: int = 5,
    conf_level_fn: Callable[[Entity], int] = default_conf_level,
    modifying_ops: FrozenSet[str] = DEFAULT_MODIFYING_OPS,
) -> PhaseIIResult:
    """Algorithm 8, scoped to Phase-I's undeliverable set (R_D) -- see module
    docstring. `choice` in {"confidentiality", "integrity"}; anything else
    (including "availability") yields an empty perm_set per the published
    pseudocode's else-branch, since Algorithm 8 itself implements only the
    first two strategies (availability's resource-removal logic in Section
    4.3.3 is applied by the caller, not inside Algorithm 8)."""
    by_id = {r.id: r for r in all_resources}
    conf: Dict[str, ConfidentialityOutcome] = {}
    integ: Dict[str, IntegrityOutcome] = {}
    for rid in undeliverable:
        r = by_id.get(rid)
        if r is None:
            continue
        if choice == "confidentiality":
            conf[rid] = preserve_confidentiality(r, all_resources, shared, policies, tau, conf_level_fn)
        elif choice == "integrity":
            integ[rid] = preserve_integrity(r, all_resources, shared, policies, modifying_ops)
    return PhaseIIResult(confidentiality=conf, integrity=integ)


def apply_availability_tradeoff(undeliverable: FrozenSet[str]) -> FrozenSet[str]:
    """Section 4.3.3: resources with no safe isolated policy are simply
    dropped from the shared set. After Phase-I, `undeliverable` already *is*
    exactly that set, so this trade-off is the identity map surfaced as its
    own function for symmetry with confidentiality/integrity and for the
    experiment runner to report R'' = R' - undeliverable explicitly."""
    return undeliverable
