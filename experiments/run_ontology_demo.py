"""
Demonstrates the one capability gap identified between Syn-ABAC and its
own stated motivation: the paper's abstract and introduction name
"attribute mismatches" across organizations as the central problem being
solved, but Algorithm 6 (pol_eval), which every Phase-I/Phase-II
mechanism is built on, requires literal string equality (or a wildcard)
-- it has no concept of attribute-value proximity across organizations at
all. Gupta & Sural [35] is the one related-work paper that actually
implements a solution to that specific problem (ontology-based hierarchy
+ distance-bounded relaxation, Sections 4.2-4.3).

Uses the SAME rule (r3) and hierarchy (Figures 6-7) from [35]'s own
worked example, already validated exactly in tests/test_ontology.py, so
no new dataset is needed for this to be a faithful reproduction rather
than a constructed strawman.

Scenario: r3 requires (Designation=AssistantDean, Department=
SchoolOfEngineering). A guest-organization user U1 has the semantically
close but differently-named (Designation=HOD, Department=
SchoolOfBasicSciences) -- exactly [35]'s own U1.
"""
from __future__ import annotations

from grace.baseline_ontology import Ontology, hierarchy_ok, relaxed_ok
from grace.model import Entity, make_flat_rule, resource_matches

# --- Build the two ontologies (same as tests/test_ontology.py) ----------
dept = Ontology()
for v, p in [("AcademicUnits", None), ("College", "AcademicUnits"), ("Faculty", "AcademicUnits"),
             ("School", "AcademicUnits"), ("CollegeOfEngineering", "College"), ("SchoolOfEngineering", "School"),
             ("SchoolOfBasicSciences", "School")]:
    dept.add(v, p)
dept.add_equivalence("College", "School")
dept.add_equivalence("Faculty", "School")

desig = Ontology()
for v, p in [("Designation", None), ("Employee", "Designation"), ("CollegeEmployee", "Employee"),
             ("AdministrativeStaff", "CollegeEmployee"), ("HOD", "AdministrativeStaff"),
             ("AssistantDean", "AdministrativeStaff")]:
    desig.add(v, p)

REQUIRED_DESIGNATION = "AssistantDean"
REQUIRED_DEPARTMENT = "SchoolOfEngineering"
USER_DESIGNATION = "HOD"
USER_DEPARTMENT = "SchoolOfBasicSciences"

# --- Syn-ABAC's own evaluator (Algorithm 6 / model.py resource_matches / ---
# --- LiteralConjunct 'in'), applied to the *subject* condition here, to ---
# --- show it has no relaxation path regardless of framing. ---------------
r3_as_flat_rule = make_flat_rule(
    "r3", {"Designation": REQUIRED_DESIGNATION, "Department": REQUIRED_DEPARTMENT}, {}, ["append"],
)
guest_user = Entity("U1_guest", {"Designation": USER_DESIGNATION, "Department": USER_DEPARTMENT})


def synabac_style_match(rule, user) -> bool:
    """Same literal-equality semantics as resource_matches/pol_eval, applied
    to the subject side -- Syn-ABAC's model has no distance/hierarchy
    concept for either side of a rule."""
    return all(
        user.attrs.get(c.attr) in (c.rhs if isinstance(c.rhs, frozenset) else {c.rhs})
        for c in rule.sub_cond
    )


def main() -> None:
    print("Rule r3 requires Designation=AssistantDean, Department=SchoolOfEngineering")
    print(f"Guest user U1 has Designation={USER_DESIGNATION}, Department={USER_DEPARTMENT}\n")

    syn_result = synabac_style_match(r3_as_flat_rule, guest_user)
    print(f"Syn-ABAC-style literal pol_eval (Algorithm 6): satisfied = {syn_result}")

    hier_result = (hierarchy_ok(desig, REQUIRED_DESIGNATION, USER_DESIGNATION)
                   and hierarchy_ok(dept, REQUIRED_DEPARTMENT, USER_DEPARTMENT))
    print(f"[35] hierarchy-only (Section 4.2, no relaxation): satisfied = {hier_result}")

    print("[35] distance-bounded relaxation (Section 4.3), sweeping D:")
    for d in range(0, 4):
        ok = (relaxed_ok(desig, REQUIRED_DESIGNATION, USER_DESIGNATION, d)
              and relaxed_ok(dept, REQUIRED_DEPARTMENT, USER_DEPARTMENT, d))
        print(f"  D={d}: satisfied = {ok}")

    print("\nConclusion: Syn-ABAC's pol_eval never grants this access at any relaxation")
    print("distance because it has no relaxation mechanism at all -- [35]'s approach")
    print("grants it starting at D=2, the minimum distance needed on both attributes")
    print("simultaneously (matches the paper's own worked numbers, see test_ontology.py).")


if __name__ == "__main__":
    main()
