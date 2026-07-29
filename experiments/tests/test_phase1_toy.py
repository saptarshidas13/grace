"""Integration test for Phase-I mechanics (pruning + greedy MSCP) using the
paper's toy dataset, with R' = {ra,rb,rc,rd,re} (rf excluded, matching the
paper's own choice of shared set for the Figure 1 walkthrough). Uses the
*computed* (correct) Pj coverage rather than the paper's inconsistent
Table 6/7 entry for Pj -- see test_paper_example.py for that divergence.
"""
import itertools
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from test_paper_example import ALL_RESOURCES, Pi, Pj, Pk, Pl, RESOURCES
from grace.phase1 import construct_repo_tree, syn_abac_phase1, find_resources

POLICIES = [Pi, Pj, Pk, Pl]
SHARED = frozenset({"ra", "rb", "rc", "rd", "re"})


def test_repo_tree_for_ra_no_pruning_needed_after_fix():
    leaves = construct_repo_tree(RESOURCES["ra"], ALL_RESOURCES, POLICIES, SHARED)
    covered_sets = {leaf.resource_set for leaf in leaves}
    # With the corrected Pj (={ra,rb,rd}), no policy governing ra reaches
    # outside the shared set, so nothing gets pruned -- unlike the paper's
    # narrative (which prunes Pj because of the erroneous rf inclusion).
    assert frozenset({"ra", "rb", "rc"}) in covered_sets  # via Pi
    assert frozenset({"ra", "rb", "rd"}) in covered_sets  # via Pj and via Pk (duplicate set, two policies)
    assert len(leaves) == 3  # Pi, Pj, Pk all applicable and all survive pruning


def test_repo_tree_prunes_rf_reachable_policies():
    # rf is confidential+archived; only Pl reaches it, and only Pl governs re/rf.
    leaves = construct_repo_tree(RESOURCES["re"], ALL_RESOURCES, POLICIES, SHARED)
    for leaf in leaves:
        assert "rf" not in leaf.resource_set, "rf is outside the shared set R' and must never appear in a surviving leaf"


def _brute_force_min_cover(universe, candidate_sets):
    """Exact minimum set cover via exhaustive search -- fine for this tiny toy instance only."""
    for k in range(1, len(candidate_sets) + 1):
        for combo in itertools.combinations(range(len(candidate_sets)), k):
            union = frozenset().union(*(candidate_sets[i] for i in combo))
            if union >= universe:
                return k
    return len(candidate_sets)


def test_phase1_end_to_end_is_optimal_on_toy_instance():
    result = syn_abac_phase1(SHARED, ALL_RESOURCES, POLICIES)
    # re is only governed by Pl, whose full coverage is {re, rf}; rf is outside
    # the shared set, so Pl's branch is pruned and re has no safe policy left.
    # This *is* the paper's own Phase-II motivating scenario (Section 4.3,
    # "re remains inaccessible in Phase-I") -- reproduced correctly here even
    # though the exact Pj numbers differ from the paper's Table 6/7 typo.
    assert result.undeliverable == frozenset({"re"})
    covered = frozenset().union(*(leaf.resource_set for leaf in result.covering_sets))
    assert covered == SHARED - {"re"}, "selected policies must jointly cover every deliverable shared resource"

    # Optimality-gap check against brute force over the *actual* policy-covered sets.
    deliverable = SHARED - result.undeliverable
    all_sets = [find_resources(p, ALL_RESOURCES) & deliverable for p in POLICIES]
    optimal_k = _brute_force_min_cover(deliverable, all_sets)
    assert len(result.covering_sets) <= optimal_k + 0  # greedy must equal optimal on this small instance


if __name__ == "__main__":
    test_repo_tree_for_ra_no_pruning_needed_after_fix()
    test_repo_tree_prunes_rf_reachable_policies()
    test_phase1_end_to_end_is_optimal_on_toy_instance()
    print("Phase-I toy integration tests passed.")
