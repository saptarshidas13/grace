# Reproducing the Results

Step-by-step instructions to regenerate every number, table, and figure
reported for GRACE from scratch by running the code in this folder.
Every command below assumes your working directory is `experiments/`
(the folder this file is in).

## 0. Prerequisites

- Python 3.10+ (developed and tested on Python 3.14.4).
- The `datasets/` folder one level up (`../datasets/`) must be present —
  it holds the real/benchmark ABAC policy sets and the Amazon Employee
  Access CSV that every script below reads. See `../datasets/README.md`
  if it's missing.

## 1. Install dependencies

```bash
pip install -r requirements.txt
```

Installs `pandas`, `pulp` (ILP solver for the exact-optimum baseline),
`numpy`, and `matplotlib`.

## 2. Verify correctness first

Before trusting any experimental number, run the test suite. These tests
validate the implementation against the paper's own worked numerical
examples (Sect. 3.3's illustrative example and the Phase-II confidentiality/
integrity walkthroughs) and against the two baseline re-implementations'
own published examples — not synthetic sanity checks, but reproductions
of specific numbers the source papers themselves report.

```bash
cd tests
python test_paper_example.py   # reproduces Table 6/7-equivalent resource-to-policy mapping
python test_phase1_toy.py      # reproduces the RePo-tree pruning + greedy MSCP walkthrough
python test_polap.py           # validates the PolAP [33] re-implementation
python test_ontology.py        # validates the ontology-relaxation [35] re-implementation
cd ..
```

Each script prints `... passed` on success and raises an `AssertionError`
immediately on the first mismatch — there is no partial-pass state to
misread.

## 3. Run the main experiment sweep (9 real/benchmark datasets)

```bash
python run_case_studies.py
```

Runs Phase-I (with naive-baseline and exact-ILP-optimum comparison) across
all 9 real/established-benchmark ABAC policy sets (ABAC Lab: healthcare,
university, project-management, workforce, e-document; Xu & Stoller [20]:
healthcare, university, project-management, online-video), sweeping the
shared fraction $|R'|/|R| \in \{0.1, 0.3, 0.5, 0.7, 0.9\}$ with 10
independent random trials per fraction. Takes well under a minute total.

**Output:** `results/<dataset-name>.csv`, one row per (fraction, trial).

## 4. Run the Amazon-derived real-world sweep

```bash
python run_amazon.py
```

Loads the real Amazon Employee Access dataset (32,769 records), derives
the 6,282-policy / 7,226-resource ABAC instance, and sweeps
$|R'|/|R| \in \{0.01, 0.05, 0.1, 0.15\}$ with 3 trials per fraction.

**This is slow — budget 15–20 minutes.** Phase-I time alone runs from
~3.5s (smallest fraction) up to ~30s per trial (largest fraction) at this
scale; the full 12-trial sweep took approximately 3.5 minutes of pure
Phase-I compute in the original run, plus dataset-loading and exact-ILP
overhead on top. Progress prints per-trial as it goes
(`amazon-employee-access frac=... trial=... phase1_time=...`), so you can
watch it advance rather than waiting on a silent process; `amazon_run.log`
in this folder is the transcript from the original run, for comparison.

**Output:** `results/amazon-employee-access.csv`.

## 5. Run the PolAP baseline comparison

```bash
python run_baseline_comparison.py
```

Re-runs Phase-I on the 9 real/benchmark datasets (fractions
$\{0.3, 0.5, 0.7, 0.9\}$, 10 trials each) and, on the *same* sampled
shared sets, runs Das et al.'s PolAP [33] re-implementation
(`grace/baseline_polap.py`) in both its literal bulk (all-or-nothing)
semantics and a per-resource decomposed semantics, for a direct,
paired comparison. Prints a per-dataset summary line as it runs.

**Output:** `results/polap_comparison.csv`.

## 6. Run the ontology-relaxation capability-gap demo

```bash
python run_ontology_demo.py
```

A short, self-contained script (no CSV output, prints directly to
stdout) reproducing Gupta & Sural's [35] own worked example — the one
where a guest user's title/department mismatch is only resolved via
their distance-bounded ontology relaxation, never via GRACE's own
literal-match evaluator, regardless of distance. Confirms the
minimum-relaxation-distance result ($D=2$) referenced in the paper's
ontology-comparison section exactly.

## 7. Generate the figures

```bash
python make_figures.py
```

Reads every `results/*.csv` written by steps 3–4 and writes three
figures (PDF + PNG) to `results/figures/`:

| File | Content |
|---|---|
| `runtime_scaling.pdf` | Phase-I runtime vs. shared-set size, all 10 data sources, log–log, small multiples |
| `undeliverable_rate.pdf` | Share of $R'$ left undeliverable by Phase-I alone, vs. shared fraction |
| `policy_reduction.pdf` | GRACE vs. naive-baseline policy count and leakage, at each dataset's largest swept fraction |

Run this *after* steps 3 and 4 — it reads their CSV output and does not
regenerate any experimental data itself.

## Run order summary

```bash
pip install -r requirements.txt
cd tests && python test_paper_example.py && python test_phase1_toy.py && python test_polap.py && python test_ontology.py && cd ..
python run_case_studies.py
python run_amazon.py              # slow -- ~15-20 min
python run_baseline_comparison.py
python run_ontology_demo.py
python make_figures.py
```

## Where each result in the paper comes from

| Paper artifact | Source |
|---|---|
| Table (dataset sizes) | `../datasets/` contents, sizes printed by `run_case_studies.py`/`run_amazon.py` |
| Fig. `runtime` | `results/figures/runtime_scaling.pdf` (step 7) |
| Fig. `undeliverable` | `results/figures/undeliverable_rate.pdf` (step 7) |
| Fig. `policy-reduction` | `results/figures/policy_reduction.pdf` (step 7) |
| Optimality-gap table | `phase1_time_s`/exact-cover columns in `results/*.csv` (step 3–4) |
| Phase-II recovery-rate table | `phase2_conf_*`/`phase2_integ_*` columns in `results/*.csv` (step 3) |
| SoD stress-test table | `sod_pairs_tested`/`sod_violations` columns in `results/*.csv` (step 3) |
| PolAP comparison table | `results/polap_comparison.csv` (step 5) |
| Ontology-relaxation comparison | stdout of `run_ontology_demo.py` (step 6) |
| Appendix per-fraction tables | all of `results/*.csv` (steps 3–4), aggregated per fraction |

`results/SUMMARY.md`, `results/FINDINGS_SUMMARY.md`, and
`results/BASELINE_COMPARISON_FINDINGS.md` are written analysis of the
CSV output above, not scripts — nothing to re-run for those; they'll go
stale only if you change the underlying experiment code and re-run steps
3–6 without updating them by hand.

## Notes on non-determinism

Every sweep seeds its random shared-set sampling per trial index
(`random.Random(t)` / `seed=t`), so re-running any of steps 3–6 should
reproduce the same shared-set draws and, in turn, the same GRACE/naive/
PolAP outcomes exactly. `phase1_time_s` (and the Amazon step's wall-clock
timing) will naturally vary run to run with your machine's load — the
qualitative scaling shape (Fig. `runtime`) is what matters, not the exact
millisecond values.
