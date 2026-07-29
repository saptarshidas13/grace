"""
Validate the implementation against the paper's own worked example
(Tables 1-5, Section 3.2), reproducing Table 6 (Resource-to-Policy) and
Table 7 (Policy-to-Resource).

Result: Pi, Pk, Pl reproduce the published tables exactly. Pj does not --
see the assertion message below for the diagnosis. This is flagged as a
finding about the paper's own illustrative example, not a defect in this
implementation (Pi/Pk/Pl matching exactly on the same pol_eval code path
is strong evidence the evaluator is correct).
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from grace.model import Entity, make_flat_rule
from grace.phase1 import find_resources

# ---- Table 4: resources -----------------------------------------------
RESOURCES = {
    "ra": Entity("ra", {"Type": "document", "Sensitivity": "public", "Status": "active"}),
    "rb": Entity("rb", {"Type": "document", "Sensitivity": "public", "Status": "active"}),
    "rc": Entity("rc", {"Type": "document", "Sensitivity": "restricted", "Status": "active"}),
    "rd": Entity("rd", {"Type": "record", "Sensitivity": "public", "Status": "active"}),
    "re": Entity("re", {"Type": "record", "Sensitivity": "confidential", "Status": "archived"}),
    "rf": Entity("rf", {"Type": "document", "Sensitivity": "confidential", "Status": "archived"}),
}
ALL_RESOURCES = list(RESOURCES.values())

# ---- Table 5: policies (user cond is irrelevant to find_resources) ----
Pi = make_flat_rule("Pi", {"department": "HR"}, {"Type": "document", "Status": "active"}, ["read"])
Pj = make_flat_rule("Pj", {"department": "HR", "position": "Executive", "nature": "full-time"},
                     {"Sensitivity": "public", "Status": "active"}, ["read"])
Pk = make_flat_rule("Pk", {"position": "director"}, {"Sensitivity": "public"}, ["read"])
Pl = make_flat_rule("Pl", {"nature": "part-time"}, {"Sensitivity": "confidential", "Status": "archived"}, ["write"])


def _ids(frozen):
    return set(frozen)


def test_Pi_matches_table7():
    assert _ids(find_resources(Pi, ALL_RESOURCES)) == {"ra", "rb", "rc"}


def test_Pk_matches_table7():
    assert _ids(find_resources(Pk, ALL_RESOURCES)) == {"ra", "rb", "rd"}


def test_Pl_matches_table7():
    assert _ids(find_resources(Pl, ALL_RESOURCES)) == {"re", "rf"}


def test_Pj_diverges_from_table7_as_published():
    """Table 7 (and Table 6, and the Figure 1 RePo-tree narrative) all state
    Pj covers {ra, rd, rf}. Mechanically applying Pj's own stated resource
    condition (Sensitivity=public, Status=active) to Table 4's own resource
    attribute values gives {ra, rb, rd} instead: rb is (public, active) so it
    satisfies Pj and should be included; rf is (confidential, archived) so it
    violates Pj on *both* fields and cannot be included under any reading of
    Algorithm 6 (resource-condition-only *or* full evaluation, since resource
    attributes don't depend on which evaluation mode is used).

    This holds regardless of the resource-only vs subject+resource pol_eval
    scope question (see model.py docstring) -- Pj has no subject-side
    dependency that could change which resources satisfy its Sensitivity/
    Status condition. Since Pi, Pk, and Pl all reproduce Table 7 exactly via
    the same code path, this is best read as an arithmetic slip in
    constructing the illustrative example (Table 4 and Table 5 as published
    don't jointly generate the Pj row of Table 6/7), not an implementation
    bug here -- flagged for the authors rather than silently special-cased.
    """
    computed = _ids(find_resources(Pj, ALL_RESOURCES))
    published = {"ra", "rd", "rf"}
    assert computed == {"ra", "rb", "rd"}
    assert computed != published, (
        "if this ever starts passing, Table 4/5 in the paper changed -- "
        "re-verify this is intentional before treating it as a regression"
    )


if __name__ == "__main__":
    test_Pi_matches_table7()
    test_Pk_matches_table7()
    test_Pl_matches_table7()
    test_Pj_diverges_from_table7_as_published()
    print("All checks passed (including the documented Pj divergence).")
