"""Sanity check for the PolAP baseline (paper [33]) on a small constructed
instance -- not the paper's own Cathy/XYZ example, because that example's
Table VI/VII are mutually inconsistent (r1's object condition is fully
wildcarded ({type=-, subject=-}), so it must match every object, but
Table VII lists it against only 2 of 3 objects) -- the same category of
worked-example slip found and documented for the Syn-ABAC paper itself in
test_paper_example.py, not a defect in this implementation.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from grace.model import Entity, make_flat_rule
from grace.baseline_polap import polap

RESOURCES = [
    Entity("o1", {"type": "public"}),
    Entity("o2", {"type": "public"}),
    Entity("o3", {"type": "secret"}),
]

# P_broad covers o1,o2,o3 (leaks o3 if subject only wants o1,o2)
P_broad = make_flat_rule("P_broad", {}, {}, ["read"])
# P_narrow covers only o1,o2 (type=public)
P_narrow = make_flat_rule("P_narrow", {}, {"type": "public"}, ["read"])
# P_o1 covers only o1
P_o1 = make_flat_rule("P_o1", {}, {"type": "public"}, ["read"])  # deliberately same cond as narrow, diff id

POLICIES = [P_broad, P_narrow, P_o1]


def test_prunes_rule_that_leaks_outside_desired_set():
    result = polap([("o1", "read"), ("o2", "read")], RESOURCES, POLICIES)
    assert not result.undeliverable
    assert "P_broad" not in result.minimal_rules, "P_broad leaks o3 and must be pruned"
    assert set(result.minimal_rules) <= {"P_narrow", "P_o1"}


def test_undeliverable_when_only_leaking_rule_exists():
    # o3 (secret) is only reachable via P_broad, which also reaches o1/o2 --
    # but if the desired set is just {o3}, P_broad's coverage {o1,o2,o3} is
    # NOT a subset of {o3}, so it must be pruned, leaving o3 undeliverable.
    result = polap([("o3", "read")], RESOURCES, POLICIES)
    assert result.undeliverable
    assert result.undeliverable_pair == ("o3", "read")


def test_minimal_hitting_set_picks_fewest_rules():
    result = polap([("o1", "read"), ("o2", "read")], RESOURCES, POLICIES)
    assert len(result.minimal_rules) == 1  # either P_narrow or P_o1 alone covers both


if __name__ == "__main__":
    test_prunes_rule_that_leaks_outside_desired_set()
    test_undeliverable_when_only_leaking_rule_exists()
    test_minimal_hitting_set_picks_fewest_rules()
    print("PolAP sanity checks passed.")
