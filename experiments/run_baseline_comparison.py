"""
Compare Syn-ABAC Phase-I against Das et al.'s PolAP [33] on identical
access targets, across the 9 real/benchmark case-study datasets.
Addresses review gap C1 (no real competing-method baseline) directly.
"""
from __future__ import annotations

import csv
import random
from dataclasses import asdict
from pathlib import Path

from grace.abac_parser import parse_abac_file
from grace.compare_polap import compare_on_target
from grace.phase1 import syn_abac_phase1

DATASETS_DIR = Path(__file__).resolve().parent.parent / "datasets"
RESULTS_DIR = Path(__file__).resolve().parent / "results"

CASE_STUDIES = {
    "healthcare": DATASETS_DIR / "abac-lab" / "DATASETS" / "abac-datasets" / "healthcare.abac",
    "university": DATASETS_DIR / "abac-lab" / "DATASETS" / "abac-datasets" / "university.abac",
    "project-management": DATASETS_DIR / "abac-lab" / "DATASETS" / "abac-datasets" / "project-management.abac",
    "workforce": DATASETS_DIR / "abac-lab" / "DATASETS" / "abac-datasets" / "workforce.abac",
    "edocument": DATASETS_DIR / "abac-lab" / "DATASETS" / "abac-datasets" / "edocument.abac",
    "xustoller-healthcare": DATASETS_DIR / "xu-stoller" / "ABAC-Mining" / "ABACMining" / "case-studies" / "healthcare.abac",
    "xustoller-university": DATASETS_DIR / "xu-stoller" / "ABAC-Mining" / "ABACMining" / "case-studies" / "university.abac",
    "xustoller-project-management": DATASETS_DIR / "xu-stoller" / "ABAC-Mining" / "ABACMining" / "case-studies" / "project-management.abac",
    "xustoller-online-video": DATASETS_DIR / "xu-stoller" / "ABAC-Mining" / "ABACMining" / "case-studies" / "online-video.abac",
}

FRACTIONS = [0.3, 0.5, 0.7, 0.9]
TRIALS = 10


def main() -> None:
    all_rows = []
    for name, path in CASE_STUDIES.items():
        parsed = parse_abac_file(path)
        resources = list(parsed.resources.values())
        policies = parsed.rules
        if not resources or not policies:
            continue
        print(f"=== {name} ===")
        for frac in FRACTIONS:
            for t in range(TRIALS):
                rng = random.Random(t)
                all_ids = [r.id for r in resources]
                k = max(1, int(round(frac * len(all_ids))))
                shared = frozenset(rng.sample(all_ids, k))
                p1 = syn_abac_phase1(shared, resources, policies)
                cmp = compare_on_target(name, frac, t, shared, p1, resources, policies)
                all_rows.append(cmp)
        rows = [r for r in all_rows if r.dataset == name]
        n = len(rows)
        avg_syn_pol = sum(r.synabac_policy_count for r in rows) / n
        avg_syn_deliv = sum(r.synabac_deliverable_count for r in rows) / n
        n_bulk_undeliv = len([r for r in rows if r.polap_bulk_undeliverable])
        avg_decomp_deliverable = sum(r.polap_decomposed_deliverable_count for r in rows) / n
        avg_decomp_rules = sum(r.polap_decomposed_rule_count for r in rows) / n
        avg_target = sum(r.target_pairs for r in rows) / n
        print(f"  target={avg_target:.1f}  Syn-ABAC delivered={avg_syn_deliv:.2f} using {avg_syn_pol:.2f} policies  "
              f"PolAP bulk undeliverable {n_bulk_undeliv}/{n}  "
              f"PolAP decomposed delivered={avg_decomp_deliverable:.2f} using {avg_decomp_rules:.2f} rules")

    out_path = RESULTS_DIR / "polap_comparison.csv"
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(asdict(all_rows[0]).keys()))
        w.writeheader()
        for r in all_rows:
            w.writerow(asdict(r))
    print(f"\n-> {out_path} ({len(all_rows)} rows)")


if __name__ == "__main__":
    main()
