"""
Experiment orchestration: sample shared-resource subsets R' at varying sizes,
run Syn-ABAC Phase-I/II plus the naive baseline and (where tractable) the
exact ILP cover, time everything, and run the SoD/BoD stress test -- then
write one CSV row per (dataset, |R'| fraction, trial).

Addresses review gaps C1 (no baseline), C3 (no runtime), C4 (single run, no
variance) by construction: every (dataset, fraction) cell gets `trials`
independent random draws of R', each timed.
"""
from __future__ import annotations

import csv
import random
import time
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import List, Sequence

from .baselines import exact_min_cover, naive_baseline
from .model import Entity, Rule
from .phase1 import syn_abac_phase1
from .phase2 import syn_abac_phase2
from .sod_bod import verify_sod_bod


@dataclass
class TrialResult:
    dataset: str
    n_resources_total: int
    n_policies_total: int
    n_users_total: int
    shared_fraction: float
    shared_count: int
    trial_seed: int
    phase1_time_s: float
    phase1_policy_count: int
    phase1_undeliverable_count: int
    naive_policy_count: int
    naive_undeliverable_count: int
    naive_leaked_resource_count: int
    exact_cover_size: object          # int or None
    exact_cover_skipped_reason: object
    greedy_optimality_gap: object     # phase1_policy_count - exact_cover_size, or None
    phase2_conf_allow: int
    phase2_conf_deny: int
    phase2_integ_grant: int
    phase2_integ_compensatory_read: int
    sod_pairs_tested: int
    sod_violations: int
    bod_pairs_tested: int
    bod_unsatisfied: int


def _naive_leak_count(naive_result, all_resources: Sequence[Entity], policies: Sequence[Rule], shared) -> int:
    from .phase1 import find_resources
    by_id = {p.id: p for p in policies}
    leaked = set()
    for pid in naive_result.policy_ids:
        p = by_id.get(pid)
        if p is None:
            continue
        leaked |= (find_resources(p, all_resources) - shared)
    return len(leaked)


def run_trial(
    dataset_name: str,
    all_resources: Sequence[Entity],
    policies: Sequence[Rule],
    shared_fraction: float,
    seed: int,
    exact_cover_max_candidates: int = 400,
    tau: int = 5,
) -> TrialResult:
    rng = random.Random(seed)
    all_ids = [r.id for r in all_resources]
    k = max(1, int(round(shared_fraction * len(all_ids))))
    shared = frozenset(rng.sample(all_ids, k))

    t0 = time.perf_counter()
    p1 = syn_abac_phase1(shared, all_resources, policies)
    phase1_time = time.perf_counter() - t0

    naive = naive_baseline(shared, all_resources, policies)
    naive_leak = _naive_leak_count(naive, all_resources, policies, shared)

    exact = exact_min_cover(shared - p1.undeliverable, p1.all_candidates, max_candidates=exact_cover_max_candidates)
    gap = (len(p1.covering_sets) - exact.size) if exact.solved and exact.size is not None else None

    p2_conf = syn_abac_phase2(p1.undeliverable, all_resources, shared, policies, choice="confidentiality", tau=tau)
    p2_integ = syn_abac_phase2(p1.undeliverable, all_resources, shared, policies, choice="integrity")

    conf_allow = sum(1 for o in p2_conf.confidentiality.values() if o.decision == "allow")
    conf_deny = sum(1 for o in p2_conf.confidentiality.values() if o.decision == "deny")
    integ_grant = sum(1 for o in p2_integ.integrity.values() if o.decision == "grant")
    integ_comp = sum(1 for o in p2_integ.integrity.values() if o.decision == "compensatory_read")

    sod_bod = verify_sod_bod(p1, all_resources, policies, n_conflict_pairs=30, seed=seed)

    return TrialResult(
        dataset=dataset_name,
        n_resources_total=len(all_resources),
        n_policies_total=len(policies),
        n_users_total=-1,
        shared_fraction=shared_fraction,
        shared_count=k,
        trial_seed=seed,
        phase1_time_s=phase1_time,
        phase1_policy_count=len(p1.policy_ids),
        phase1_undeliverable_count=len(p1.undeliverable),
        naive_policy_count=len(naive.policy_ids),
        naive_undeliverable_count=len(naive.undeliverable),
        naive_leaked_resource_count=naive_leak,
        exact_cover_size=exact.size,
        exact_cover_skipped_reason=exact.skipped_reason,
        greedy_optimality_gap=gap,
        phase2_conf_allow=conf_allow,
        phase2_conf_deny=conf_deny,
        phase2_integ_grant=integ_grant,
        phase2_integ_compensatory_read=integ_comp,
        sod_pairs_tested=sod_bod.n_sod_pairs_tested,
        sod_violations=len(sod_bod.sod_violations),
        bod_pairs_tested=sod_bod.n_bod_pairs_tested,
        bod_unsatisfied=len(sod_bod.bod_unsatisfied),
    )


def run_sweep(
    dataset_name: str,
    all_resources: Sequence[Entity],
    policies: Sequence[Rule],
    fractions: Sequence[float],
    trials: int,
    seed0: int = 0,
    exact_cover_max_candidates: int = 400,
) -> List[TrialResult]:
    results = []
    for frac in fractions:
        for t in range(trials):
            seed = seed0 + t
            r = run_trial(dataset_name, all_resources, policies, frac, seed,
                           exact_cover_max_candidates=exact_cover_max_candidates)
            results.append(r)
            print(f"  {dataset_name} frac={frac:.2f} trial={t} "
                  f"phase1_time={r.phase1_time_s:.4f}s policies={r.phase1_policy_count} "
                  f"undeliverable={r.phase1_undeliverable_count} gap={r.greedy_optimality_gap}")
    return results


def write_csv(results: List[TrialResult], out_path: str | Path) -> None:
    out_path = Path(out_path)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    if not results:
        return
    fieldnames = list(asdict(results[0]).keys())
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        for r in results:
            w.writerow(asdict(r))
