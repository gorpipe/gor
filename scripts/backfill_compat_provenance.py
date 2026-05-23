#!/usr/bin/env python3
"""Backfill `docs:` and `junit:` provenance into compatibility-cases.yml.

Inserts docs+junit blocks immediately after each `- id:` line, mapped from
the case id prefix. Skips cases that already have provenance.

Run with no args from repo root. Edits the file in place.
"""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASES = ROOT / "gortools/src/test/resources/compat/cases/compatibility-cases.yml"
DOC_CMD = "documentation/src/command"
DOC_FN = "documentation/src/functions"
DOC_LANG = "documentation/src/languageBasics.rst"
DOC_CMDS_INDEX = "documentation/src/commands.rst"
DOC_FNS_INDEX = "documentation/src/all_functions.rst"
JUNIT_DIR = "gortools/src/test/java/gorsat"

# Map cmd.<name> -> (docs, junit).
# Doc files we know exist on disk are referenced; otherwise fall back to commands.rst.
CMD_DOC_OVERRIDES = {
    "atmax": "ATMAX.rst",
    "atmin": "ATMIN.rst",
    "bucketsplit": "BUCKETSPLIT.rst",
    "calc": "CALC.rst",
    "calcifmissing": "CALCIFMISSING.rst",
    "collect": "COLLECT.rst",
    "colnum": "COLNUM.rst",
    "cols2list": "COLS2LIST.rst",
    "colsplit": "COLSPLIT.rst",
    "coltype": "COLTYPE.rst",
    "columnsort": "COLUMNSORT.rst",
    "distinct": "DISTINCT.rst",
    "filterinvalidrows": "FILTERINVALIDROWS.rst",
    "first": "FIRST.rst",
    "grep": "GREP.rst",
    "group": "GROUP.rst",
    "hide": "HIDE.rst",
    "inset": "INSET.rst",
    "join": "JOIN.rst",
    "leftwhere": "LEFTWHERE.rst",
    "map": "MAP.rst",
    "merge": "MERGE.rst",
    "multimap": "MULTIMAP.rst",
    "pivot": "PIVOT.rst",
    "prefix": "PREFIX.rst",
    "rank": "RANK.rst",
    "regex": "REGEX.rst",
    "rename": "RENAME.rst",
    "replace": "REPLACE.rst",
    "rownum": "ROWNUM.rst",
    "sed": "SED.rst",
    "segspan": "SEGSPAN.rst",
    "select": "SELECT.rst",
    "selwhere": "SELWHERE.rst",
    "setcoltype": "SETCOLTYPE.rst",
    "skip": "SKIP.rst",
    "sort": "SORT.rst",
    "split": "SPLIT.rst",
    "throwif": "THROWIF.rst",
    "top": "TOP.rst",
    "tryhide": "TRYHIDE.rst",
    "tryselect": "TRYSELECT.rst",
    "trywhere": "TRYWHERE.rst",
    "until": "UNTIL.rst",
    "upto": "UPTO.rst",
    "varnorm": "VARNORM.rst",
    "verifycoltype": "VERIFYCOLTYPE.rst",
    "verifyorder": "VERIFYORDER.rst",
    "wait": "WAIT.rst",
    "where": "WHERE.rst",
    "adjust": "ADJUST.rst",
    "countrows": "COUNTROWS.rst",
    "inline": "INLINE.rst",
    "inline_fallback": "INLINE.rst",
    "nested": "languageBasics.rst",
    "error": "languageBasics.rst",
}

# Junit overrides per cmd name (file under gortools/src/test/java/gorsat/...).
CMD_JUNIT_OVERRIDES = {
    "atmax": "UTestAtMinMax.java",
    "atmin": "UTestAtMinMax.java",
    "calc": "Commands/UTestCalc.java",
    "calcifmissing": "parser/UTestCalcIfMissing.java",
    "colnum": "UTestColNum.java",
    "cols2list": "UTestCols2List.java",
    "coltype": "UTestColType.java",
    "distinct": "UTestDistinct.java",
    "first": "UTestFirst.java",
    "group": "UTestGroup.java",
    "hide": "UTestHide.java",
    "join": "UTestJoin.java",
    "merge": "UTestMerge.java",
    "rank": "UTestRank.java",
    "select": "UTestSelect.java",
    "sort": "UTestSort.java",
    "top": "UTestTop.java",
    "where": "UTestWhere.java",
    "prefix": "Commands/UTestPrefix.java",
    "rename": "UTestRename.java",
    "regex": "UTestRegex.java",
    "split": "UTestSplit.java",
    "varnorm": "UTestVarNorm.java",
    "verifyorder": "UTestVerifyOrder.java",
    "throwif": "UTestThrowIf.java",
    "rownum": "UTestRowNum.java",
}

# Map fn.<group> -> docs file (relative to repo root).
FN_GROUP_DOC = {
    "math": "documentation/src/functions/abs.rst",
    "alg": "documentation/src/functions/abs.rst",
    "trig": "documentation/src/functions/sine.rst",
    "string": "documentation/src/functions/upper.rst",
    "list": "documentation/src/functions/listadd.rst",
    "date": "documentation/src/functions/date.rst",
    "bool": "documentation/src/functions/if.rst",
    "conv": "documentation/src/functions/int.rst",
    "convert": "documentation/src/functions/int.rst",
    "genomic": "documentation/src/functions/segoverlap.rst",
    "pr": "documentation/src/functions/pval.rst",
}

FN_GROUP_JUNIT = {
    "math": "parser/UTestAlgebraicFunctions.java",
    "alg": "parser/UTestAlgebraicFunctions.java",
    "trig": "parser/UTestAlgebraicFunctions.java",
    "string": "parser/UTestStringFunctions.java",
    "list": "parser/UTestListFunctions.java",
    "date": "parser/UTestDateFunctions.java",
    "bool": "parser/UTestBooleanFunctions.java",
    "conv": "parser/UTestCalcCompiler.java",
    "convert": "parser/UTestCalcCompiler.java",
    "genomic": "parser/UTestGenomicFunctions.java",
    "pr": "parser/UTestPrFunctions.java",
}


def cmd_docs_for(name: str) -> list[str]:
    if name in CMD_DOC_OVERRIDES:
        return [f"{DOC_CMD}/{CMD_DOC_OVERRIDES[name]}"]
    return [DOC_CMDS_INDEX]


def cmd_junit_for(name: str) -> list[str]:
    if name in CMD_JUNIT_OVERRIDES:
        return [f"{JUNIT_DIR}/{CMD_JUNIT_OVERRIDES[name]}"]
    return [JUNIT_DIR]


def provenance_for(case_id: str) -> tuple[list[str], list[str]]:
    parts = case_id.split(".")
    head = parts[0]
    sub = parts[1] if len(parts) > 1 else ""
    if head == "cmd":
        return cmd_docs_for(sub), cmd_junit_for(sub)
    if head == "bulk":
        # bulk.<topic>.<num> — topic mirrors a cmd name when possible
        return cmd_docs_for(sub), cmd_junit_for(sub)
    if head == "fn":
        doc = FN_GROUP_DOC.get(sub, DOC_FNS_INDEX)
        junit = FN_GROUP_JUNIT.get(sub, f"{JUNIT_DIR}/parser/UTestCalcCompiler.java")
        return [doc], [junit]
    if head == "parser":
        return [DOC_LANG, "documentation/src/basicGORqueries.rst"], [
            f"{JUNIT_DIR}/UTestParser.java"
        ]
    if head == "syntax":
        return [DOC_LANG], [f"{JUNIT_DIR}/UTestSyntaxChecker.java"]
    if head == "semantic":
        return [DOC_LANG], [f"{JUNIT_DIR}/UTestSyntaxChecker.java"]
    if head == "script":
        return [f"{DOC_CMD}/CREATE.rst", f"{DOC_CMD}/DEF.rst"], [
            f"{JUNIT_DIR}/Script"
        ]
    if head == "mode":
        return [DOC_LANG, f"{DOC_CMD}/GORROW.rst"], [JUNIT_DIR]
    return [DOC_LANG], [JUNIT_DIR]


ID_LINE = re.compile(r"^(- id:\s*)(\S+)\s*$")


def main() -> int:
    text = CASES.read_text()
    lines = text.split("\n")
    out: list[str] = []
    rewritten = 0
    skipped_existing = 0
    i = 0
    while i < len(lines):
        line = lines[i]
        m = ID_LINE.match(line)
        if not m:
            out.append(line)
            i += 1
            continue
        case_id = m.group(2)
        # Check if next non-blank lines already declare docs/junit at indent level 2 spaces
        has_docs = False
        has_junit = False
        j = i + 1
        while j < len(lines) and (
            lines[j].startswith("  ") or lines[j].strip() == ""
        ):
            stripped = lines[j].lstrip()
            if stripped.startswith("docs:"):
                has_docs = True
            if stripped.startswith("junit:"):
                has_junit = True
            # stop scanning at next case
            if lines[j].startswith("- id:"):
                break
            j += 1
        out.append(line)
        if has_docs and has_junit:
            skipped_existing += 1
        else:
            docs, junit = provenance_for(case_id)
            if not has_docs:
                out.append("  docs:")
                for d in docs:
                    out.append(f"    - \"{d}\"")
            if not has_junit:
                out.append("  junit:")
                for j_ in junit:
                    out.append(f"    - \"{j_}\"")
            rewritten += 1
        i += 1
    new_text = "\n".join(out)
    CASES.write_text(new_text)
    print(f"Rewrote {rewritten} case(s); skipped {skipped_existing} that already had provenance.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
