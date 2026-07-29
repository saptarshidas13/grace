"""
Paper-ready figures from experiments/results/*.csv, addressing review gaps
C3 (no runtime/scalability plots) and C7 (table-only Section 5, no figures).

Palette/chrome follow the dataviz skill's validated default (references/palette.md):
categorical slot 1 (blue #2a78d6) and slot 2 (orange #eb6834), sequential blue
ramp, light chart chrome (#0b0b0b primary ink, #898781 muted axis, #e1e0d9
gridlines). Small multiples used wherever series count would exceed the
8-hue categorical cap (9 datasets), rather than an overplotted single panel.
"""
from __future__ import annotations

import csv
import glob
from collections import defaultdict
from pathlib import Path

import matplotlib
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import numpy as np


def _clean_log_axis(ax, axis="both"):
    """Major-ticks-only log axis: the default minor-tick labels overlap badly
    in a small-multiples grid this dense."""
    for name in (["xaxis", "yaxis"] if axis == "both" else [f"{axis}axis"]):
        a = getattr(ax, name)
        a.set_major_locator(mticker.LogLocator(base=10, numticks=4))
        a.set_minor_locator(mticker.NullLocator())
        a.set_major_formatter(mticker.LogFormatterSciNotation(base=10))

matplotlib.rcParams.update({
    "font.family": "sans-serif",
    "font.size": 9,
    "axes.edgecolor": "#c3c2b7",
    "axes.labelcolor": "#0b0b0b",
    "text.color": "#0b0b0b",
    "xtick.color": "#898781",
    "ytick.color": "#898781",
    "axes.grid": True,
    "grid.color": "#e1e0d9",
    "grid.linewidth": 0.6,
    "axes.spines.top": False,
    "axes.spines.right": False,
    "figure.facecolor": "#fcfcfb",
    "axes.facecolor": "#fcfcfb",
    "savefig.facecolor": "#fcfcfb",
})

BLUE = "#2a78d6"
ORANGE = "#eb6834"
MUTED = "#898781"

RESULTS_DIR = Path(__file__).resolve().parent / "results"
FIG_DIR = RESULTS_DIR / "figures"
FIG_DIR.mkdir(parents=True, exist_ok=True)

# Case studies first (small scale, sorted by size), Amazon last (large scale, own panel).
DATASET_ORDER = [
    "xustoller-online-video", "xustoller-healthcare", "healthcare",
    "university", "xustoller-university", "project-management",
    "xustoller-project-management", "workforce", "edocument",
    "amazon-employee-access",
]


def load(name):
    path = RESULTS_DIR / f"{name}.csv"
    if not path.exists():
        return []
    return list(csv.DictReader(open(path, encoding="utf-8")))


def fig_runtime_scaling():
    names = [n for n in DATASET_ORDER if load(n)]
    ncols = 5
    nrows = int(np.ceil(len(names) / ncols))
    fig, axes = plt.subplots(nrows, ncols, figsize=(15, 3.2 * nrows), squeeze=False)
    for i, name in enumerate(names):
        ax = axes[i // ncols][i % ncols]
        rows = load(name)
        by_shared = defaultdict(list)
        for r in rows:
            by_shared[int(r["shared_count"])].append(float(r["phase1_time_s"]) * 1000)  # ms
        xs = sorted(by_shared)
        med = [np.median(by_shared[x]) for x in xs]
        lo = [np.min(by_shared[x]) for x in xs]
        hi = [np.max(by_shared[x]) for x in xs]
        ax.fill_between(xs, lo, hi, color=BLUE, alpha=0.15, linewidth=0)
        ax.plot(xs, med, color=BLUE, linewidth=1.6, marker="o", markersize=3)
        ax.set_xscale("log")
        ax.set_yscale("log")
        _clean_log_axis(ax)
        ax.set_title(name, fontsize=8, color="#0b0b0b")
        ax.tick_params(labelsize=7)
        if i % ncols == 0:
            ax.set_ylabel("Phase-I time (ms)", fontsize=7.5)
        if i // ncols == nrows - 1:
            ax.set_xlabel("|R'| (shared resources)", fontsize=7.5)
    for j in range(len(names), nrows * ncols):
        axes[j // ncols][j % ncols].axis("off")
    fig.suptitle("Phase-I runtime vs. shared-resource-set size (median, min–max band over trials, log–log)",
                  fontsize=10, y=1.02)
    fig.tight_layout()
    fig.savefig(FIG_DIR / "runtime_scaling.pdf", bbox_inches="tight")
    fig.savefig(FIG_DIR / "runtime_scaling.png", dpi=200, bbox_inches="tight")
    plt.close(fig)


def fig_undeliverable_rate():
    names = [n for n in DATASET_ORDER if load(n)]
    ncols = 5
    nrows = int(np.ceil(len(names) / ncols))
    fig, axes = plt.subplots(nrows, ncols, figsize=(15, 3.2 * nrows), squeeze=False)
    for i, name in enumerate(names):
        ax = axes[i // ncols][i % ncols]
        rows = load(name)
        by_frac = defaultdict(list)
        for r in rows:
            pct = 100 * float(r["phase1_undeliverable_count"]) / max(1, float(r["shared_count"]))
            by_frac[float(r["shared_fraction"])].append(pct)
        xs = sorted(by_frac)
        med = [np.median(by_frac[x]) for x in xs]
        lo = [np.min(by_frac[x]) for x in xs]
        hi = [np.max(by_frac[x]) for x in xs]
        ax.fill_between(xs, lo, hi, color=ORANGE, alpha=0.15, linewidth=0)
        ax.plot(xs, med, color=ORANGE, linewidth=1.6, marker="o", markersize=3)
        ax.set_ylim(-5, 105)
        ax.set_title(name, fontsize=8, color="#0b0b0b")
        ax.tick_params(labelsize=7)
        if i % ncols == 0:
            ax.set_ylabel("Undeliverable in Phase-I (%)", fontsize=7.5)
        if i // ncols == nrows - 1:
            ax.set_xlabel("shared fraction |R'|/|R|", fontsize=7.5)
    for j in range(len(names), nrows * ncols):
        axes[j // ncols][j % ncols].axis("off")
    fig.suptitle("Share of R' left undeliverable by Phase-I alone (median, min–max band over trials)",
                  fontsize=10, y=1.02)
    fig.tight_layout()
    fig.savefig(FIG_DIR / "undeliverable_rate.pdf", bbox_inches="tight")
    fig.savefig(FIG_DIR / "undeliverable_rate.png", dpi=200, bbox_inches="tight")
    plt.close(fig)


def fig_policy_reduction():
    # Use the LARGEST swept fraction, not the median: the undeliverable-rate
    # figure shows Phase-I delivers ~0 resources at low/mid fractions for
    # several datasets (policy footprints are broad relative to a small
    # random R'), which would make a median-fraction comparison here
    # misleadingly show an empty Phase-I bar for the wrong reason.
    names = [n for n in DATASET_ORDER if load(n)]
    naive_vals, phase1_vals, leak_vals, labels = [], [], [], []
    for name in names:
        rows = load(name)
        fracs = sorted(set(float(r["shared_fraction"]) for r in rows))
        top = fracs[-1]
        top_rows = [r for r in rows if float(r["shared_fraction"]) == top]
        naive_vals.append(np.mean([float(r["naive_policy_count"]) for r in top_rows]))
        phase1_vals.append(np.mean([float(r["phase1_policy_count"]) for r in top_rows]))
        leak_vals.append(np.mean([float(r["naive_leaked_resource_count"]) for r in top_rows]))
        labels.append(f"{name} (frac={top:g})")

    y = np.arange(len(labels))
    h = 0.35
    fig, ax = plt.subplots(figsize=(9, 0.5 * len(labels) + 1))
    ax.barh(y + h / 2, [max(v, 0.08) for v in naive_vals], height=h, color=BLUE,
            label="Naive baseline (no minimization)")
    ax.barh(y - h / 2, [max(v, 0.08) for v in phase1_vals], height=h, color=ORANGE,
            label="GRACE Phase-I (minimized)")
    def _plural(n):
        return "policy" if round(n) == 1 else "policies"

    for yi, (nv, pv, lk) in enumerate(zip(naive_vals, phase1_vals, leak_vals)):
        ax.text(max(nv, 0.08) * 1.15, yi + h / 2, f"{nv:.0f} {_plural(nv)}, leaks {lk:.0f} res.",
                va="center", fontsize=7, color="#52514e")
        ax.text(max(pv, 0.08) * 1.15, yi - h / 2, f"{pv:.0f} {_plural(pv)}, 0 leaked",
                va="center", fontsize=7, color="#52514e")
    ax.set_yticks(y)
    ax.set_yticklabels(labels, fontsize=8)
    ax.set_xlabel("Policies used to cover the deliverable shared resources (log scale; labels give exact counts)")
    ax.set_xscale("log")
    ax.set_xlim(right=ax.get_xlim()[1] * 4)
    _clean_log_axis(ax, axis="x")
    ax.invert_yaxis()
    ax.legend(frameon=False, loc="upper right", fontsize=8)
    ax.set_title("Policy-set size at the largest swept fraction: naive baseline vs. GRACE Phase-I\n"
                  "(naive also over-permissions onto unauthorized resources; Phase-I never does)",
                  fontsize=10)
    fig.tight_layout()
    fig.savefig(FIG_DIR / "policy_reduction.pdf", bbox_inches="tight")
    fig.savefig(FIG_DIR / "policy_reduction.png", dpi=200, bbox_inches="tight")
    plt.close(fig)


if __name__ == "__main__":
    fig_runtime_scaling()
    fig_undeliverable_rate()
    fig_policy_reduction()
    print("Wrote figures to", FIG_DIR)
