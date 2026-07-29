# Findings summary: how the experiments align with the paper's objectives

Basis: 10 datasets (9 established real/benchmark ABAC policy sets + real Amazon
access-log data), 480 trials total, `experiments/results/*.csv`, figures in
`experiments/results/figures/`. Full methodology in `IMPLEMENTATION_NOTES.md`.

## The paper's stated objectives (Abstract / §1)

1. Resolve attribute mismatch and enable secure cross-org resource sharing
   while respecting the resource-owning org's own policies.
2. Enforce the CIA triad.
3. Preserve Separation-of-Duty (SoD) and Binding-of-Duty (BoD).
4. Prove the underlying problem NP-Complete, then give a practical algorithm.
5. Show "wide applicability... for varied security requirements" via
   experimental evaluation.

## Objective-by-objective result

### 1. Minimal cross-org sharing without leakage — confirmed and quantified
The paper asserts Phase-I finds a minimal, leak-free policy set. Across every
dataset, Phase-I matches or beats the naive (no-minimization) baseline on
policy count **and never leaks a single unauthorized resource**, where naive
leaks by construction. On real Amazon data at the largest tested scale: naive
uses 763 policies and leaks 3,511 resources; Phase-I uses 132 policies and
leaks 0 (`results/figures/policy_reduction.pdf`). This is the paper's central
claim and it holds up strongly under real data at a scale (7,226 resources)
5x beyond anything the paper itself tested.

### 2. NP-Completeness + practical tractability — confirmed
No trial contradicted the hardness result, and more usefully: the greedy
Phase-I heuristic **matched the exact ILP optimum in every instance where
exact solving was tractable to check** (all 500 case-study trials solved
exactly; 5/12 Amazon trials, the rest exceeded the ILP candidate cap at that
scale). The paper cites the greedy H(n) approximation bound but never
measures how close it actually lands — this is empirical evidence it lands
exactly on optimum in practice, worth adding to the manuscript.

### 3. CIA trade-offs (Phase-II) — confirmed as *necessary*, and shown to be the
### load-bearing mechanism, not the edge case the paper frames it as
This is the most important reframing the data suggests. The paper presents
Phase-II (§4.3) as handling "specific cases" where Phase-I's default
mechanism "may not suffice." In the experiments, Phase-I alone left
**73–100% of the shared set undeliverable** at low-to-moderate sharing
fractions, across every real dataset (`results/figures/undeliverable_rate.pdf`).
Undeliverability falls only as the shared fraction approaches the *entire*
resource set (because a small random subset rarely happens to fully contain
a real policy's whole coverage footprint — real policies are broad relative
to typical resources). Practical implication: **for realistic partial
cross-org sharing, Phase-II is not an edge-case fallback — it is what makes
Syn-ABAC usable at all.** The manuscript's framing should change accordingly.

Within Phase-II, the two strategies behave very differently, worth reporting
separately rather than as one "CIA trade-off" claim:
- **Confidentiality** (τ=5 default threshold): recovers only **35.2%**
  (8,238 / 23,406) of otherwise-undeliverable resources — the rest are
  correctly denied because their risk score exceeds τ. This is a genuine,
  threshold-sensitive trade-off, and the paper should report results at
  multiple τ values rather than the single worked example it currently gives.
- **Integrity**: recovers **99.1%** (23,190 grants + compensatory-reads out
  of 23,406) — almost everything undeliverable in Phase-I becomes accessible
  under the integrity strategy, mostly via outright grant (216 needed the
  compensatory-read downgrade). Integrity is far more permissive in practice
  than confidentiality; the paper doesn't currently compare the two
  strategies' recovery rates against each other, and it's a striking
  asymmetry worth stating explicitly.

### 4. SoD preservation — nuanced, not a clean confirmation
The paper asserts (§4.2.4) that mapping each selected policy to a
representative user "ensures" SoD; it never proves this. Under the new
empirical stress test (`synabac/sod_bod.py`): **71/6,450 (1.10%) of randomly
sampled conflict pairs were violated**, concentrated almost entirely in one
dataset (**workforce: 4.20%**; university/xustoller-university: 0.27%; the
other six datasets: 0.00%). Mechanism, confirmed by tracing specific cases:
a user synthesized to satisfy one selected policy's subject condition can
*also* satisfy a second, broader/near-wildcard policy in the same selected
set, picking up that policy's operations too. **Implication for the paper**:
the SoD claim should be qualified — it holds empirically in most tested
settings, but not universally, and the failure mode (broad/wildcard subject
conditions in the selected policy set) is identifiable and could motivate an
explicit SoD-safety check as future work, rather than an unconditional
guarantee.

### 5. BoD preservation — test inconclusive as currently designed (own limitation)
99.02% of randomly sampled operation pairs showed no single induced user
authorized for both. This number is **not strong evidence against the
paper's BoD claim** — it's largely an artifact of testing *random* operation
pairs rather than pairs an organization would actually declare as
bound-by-duty (which are typically both granted by the *same* policy by
design, and would trivially satisfy BoD under the induced-user mapping).
Flagged in `IMPLEMENTATION_NOTES.md` as a methodology gap: a same-policy vs.
cross-policy pair split would give a much more meaningful BoD result. Treat
the current BoD number as "not yet tested properly," not as a finding.

### 6. Wide applicability across diverse datasets — confirmed and substantially strengthened
The paper's own evaluation used only its own synthetic generator. These
experiments add 9 established literature benchmarks (Xu & Stoller;
ABAC Lab/SACMAT 2025) spanning healthcare, university, project-management,
workforce, e-document, and online-video domains, plus real 2010-2011 Amazon
access-log data (32,769 real access decisions, derived into a 6,282-policy /
7,226-resource instance — larger than the paper's own largest synthetic
configuration). This directly answers the original review's C1/C5 gaps
(no baseline, no real-world validation) and materially strengthens the
"wide applicability" claim with evidence beyond a self-generated dataset.

## Overall implications for the revision

- **Strengthen, with numbers**: minimal-cover correctness, zero-leakage vs.
  naive, greedy-optimality, and real/benchmark-dataset applicability all hold
  up and now have concrete, citable figures instead of only synthetic sweeps.
- **Reframe**: Phase-II should be presented as core to practical deployment,
  not a corner case — the current framing undersells the paper's own best
  result (integrity recovery works almost 99% of the time).
- **Qualify**: the SoD guarantee should be stated conditionally (holds absent
  broad/wildcard-subject policies in the selected set) rather than
  unconditionally; report the confidentiality threshold τ as a swept
  parameter, not a single worked value.
- **Flag as future work, not a settled result**: BoD needs a properly
  designed empirical test (same-policy vs. cross-policy conflict pairs)
  before any claim about it — random pairs weren't the right instrument.

## Caveats on the experiments themselves

- Confidentiality results are reported at a single default τ=5; a τ-sweep
  would make the 35.2% recovery figure much more informative.
- The "modifying operations" vocabulary used to classify integrity risk
  (write/update/delete/create/addItem/...) is a reasonable but explicit
  choice not given by the paper — see IMPLEMENTATION_NOTES.md #2.
- Amazon-derived policies use a single generic "access" operation (the log
  is binary approved/not), so SoD/BoD testing wasn't run on that dataset
  (no operation diversity to sample conflict pairs from).
- Exact-cover optimality checks were capped (400 candidates for case
  studies, 100 for Amazon) for ILP tractability; gaps are unmeasured, not
  necessarily zero, beyond that cap.
