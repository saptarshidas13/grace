# Datasets for Syn-ABAC Experiments

Downloaded for the "compare against baselines / use real & established benchmark
data" gap identified in `Review_and_Revision_Notes.md` (item C1/C5). Everything
here is public and license-compatible with academic reuse (see per-source notes).

## 1. `abac-lab/DATASETS/` — ABAC Lab standardized benchmarks (SACMAT 2025)
Source: https://github.com/ABAC-Lab-Admin/ABAC-Lab (cloned in full; app code + datasets)
Paper: https://arxiv.org/abs/2505.08209

Five ready-to-use `.abac` policy files (subject attributes, resource attributes,
and rules — same name-value-pair structure as Syn-ABAC's `AU`/`AR`/`AP` model
in §3.1 of the paper):

| Dataset | Path | Size |
|---|---|---|
| University | `abac-lab/DATASETS/abac-datasets/university.abac` | 148 lines |
| Workforce | `abac-lab/DATASETS/abac-datasets/workforce.abac` | 812 lines |
| Healthcare | `abac-lab/DATASETS/abac-datasets/healthcare.abac` | 101 lines |
| Project Management | `abac-lab/DATASETS/abac-datasets/project-management.abac` | 128 lines |
| Edocument | `abac-lab/DATASETS/abac-datasets/edocument.abac` | 892 lines |

Each also has its own subfolder (`abac-lab/DATASETS/<name>/`) with a per-dataset
`README.md` describing the attribute schema — read that before parsing.

## 2. `xu-stoller/ABAC-Mining/` — original Xu & Stoller policy-mining case studies
Source: https://www3.cs.stonybrook.edu/~stoller/software/ABAC-Mining.zip
Papers: IEEE TDSC 2015 "Mining Attribute-Based Access Control Policies" (arXiv:1306.2401);
these `healthcare`/`university`/`project-management` case studies are the *de facto*
standard benchmark cited by nearly every later ABAC policy-mining paper, including
several in Syn-ABAC's own related work (Karimi & Joshi, Alohaly & Takabi, etc.).
License: GPLv3 (`ABACMining/COPYING`).

Key contents under `ABAC-Mining/ABACMining/`:
- `case-studies/healthcare.abac`, `healthcare-fraction.abac`, `university.abac`,
  `project-management.abac`, `project-management1.abac` (Progol-compatible
  variant — see `case-studies/notes.txt`), `online-video.abac`
- `gen-case-studies/{healthcare,university,projectmanagement}/` — policy-set
  generators/variants at different scales
- `ir-gen-case-studies/` — incomplete-rule generation variants (useful for
  noise/robustness experiments)
- `noise_experiment_results/*.csv` and `generalization-error/*.output` — the
  original paper's own experimental result logs, useful as a sanity check when
  you replicate their setup for a baseline comparison
- `lib/` — Apache Commons Math jars used by their Java implementation (not
  needed for a from-scratch reimplementation)

## 3. `xu-stoller/ABACMiningFromLogs/` — ABAC-from-logs case study (bonus)
Source: https://www3.cs.stonybrook.edu/~stoller/software/ABACMiningFromLogs.zip
Paper: DBSec 2014 "Mining Attribute-Based Access Control Policies from Logs" (arXiv:1403.5715)
Contains an `atm` case-study (access logs + ML-based mining, Weka-based). This
one is large (~128MB, mostly jar libraries) and is a different problem framing
(mining policies from logs rather than adapting/sharing existing policies), so
treat it as a secondary/optional resource — lower priority than #1 and #2 for
directly testing Syn-ABAC.
License: check `ABACMiningFromLogs/COPYING` once fully extracted.

## 4. `amazon-employee-access/` — real-world access-control dataset
Source: OpenML dataset id 4135 (mirrors the Kaggle "Amazon.com — Employee Access
Challenge" competition data), https://www.openml.org/d/4135
Files: `amazon_employee_access.arff` (original) and `amazon_employee_access.csv`
(converted, 32,769 rows + header).

This is **real historical access-request data** (Amazon, 2010–2011): each row is
a resource-access decision (`ACTION` / `target`: approved=1) keyed by
`RESOURCE, MGR_ID, ROLE_ROLLUP_1, ROLE_ROLLUP_2, ROLE_DEPTNAME, ROLE_TITLE,
ROLE_FAMILY_DESC, ROLE_FAMILY, ROLE_CODE`. It's role-based rather than
attribute-based as published, but it's the standard "real data" citation used
across the ABAC/RBAC policy-mining literature (including papers Syn-ABAC cites,
e.g. Xu & Stoller) — useful for the external-validity gap flagged in review
note C5, either directly (treating role fields as attributes) or as a source to
derive a synthetic-but-realistic ABAC policy set from.
License: Kaggle competition data, permitted for research/benchmarking use;
do not redistribute commercially.

---

## Suggested use against the review gaps

- **C1 (no baseline)**: run Syn-ABAC's Phase-I MSCP-based policy selection
  against the *same* healthcare/university/project-management case studies
  Xu & Stoller (and later ABAC-mining papers) use — directly comparable numbers
  without inventing a new synthetic generator.
- **C5 (no real-world validation)**: use `amazon-employee-access/` as the one
  real (non-synthetic-policy) dataset; report Syn-ABAC behavior on it alongside
  the synthetic sweeps already in the paper (Tables 8–11).
- Keep the paper's own synthetic generator too — it's still valid for the
  scalability sweep (10–1000 users, 100–5000 resources); real/benchmark data
  above is for the *credibility* gap, not a replacement.

Next step (not done yet): write the parser/loader for `.abac` files and the
Amazon CSV so both feed into the same internal `(U, R, E, P)` representation
Syn-ABAC's algorithms expect — that's the actual experiment coding, starting
next.
