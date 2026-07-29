"""Validate baseline_ontology.py against Gupta & Sural's own worked example
(IWSPA 2023, Section 4.4, Figures 6-7): rules r1-r5, user U1
(Designation=HOD, Department=SchoolOfBasicSciences), reproducing their
stated satisfied/unsatisfied outcomes and their stated distances exactly.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from grace.baseline_ontology import Ontology, hierarchy_ok, relaxed_ok

# ---- Figure 6: academic unit hierarchy (tree) --------------------------
dept = Ontology()
dept.add("AcademicUnits", None)
dept.add("College", "AcademicUnits")
dept.add("Faculty", "AcademicUnits")
dept.add("School", "AcademicUnits")
dept.add("CollegeOfEngineering", "College")
dept.add("CollegeOfHumanities", "College")
dept.add("CollegeOfLiberalArts", "College")
dept.add("SchoolOfEngineering", "School")
dept.add("SchoolOfBasicSciences", "School")
dept.add("SchoolOfBusiness", "School")
# College, Faculty, School are mutually equivalent (bidirectional edges, Section 4.4)
dept.add_equivalence("College", "School")
dept.add_equivalence("Faculty", "School")

# ---- Figure 7: designation hierarchy (tree) -----------------------------
desig = Ontology()
desig.add("Designation", None)
desig.add("Employee", "Designation")
desig.add("CollegeEmployee", "Employee")
desig.add("SchoolEmployee", "Employee")
desig.add("AdministrativeStaff", "CollegeEmployee")
desig.add("AcademicStaff", "CollegeEmployee")
desig.add("HOD", "AdministrativeStaff")
desig.add("AssistantDean", "AdministrativeStaff")


def test_r2_satisfied_via_hierarchy():
    # r2: Department=School; U1: Department=SchoolOfBasicSciences -> School is an ancestor
    assert hierarchy_ok(dept, "School", "SchoolOfBasicSciences") is True


def test_r1_not_satisfied():
    # r1: Department=ME; U1: SchoolOfBasicSciences -- unrelated branch, not even with hierarchy
    assert hierarchy_ok(dept, "ME", "SchoolOfBasicSciences") is False
    assert relaxed_ok(dept, "ME", "SchoolOfBasicSciences", max_distance=1) is False


def test_r3_distance_is_2_both_attributes():
    # r3 designation: AssistantDean vs HOD -> distance 2 (paper's own stated value)
    assert desig.distance("AssistantDean", "HOD") == 2
    # r3 department: SchoolOfEngineering vs SchoolOfBasicSciences -> distance 2
    assert dept.distance("SchoolOfEngineering", "SchoolOfBasicSciences") == 2
    assert relaxed_ok(desig, "AssistantDean", "HOD", max_distance=2) is True
    assert relaxed_ok(dept, "SchoolOfEngineering", "SchoolOfBasicSciences", max_distance=2) is True
    assert relaxed_ok(desig, "AssistantDean", "HOD", max_distance=1) is False


def test_r5_department_distance_collapses_to_2_via_equivalence():
    # r5: Department=CollegeOfEngineering vs U1's SchoolOfBasicSciences.
    # Paper: "the required distance would have been 3" without the College=School
    # equivalence; with it, the same as r3's case (distance 2).
    assert dept.distance("CollegeOfEngineering", "SchoolOfBasicSciences") == 2
    assert desig.distance("HOD", "HOD") == 0  # r5 designation matches directly


if __name__ == "__main__":
    test_r2_satisfied_via_hierarchy()
    test_r1_not_satisfied()
    test_r3_distance_is_2_both_attributes()
    test_r5_department_distance_collapses_to_2_via_equivalence()
    print("Ontology baseline reproduces the paper's own worked example exactly.")
