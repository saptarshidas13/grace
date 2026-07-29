"""
Run the Syn-ABAC experiment sweep on the real Amazon Employee Access dataset
(derived ABAC instance: 6282 role-class policies/users, 7226 resources) --
addresses review gap C5 (no real-world, non-synthetic-policy validation) and
demonstrates scale beyond the paper's own tested maximum (paper: up to 5000
resources / 200 policies; here: 7226 resources / 6282 policies, real data).

Smaller fraction set and trial count than the case studies, and a lower
exact-cover cutoff, since this instance is ~30x larger.
"""
from __future__ import annotations

from pathlib import Path

from grace.amazon_loader import load_amazon_as_abac
from grace.runner import run_sweep, write_csv

DATASETS_DIR = Path(__file__).resolve().parent.parent / "datasets"
RESULTS_DIR = Path(__file__).resolve().parent / "results"
CSV_PATH = DATASETS_DIR / "amazon-employee-access" / "amazon_employee_access.csv"

FRACTIONS = [0.01, 0.05, 0.1, 0.15]
TRIALS = 3


def main() -> None:
    print("Loading Amazon Employee Access dataset...")
    ap = load_amazon_as_abac(CSV_PATH)
    resources = list(ap.resources.values())
    policies = ap.rules
    print(f"users(role-classes)={len(ap.users)} resources={len(resources)} policies={len(policies)}")

    results = run_sweep("amazon-employee-access", resources, policies, FRACTIONS, TRIALS, seed0=0,
                         exact_cover_max_candidates=100)
    write_csv(results, RESULTS_DIR / "amazon-employee-access.csv")
    print(f"-> results/amazon-employee-access.csv ({len(results)} rows)")


if __name__ == "__main__":
    main()
