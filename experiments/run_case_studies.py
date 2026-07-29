"""
Run the Syn-ABAC experiment sweep on the real-world ABAC policy-mining case
studies (Xu & Stoller; ABAC Lab) -- addresses review gaps C1 (baseline),
C3 (runtime), C4 (variance), C5 (real-world validation, at least
established-benchmark validation since these are single-org policy sets).
"""
from __future__ import annotations

from pathlib import Path

from grace.abac_parser import parse_abac_file
from grace.runner import run_sweep, write_csv

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

FRACTIONS = [0.1, 0.3, 0.5, 0.7, 0.9]
TRIALS = 10


def main() -> None:
    for name, path in CASE_STUDIES.items():
        print(f"=== {name} ({path.name}) ===")
        parsed = parse_abac_file(path)
        resources = list(parsed.resources.values())
        policies = parsed.rules
        print(f"  users={len(parsed.users)} resources={len(resources)} policies={len(policies)}")
        if not resources or not policies:
            print("  skipping (empty resources or policies)")
            continue
        results = run_sweep(name, resources, policies, FRACTIONS, TRIALS, seed0=0,
                             exact_cover_max_candidates=800)
        write_csv(results, RESULTS_DIR / f"{name}.csv")
        print(f"  -> results/{name}.csv ({len(results)} rows)")


if __name__ == "__main__":
    main()
