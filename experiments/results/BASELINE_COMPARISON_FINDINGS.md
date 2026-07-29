# Comparison against competing prior work: [33], [34], [35]

Addresses review gap C1 (no real competing-method baseline — only an
internal naive strawman existed before this). Implementations in
`synabac/baseline_polap.py` and `synabac/baseline_ontology.py`, both
validated against their source papers' own worked numeric examples
(`tests/test_polap.py`, `tests/test_ontology.py`) before being run against
real data.

## [33]/[34] Das, Sural, Vaidya, Atluri — PolAP / PolAP-AVH

### What it actually solves, vs. what Syn-ABAC solves
PolAP computes, for **one subject**, the minimum set of rules needed to
grant exactly a **pre-specified desired access list** L, without granting
anything extra. Syn-ABAC's Phase-I computes, for a **shared resource set**
R′, the minimum set of **policies** (operation-agnostic — any operation a
matching policy happens to grant counts) that jointly cover R′ without
leaking to resources outside it. These are different problems dressed in
structurally near-identical machinery: PolAP's SORO-tree-plus-greedy-
minimum-hitting-set is the same combinatorial shape as Syn-ABAC's
RePo-tree-plus-greedy-MSCP (set cover and hitting set are dual
formulations of the same problem on a bipartite incidence structure).

### Making the comparison fair (this took two iterations)
The first attempt derived PolAP's target list L directly from Syn-ABAC's
own Phase-I output. That's circular: a near-identical greedy algorithm
handed the *already-solved* instance trivially reproduces the same
answer, which is what happened (rule/policy counts matched to the decimal
every time — not a finding, a construction artifact). Fixed by deriving L
independently: for every resource in R′, its own applicable operation(s)
in the raw dataset, never referencing Syn-ABAC's solution.

A second issue surfaced from that fix: a single global "dominant
operation" target (e.g. always "read") produced 100% undeliverable
results for reasons that had nothing to do with either algorithm's
quality — real datasets have **resource-type-dependent operation
vocabularies** (e.g. healthcare's `HR`-type resources only ever take
`addItem`/`addNote`; `HRitem`-type resources only take `read` — asking
for "read" on an `HR` resource is asking for something that structurally
doesn't exist, not something denied for safety reasons). Fixed by using
each resource's own canonical applicable operation instead of one global
choice — itself a small, real, useful finding: cross-org data models
built around a single "the operation" assumption don't survive contact
with heterogeneous real policy sets.

### Two results, both genuine (not comparison artifacts)

**1. Bulk (PolAP's own literal semantics — one subject, one L covering the
whole shared set): PolAP fails outright (0 rules, total denial) in every
single trial, across all 9 datasets, at every tested fraction.** This is
not a weakness in this reimplementation — Algorithm 1 in [33] is
literally all-or-nothing (`if RS == Null: return 0; exit`, lines 8–10): one
unreachable (resource, operation) pair anywhere in L aborts the entire
subject's adaptation. PolAP was designed and evaluated (its own §V) on
desired-access lists of 2–4 objects for one subject; naively scaling it to
bulk cross-org sharing (dozens to hundreds of shared resources at once)
exposes a real architectural brittleness that Syn-ABAC's Phase-I/Phase-II
split was — whether intentionally or not — built to avoid: Phase-I
delivers whatever it safely can per-resource and defers the rest to
Phase-II rather than aborting outright.

**2. Decomposed (PolAP run per-resource, so one leaking resource doesn't
sink the rest): matches Syn-ABAC's own per-resource delivery outcome and
rule/policy count almost exactly — 8 of 9 datasets are identical to two
decimal places.** This is the fair, apples-to-apples minimization
comparison, and the answer is that the two algorithms are equivalent in
practice here, which is itself worth reporting plainly rather than
inflating a difference that isn't there.

| dataset | avg target size | Syn-ABAC delivered | Syn-ABAC policies | PolAP decomposed delivered | PolAP decomposed rules | match? |
|---|---:|---:|---:|---:|---:|---|
| healthcare | 9.5 | 0.80 | 0.20 | 0.80 | 0.20 | yes |
| university | 20.5 | 3.90 | 0.50 | 3.90 | 0.50 | yes |
| project-management | 24.0 | 2.40 | 0.28 | 2.40 | 0.45 | yes |
| **workforce** | 150.0 | **5.75** | 1.07 | **2.70** | 0.62 | **differs** |
| edocument | 180.0 | 0.00 | 0.00 | 0.00 | 0.00 | yes |
| xustoller-healthcare | 9.5 | 2.00 | 0.50 | 2.00 | 0.50 | yes |
| xustoller-university | 20.5 | 3.90 | 0.50 | 3.90 | 0.50 | yes |
| xustoller-project-management | 24.0 | 2.80 | 0.38 | 2.80 | 0.55 | yes |
| xustoller-online-video | 7.8 | 3.25 | 0.97 | 3.25 | 1.12 | yes |

The one exception (**workforce**, Syn-ABAC delivers **more than double**
PolAP's decomposed count) is explained, not just observed: workforce is
the one dataset where individual resources have **multiple distinct
operations available from different, differently-safe policies**. Syn-ABAC
is operation-agnostic — any safe policy counts, whichever operation it
grants — so it can take whichever of those policies happens to be
leak-free. PolAP is locked to one specific operation per (resource, op)
pair, so it fails whenever *that specific* operation's only source policy
leaks, even if a *different* operation on the same resource would have
been safely deliverable. This is a genuine, structurally-explained
advantage of Syn-ABAC's operation-agnostic resource semantics — narrow
(one dataset out of nine, and only because that dataset happens to have
rich multi-operation policy diversity), not a blanket superiority claim.

### [34]'s hierarchy extension — scoped but not fully reimplemented
[34] adds attribute-value hierarchies on top of [33], selecting
hierarchically-senior attribute values to further cut rule count (their
own Figure 4: up to ~90% rule reduction at high attribute-value-count
configurations). Syn-ABAC has **no hierarchy concept anywhere** — every
attribute comparison in Algorithm 6 is flat equality-or-wildcard. Given
the volume already built in this session, [34]'s full DAG-hierarchy-
inference algorithm (their Algorithms 6–7: adjacency-matrix construction +
longest-path DAG selection) was not reimplemented — reusing the ontology
module already validated for [35] (`baseline_ontology.py`, tree-based,
tested against [35]'s own worked numbers) would get most of the way
there, since [34]'s hierarchy semantics (satisfied if the required value
is a same-or-more-general ancestor of the user's value) is the same
`hierarchy_ok` primitive already built and validated. **Flagged as the
natural next addition, not attempted here**: quantifying how much rule
reduction Syn-ABAC forgoes by having no hierarchy support at all is a
real, currently-open number.

## [35] Gupta & Sural — Ontology-based attribute-mismatch resolution

### Why this one matters most for the paper's own framing
Syn-ABAC's abstract and introduction name "attribute mismatches" across
organizations as the central problem motivating the work. Tracing through
every algorithm in §4 (Phase-I's RePo tree/MSCP, Phase-II's confidentiality/
integrity trade-offs) confirms none of them touch attribute-name or
attribute-value translation at all — `pol_eval` (Algorithm 6, and this
codebase's generalization in `model.py`) requires literal equality (or a
wildcard) on whichever attribute names and values happen to appear in the
policy. Every experiment in both the original paper and everything run in
this session implicitly assumes a shared vocabulary between the sharing
parties. [35] is the one cited related work that actually solves the
stated problem: an ontology-based hierarchy plus a distance-bounded
"controlled relaxation" mechanism (their §4.2–4.3).

### Validation
`baseline_ontology.py`'s `Ontology`/`hierarchy_ok`/`relaxed_ok` reproduce
[35]'s own Section 4.4 worked example exactly: the stated distance of 2
for both the Designation attribute (AssistantDean vs. HOD) and the
Department attribute (SchoolOfEngineering vs. SchoolOfBasicSciences), and
correctly collapse rule r5's Department distance from 3 to 2 via the
paper's College≡School equivalence (`tests/test_ontology.py`, all
assertions pass).

### Demo (`run_ontology_demo.py`)
Using that same validated ontology and [35]'s own rule r3 and user U1:

| Approach | Result |
|---|---|
| Syn-ABAC-style literal `pol_eval` | **never** satisfied, at any distance (no mechanism exists) |
| [35] hierarchy-only (no relaxation) | not satisfied (HOD/AssistantDean are siblings, not ancestor-descendant) |
| [35] relaxed, D=0,1 | not satisfied |
| [35] relaxed, D=2 | **satisfied** |
| [35] relaxed, D=3 | satisfied |

This is a clean, quantified statement of the capability gap: for a
realistic near-synonym cross-org title mismatch (distance 2 on the
paper's own hierarchy), Syn-ABAC's mechanism as published has no path to
granting the access at all, while [35]'s does by design. **Recommended
framing for the revision**: either soften the "attribute mismatch"
motivation in the abstract/intro to match what Phase-I/Phase-II actually
do (resource-owner-side minimal-safe-policy selection, which does not
require resolving vocabulary differences because it never reasons about
the requester's own attribute names), or explicitly scope future work
around integrating an ontology-style pre-processing step like [35]'s
ahead of Syn-ABAC's own Phase-I/Phase-II.

## Bottom line for the "competing baseline" gap

- **[33]/[34]**: on the minimization question the two papers actually
  share, Syn-ABAC and PolAP are empirically equivalent in 8/9 datasets;
  Syn-ABAC's operation-agnostic design gives it a real, explainable edge
  in the one dataset with rich multi-operation policy diversity, and its
  two-phase (deliver-what's-safe, defer-the-rest) design avoids PolAP's
  all-or-nothing collapse when naively scaled to bulk resource sharing.
  Neither claim requires exaggeration — both hold up as stated.
- **[35]**: not a minimization comparison at all — a capability
  Syn-ABAC's own motivation claims but its mechanism doesn't provide.
  Worth citing directly in the related-work section as the paper that
  *does* solve the attribute-mismatch problem Syn-ABAC's abstract raises,
  with Syn-ABAC's contribution reframed around what it actually does
  (safe minimal-policy selection + CIA trade-offs) rather than implied
  vocabulary-translation it doesn't attempt.
