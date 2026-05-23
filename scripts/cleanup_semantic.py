#!/usr/bin/env python3
"""Remove non-error semantic cases that don't actually fail in GOR, and remove
duplicates. Also strip unused `inputs:` from cases that don't reference them.
"""
from __future__ import annotations
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASES = ROOT / "gortools/src/test/resources/compat/cases/compatibility-cases.yml"

DELETE = {
    "semantic.calc.duplicate_column",
    "semantic.where.numeric_compare_against_string_literal",
    "semantic.gorrow.invalid_chrom_format",
    "semantic.calc.divide_by_zero_int",
    "semantic.sort.unknown_genome",
    "semantic.where.eq_string_to_num",
    "semantic.where.unknown_column_in_compare",
    "semantic.select.empty",
}

# For these, delete the unused `inputs:` block.
STRIP_INPUTS = {
    "semantic.gor.missing_file",
    "semantic.join.unknown_source_file",
}

ID_LINE = re.compile(r"^- id:\s*(\S+)\s*$")


def main() -> int:
    text = CASES.read_text()
    lines = text.split("\n")
    out: list[str] = []
    deleted = 0
    stripped = 0
    i = 0
    while i < len(lines):
        line = lines[i]
        m = ID_LINE.match(line)
        if not m:
            out.append(line)
            i += 1
            continue
        case_id = m.group(1)
        j = i + 1
        while j < len(lines) and not lines[j].startswith("- id:"):
            j += 1
        block = lines[i:j]
        if case_id in DELETE:
            deleted += 1
            i = j
            continue
        if case_id in STRIP_INPUTS:
            new_block = []
            k = 0
            while k < len(block):
                ln = block[k]
                if ln.strip().startswith("inputs:"):
                    # skip until next 2-space-indent key or end of block
                    k += 1
                    while k < len(block):
                        ln2 = block[k]
                        if ln2.startswith("  ") and not ln2.startswith("    ") and not ln2.startswith("   ") and ":" in ln2 and not ln2.lstrip().startswith("-"):
                            # Top-level key of the case block (2 spaces indent)
                            break
                        k += 1
                    stripped += 1
                    continue
                new_block.append(ln)
                k += 1
            block = new_block
        out.extend(block)
        i = j
    CASES.write_text("\n".join(out))
    print(f"Deleted {deleted} semantic case(s); stripped inputs from {stripped} case(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
