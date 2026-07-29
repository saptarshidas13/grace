# Implementation notes: deviations from the published pseudocode

Every deviation below was needed to make an algorithm actually executable or
internally consistent, or was an explicit design fork put to the user via
AskUserQuestion before implementing. None change the paper's stated goals;
all are candidate clarifications/fixes for the revised manuscript.

## Confirmed with the user

1. **pol_eval / Algorithm 6 generalized.** The published version only
   supports single-valued exact-match or wildcard conditions. The real
   benchmark datasets (Xu & Stoller; ABAC Lab) need multi-valued attributes
   and cross-entity relational constraints (`aus=ars`, `aum>arm`, `aus[arm`,
   `aum]ars` -- the `cons` field of their rule grammar). Implemented as a
   strict generalization: `synabac/model.py`'s `resource_matches` /
   `full_request_allowed`, with the paper's flat model as the exact
   special case (`make_flat_rule`, exercised by `tests/test_paper_example.py`).

2. **Algorithm 10 (preserve_integrity) implemented per Equation 4 + prose,
   not the literal pseudocode.** The pseudocode minimizes raw `|PR_i|`
   (any unauthorized exposure, regardless of operation); Equation 4 and the
   Section 4.3.2 worked example only count *non-modifying* exposure, with a
   "compensatory read" fallback when every applicable policy also grants a
   modifying op outside R'. Implemented the Equation 4 / prose version in
   `synabac/phase2.py::preserve_integrity`, using an explicit
   `DEFAULT_MODIFYING_OPS` vocabulary (write/update/delete/create/addItem/
   addNote/addScore/createAppointment) since the paper never defines one.
   Verified: reproduces the paper's own numeric example exactly (resource
   `re`, policy `Pl` granting `write` on `rf` -> `compensatory_read`, see
   `tests/test_phase1_toy.py` / interactive check in the session).

3. **Phase-II (Algorithm 8) scoped to Phase-I's undeliverable set R_D, not
   literally every resource in R'.** The pseudocode's loop header says
   `for all r_i in R'`, but Section 4.3's prose states Phase-II exists to
   recover "the shared resources that remain inaccessible in Phase-I."
   Implemented `synabac/phase2.py::syn_abac_phase2` to take `undeliverable`
   (Phase-I's R_D) as its iteration set.

## Applied without a separate confirmation round (bug fixes, not design forks)

4. **Algorithm 2's MSCP call and R_D loop use the shared set R', not the
   full resource universe R.** As published, `MSCP(R - R_D, S)` covers the
   *entire* universe R with candidate sets S that are all subsets of R'
   (by construction, via Algorithm 3's own pruning) -- impossible whenever
   R' subset R. The evident intent (matches the rest of Section 4.2, "a
   minimal set of policies that facilitates access to the shared
   resources") is to cover R'. `synabac/phase1.py::syn_abac_phase1` loops
   over `shared` and covers `shared - undeliverable`.

5. **Algorithm 3/Algorithm 2's provenance handling.** The published
   Algorithm 2 (lines 13-18) recovers which policy produced a selected
   cover-set by re-running `find_resources(Pi, R)` for every policy and
   testing set equality against the chosen set -- redundant and fragile.
   `construct_repo_tree` instead returns `(policy_id, resource_set)` pairs
   directly (`RePoLeaf`), carrying provenance through construction. Same
   selection semantics: if two distinct policies cover an identical
   resource set, both are still retained as valid options.

6. **Algorithm 8's `choice` parameter.** Referenced in the pseudocode body
   but absent from its `Require:` list (a free variable). Made an explicit
   parameter of `syn_abac_phase2`.

7. **Algorithm 9's missing `else` branch.** No pseudocode path defines the
   outcome when `CRS* > tau`. Added an explicit `deny` outcome in
   `preserve_confidentiality`.

## Data-adaptation choices (not algorithm changes; documented for audit)

8. **Amazon Employee Access -> ABAC derivation** (`synabac/amazon_loader.py`):
   one synthetic "role-class" user per distinct 7-tuple of role columns
   (MGR_ID dropped -- near-unique per manager, would fragment into
   near-singleton policies); one resource per distinct RESOURCE id with no
   attributes beyond its own id (the dataset gives no resource-side
   attributes at all); one policy per role-tuple with >=1 approved row,
   resource condition `rid in {approved resource ids}`, single operation
   `"access"` (the log is binary approved/not, no read/write granularity).

9. **SoD/BoD verification is a new, additive experiment**, not present in
   the paper (Review_and_Revision_Notes.md item A3: the paper asserts but
   never proves that Phase-I's policy-to-user mapping preserves SoD/BoD).
   Methodology (`synabac/sod_bod.py`): one induced representative user per
   policy selected by Phase-I (attributes instantiate that policy's subject
   condition exactly); since no dataset ships an explicit SoD/BoD conflict
   specification, conflict pairs (SoD) and related-task pairs (BoD) are
   *randomly sampled* from the operation labels actually present in each
   dataset -- a domain-agnostic stress test, not curated business rules.
   **Finding**: SoD violations were observed in 3 of 9 case-study datasets
   (university, workforce, xustoller-university) under this random-pair
   stress test -- see results/*.csv, `sod_violations` column. Mechanism: an
   induced user built to satisfy one policy's subject condition can
   incidentally *also* satisfy a second, broader/wildcard-subject policy in
   the same set, gaining that policy's operations too -- i.e. the "one user
   per selected policy" mapping is not automatically SoD-safe in general;
   it depends on whether other selected policies' subject conditions are
   broad enough to also match that induced user. This nuances (does not
   simply confirm) the paper's Section 4.2.4 claim and is worth reporting
   in the revision rather than asserting SoD/BoD compliance unconditionally.

## What's validated

- `tests/test_paper_example.py`: Pi, Pk, Pl reproduce the paper's own
  Table 6/7 exactly. Pj does not -- Table 4 (resource attributes) and
  Table 5 (Pj's stated resource condition) do not jointly generate the
  published Pj row under *any* reading of pol_eval; documented as a
  found inconsistency in the paper's own worked example, not a code bug.
- `tests/test_phase1_toy.py`: RePo-tree pruning behavior and Phase-I's
  qualitative story (resource `re` needing Phase-II) both reproduce
  correctly; greedy MSCP matches brute-force optimal on the toy instance.
- Interactive check: `preserve_confidentiality`/`preserve_integrity`
  reproduce the paper's own Section 4.3.1/4.3.2 numeric walkthroughs
  exactly (CRS=5 at tau=5 -> allow; compensatory-read on policy `Pl`).
- Greedy Phase-I MSCP matched the exact ILP optimum on every trial run so
  far across all 9 case-study datasets (`greedy_optimality_gap` column is
  0 in every results/*.csv row) -- empirical support for citing Chvatal's
  H(n) bound as a *worst case* the experiments don't come close to hitting.
