#!/usr/bin/env python3
"""Emit a GOR table of (Chrom, Pos, Ref) rows read straight from a chromSeq build folder.

This is the trivial "byte at offset pos-1 is the base at 1-based pos" interpretation of the
format. Feeding it back through GOR's own reader validates that GOR agrees with that
convention -- i.e. that VARJOIN/VARMERGE/VARNORM and refbase()/refbases() read the build
correctly with no off-by-one:

    python3 chromseq_to_gor.py ref/hg38/chromSeq --step 1000 > bases.gor
    gorpipe "gor bases.gor | calc x refbase(Chrom,Pos) | throwif Ref != x"

A clean run (no throw) means GOR's seek/offset logic matches the on-disk format exactly.
Use --step to sample every Nth position (whole-chromosome breadth, cheap row count); use
--step 1 for an exhaustive position-by-position check.

By default zero bytes / 'N' / 'n' positions are skipped (GOR maps a zero byte to 'N', so
comparing there is uninformative); pass --keep-n to include them.
"""

from __future__ import annotations

import argparse
import glob
import os
import sys


def iter_files(path: str):
    if os.path.isdir(path):
        for fp in sorted(glob.glob(os.path.join(path, "*.txt"))):
            yield os.path.basename(fp)[:-4], fp
    else:
        yield os.path.basename(path)[:-4], path


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("chromseq", help="chromSeq folder, or a single <contig>.txt file")
    p.add_argument("--step", type=int, default=1,
                   help="emit every Nth position (default: 1 = every base)")
    p.add_argument("--chrom", help="restrict to this single contig name")
    p.add_argument("--keep-n", action="store_true",
                   help="also emit N/n and zero-byte positions (skipped by default)")
    args = p.parse_args()

    if args.step < 1:
        print("error: --step must be >= 1", file=sys.stderr)
        return 1

    out = sys.stdout
    out.write("Chrom\tPos\tRef\n")
    step = args.step
    for chrom, fp in iter_files(args.chromseq):
        if args.chrom and chrom != args.chrom:
            continue
        with open(fp, "rb") as f:
            data = f.read()
        # A lone trailing newline may exist; it is never a valid base position.
        n = len(data)
        if n and data[-1] == 0x0A:
            n -= 1
        for i in range(0, n, step):
            b = data[i]
            if not args.keep_n and b in (0x00, 0x4E, 0x6E):  # 0, 'N', 'n'
                continue
            out.write(f"{chrom}\t{i + 1}\t{chr(b)}\n")  # 1-based Pos = byte offset + 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
