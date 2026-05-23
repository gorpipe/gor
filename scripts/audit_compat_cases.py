#!/usr/bin/env python3
"""Apply lint-driven cleanups to compatibility-cases.yml.

- Remove duplicates listed in DUPLICATE_GROUPS (keeping the first id of each group).
- Add `behavior: pipeline` to cases listed in PIPELINE_BEHAVIOR so the >3-pipe lint
  recognizes them as intentional multi-step examples.

Run from repo root.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASES = ROOT / "gortools/src/test/resources/compat/cases/compatibility-cases.yml"

# Each tuple: keep-first, delete rest.
DUPLICATE_GROUPS = [
    ("cmd.calc.constant_expr", "cmd.calc.parentheses_precedence"),
    ("bulk.pipe.082", "bulk.cmd.170"),
    ("bulk.cmd.162", "bulk.cmd.165"),
    ("bulk.cmd.164", "bulk.cmd.169"),
    ("bulk.cmd.174", "bulk.cmd.178"),
    ("bulk.cmd.175", "bulk.cmd.179"),
    ("bulk.cmd.188", "bulk.cmd.199"),
    ("bulk.cmd.190", "bulk.cmd.200"),
    ("bulk.new.014", "bulk.new.142"),
    ("bulk.new.015", "bulk.new.143"),
    ("bulk.new.016", "bulk.new.144"),
    ("bulk.new.017", "bulk.new.145"),
    ("bulk.new.018", "bulk.new.152"),
    ("bulk.new.019", "bulk.new.153"),
    ("bulk.new.020", "bulk.new.154"),
    ("bulk.new.021", "bulk.new.155"),
    ("bulk.new.022", "bulk.new.148"),
    ("bulk.new.023", "bulk.new.149"),
    ("bulk.new.024", "bulk.new.156"),
    ("bulk.new.025", "bulk.new.157"),
    ("bulk.new.026", "bulk.new.158"),
    ("bulk.new.027", "bulk.new.159"),
    ("bulk.new.028", "bulk.new.160"),
    ("bulk.new.029", "bulk.new.146"),
    ("bulk.new.030", "bulk.new.147"),
    ("bulk.new.031", "bulk.new.161"),
    ("bulk.new.032", "bulk.new.162"),
    ("bulk.new.033", "bulk.new.163"),
    ("bulk.new.034", "bulk.new.150"),
    ("bulk.new.035", "bulk.new.151"),
    ("bulk.new.036", "bulk.new.164"),
    ("bulk.new.039", "bulk.new.093", "bulk.new.133", "bulk.new.188"),
    ("bulk.new.040", "bulk.new.140"),
    ("bulk.new.041", "bulk.new.141"),
    ("bulk.new.042", "bulk.new.092", "bulk.new.134", "bulk.new.189"),
    ("bulk.new.043", "bulk.new.135", "bulk.new.190"),
    ("bulk.new.046", "bulk.new.120"),
    ("bulk.new.050", "bulk.new.123"),
    ("bulk.new.052", "bulk.new.191"),
    ("bulk.new.053", "bulk.new.112"),
    ("bulk.new.054", "bulk.new.113"),
    ("bulk.new.056", "bulk.new.115"),
    ("bulk.new.057", "bulk.new.116"),
    ("bulk.new.058", "bulk.new.117"),
    ("bulk.new.059", "bulk.new.118"),
    ("bulk.new.060", "bulk.new.119"),
    ("bulk.new.063", "bulk.new.165"),
    ("bulk.new.064", "bulk.new.166"),
    ("bulk.new.065", "bulk.new.167"),
    ("bulk.new.066", "bulk.new.168"),
    ("bulk.new.067", "bulk.new.169"),
    ("bulk.new.068", "bulk.new.170"),
    ("bulk.new.069", "bulk.new.171"),
    ("bulk.new.070", "bulk.new.172"),
    ("bulk.new.071", "bulk.new.173"),
    ("bulk.new.072", "bulk.new.174"),
    ("bulk.new.073", "bulk.new.175"),
    ("bulk.new.074", "bulk.new.176"),
    ("bulk.new.077", "bulk.new.139"),
    ("bulk.new.078", "bulk.new.138"),
    ("bulk.new.081", "bulk.new.192"),
    ("bulk.new.082", "bulk.new.193"),
    ("bulk.new.084", "bulk.new.195"),
    ("bulk.new.085", "bulk.new.196"),
    ("bulk.new.086", "bulk.new.197"),
    ("bulk.new.087", "bulk.new.198"),
    ("bulk.new.088", "bulk.new.199"),
    ("bulk.new.091", "bulk.new.107"),
    ("bulk.new.094", "bulk.new.131", "bulk.new.186"),
    ("bulk.new.095", "bulk.new.132", "bulk.new.187"),
    ("bulk.new.096", "bulk.new.177"),
    ("bulk.new.097", "bulk.new.178"),
    ("bulk.new.098", "bulk.new.179"),
    ("bulk.new.099", "bulk.new.180"),
    ("bulk.new.100", "bulk.new.181"),
]

PIPELINE_BEHAVIOR = {
    "cmd.colnum.multi",
    "cmd.distinct.nor_sorted",
    "cmd.rename.regex_groups",
    "cmd.select.append_option",
    "cmd.hide.sort_option",
    "cmd.hide.select_remaining",
    "parser.pipe.chain.five_cmds",
    "cmd.hide.all_calc_keep_core",
    "cmd.cols2list.nor_gc_top0",
    "cmd.setcoltype.multiple_columns",
    "cmd.coltype.multiple_values",
    "cmd.selwhere.by_colname_prefix",
    "cmd.where.parenthesized_precedence",
}

ID_LINE = re.compile(r"^- id:\s*(\S+)\s*$")


def main() -> int:
    deletions: set[str] = set()
    for grp in DUPLICATE_GROUPS:
        for d in grp[1:]:
            deletions.add(d)

    text = CASES.read_text()
    lines = text.split("\n")
    out: list[str] = []
    deleted = 0
    pipeline_added = 0
    i = 0
    while i < len(lines):
        line = lines[i]
        m = ID_LINE.match(line)
        if not m:
            out.append(line)
            i += 1
            continue
        case_id = m.group(1)
        # Find end of this case block (next `- id:` or EOF)
        j = i + 1
        while j < len(lines) and not lines[j].startswith("- id:"):
            j += 1
        block = lines[i:j]
        if case_id in deletions:
            deleted += 1
            i = j
            continue
        if case_id in PIPELINE_BEHAVIOR:
            # Insert `behavior: pipeline` after the id line if not already present
            has_behavior = any(b.strip().startswith("behavior:") for b in block[1:])
            if not has_behavior:
                block = [block[0], "  behavior: pipeline"] + block[1:]
                pipeline_added += 1
        out.extend(block)
        i = j

    new_text = "\n".join(out)
    CASES.write_text(new_text)
    print(f"Deleted {deleted} duplicate case(s); added behavior=pipeline to {pipeline_added} case(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
