#!/usr/bin/env python3
"""Convert standard FASTA reference genome files into the GOR "chromSeq" build format.

GOR's reference-build commands and functions -- VARJOIN, VARMERGE, VARNORM and the
refbase()/refbases() parser functions -- read reference bases from a build folder that
holds ONE file per chromosome/contig, named ``<contig>.txt``. Each file is the raw base
sequence stored as one byte per base, with NO interspersed newlines: the byte at file
offset ``pos-1`` is the base at 1-based genomic position ``pos``. Case is preserved
(lowercase = soft-masked/repeat regions); a zero byte is treated as ``N`` by the engine.

The folder is pointed at by ``buildPath`` in the gor config file, next to two sibling
metadata files:
  * ``buildsize.gor``  -- tab-separated ``contig<TAB>size`` (chromosome lengths)
  * ``buildsplit.txt`` -- tab-separated ``contig<TAB>splitPosition`` (centromere split)

This script streams one or more FASTA files (plain or gzipped) and emits that layout:

    chromSeq/<contig>.txt   one byte per base, no newlines
    buildsize.gor           measured directly from the FASTA
    buildsplit.txt          gor's built-in table for the chosen build (with --build)
    gor_config.txt          ready-to-use config pointing at the three above

Example
-------
    # UCSC hg38 (already chr-prefixed, soft-masked -- ideal input)
    python3 fasta_to_chromseq.py hg38.fa.gz --out ref/hg38 --build hg38 --primary

    # Ensembl GRCh38 (contigs named "1".."22","X","MT" -> add chr prefix, MT -> M)
    python3 fasta_to_chromseq.py GRCh38.fa.gz --out ref/hg38 --build hg38 \
            --chr-prefix add --mt M --primary
"""

from __future__ import annotations

import argparse
import gzip
import os
import re
import sys

# gor's built-in size/split tables (mirrors ReferenceBuildDefaults.java). buildsize is
# always measured from the FASTA instead; the split tables are used for buildsplit.txt.
# hg19 and hg18 share one split table in gorpipe (buildSplit_hg19 = buildSplit_hg18 in
# ReferenceBuildDefaults.java), so a single dict backs both.
_SPLIT_HG19_18 = {
    "chr1": 124000000, "chr2": 93100000, "chr3": 91350000, "chr4": 50750000,
    "chr5": 47650000, "chr6": 60125000, "chr7": 59330000, "chr8": 45500000,
    "chr9": 53700000, "chr10": 40350000, "chr11": 52750000, "chr12": 35000000,
    "chrX": 60000000,
}
_SPLIT_HG38 = {
    "chr1": 123400000, "chr2": 93900000, "chr3": 90900000, "chr4": 50000000,
    "chr5": 48800000, "chr6": 59800000, "chr7": 60100000, "chr8": 45200000,
    "chr9": 43000000, "chr10": 39800000, "chr11": 53400000, "chr12": 35500000,
    "chrX": 61000000,
}
SPLIT_TABLES = {
    "hg18": _SPLIT_HG19_18,
    "hg19": _SPLIT_HG19_18,
    "hg38": _SPLIT_HG38,
    "generic": _SPLIT_HG19_18,
}

# Primary assembly: autosomes 1-22 plus X, Y, and the mitochondrion (M or MT).
PRIMARY_CORES = {str(i) for i in range(1, 23)} | {"X", "Y", "M", "MT"}

FASTA_EXTS = (".fa", ".fasta", ".fna", ".fa.gz", ".fasta.gz", ".fna.gz")


def core_name(name: str) -> str:
    """Strip a leading 'chr' (case-insensitive) to get the bare contig core, e.g. '1', 'X', 'MT'."""
    return name[3:] if name.lower().startswith("chr") else name


def transform_name(name: str, chr_prefix: str, mt: str) -> str:
    """Apply --chr-prefix and --mt normalization to a contig name."""
    core = core_name(name)
    if mt != "keep" and core.upper() in ("M", "MT"):
        core = mt  # canonicalize the mitochondrion to the requested spelling
        name = "chr" + core if name.lower().startswith("chr") else core
    if chr_prefix == "add":
        return "chr" + core
    if chr_prefix == "strip":
        return core
    return name  # keep


def open_fasta(path: str):
    """Open a FASTA file for binary line iteration, transparently handling gzip."""
    if path.endswith(".gz"):
        return gzip.open(path, "rb")
    with open(path, "rb") as probe:
        magic = probe.read(2)
    if magic == b"\x1f\x8b":  # gzip magic, regardless of extension
        return gzip.open(path, "rb")
    return open(path, "rb")


def gather_inputs(inputs: list[str]) -> list[str]:
    """Expand any directories in the input list into the FASTA files they contain."""
    files: list[str] = []
    for item in inputs:
        if os.path.isdir(item):
            for entry in sorted(os.listdir(item)):
                if entry.lower().endswith(FASTA_EXTS):
                    files.append(os.path.join(item, entry))
        else:
            files.append(item)
    return files


def convert(args) -> int:
    chromseq_dir = os.path.join(args.out, "chromSeq")
    os.makedirs(chromseq_dir, exist_ok=True)

    include = re.compile(args.include) if args.include else None
    exclude = re.compile(args.exclude) if args.exclude else None

    sizes: list[tuple[str, int]] = []      # (output_name, length) in encounter order
    written: set[str] = set()              # guards against duplicate contigs
    skipped: list[str] = []

    for path in gather_inputs(args.inputs):
        print(f"Reading {path}", file=sys.stderr)
        with open_fasta(path) as fh:
            out = None
            out_name = None
            length = 0
            for line in fh:
                if line[:1] == b">":
                    if out is not None:
                        out.close()
                        sizes.append((out_name, length))
                        print(f"  {out_name}\t{length}", file=sys.stderr)
                    raw = line[1:].split()[0].decode("ascii", "replace")
                    out_name = transform_name(raw, args.chr_prefix, args.mt)
                    core = core_name(out_name)
                    keep = True
                    if args.primary and core.upper() not in PRIMARY_CORES:
                        keep = False
                    if include and not include.search(out_name):
                        keep = False
                    if exclude and exclude.search(out_name):
                        keep = False
                    if not keep:
                        skipped.append(raw)
                        out, out_name, length = None, None, 0
                        continue
                    if out_name in written:
                        print(f"error: contig '{out_name}' appears more than once "
                              f"(from '{raw}')", file=sys.stderr)
                        return 1
                    written.add(out_name)
                    out = open(os.path.join(chromseq_dir, out_name + ".txt"), "wb")
                    length = 0
                elif out is not None:
                    seq = line.strip()  # drop the newline and any stray whitespace
                    if seq:
                        out.write(seq)
                        length += len(seq)
            if out is not None:
                out.close()
                sizes.append((out_name, length))
                print(f"  {out_name}\t{length}", file=sys.stderr)

    if not sizes:
        print("error: no contigs written (check filters / input paths)", file=sys.stderr)
        return 1

    # buildsize.gor -- measured lengths, no header (matches gor's parser).
    size_path = os.path.join(args.out, "buildsize.gor")
    with open(size_path, "w") as f:
        for name, length in sizes:
            f.write(f"{name}\t{length}\n")

    # buildsplit.txt -- gor's built-in split table for the build, keyed to our names.
    split_written = False
    if args.build:
        table = SPLIT_TABLES[args.build]
        written_names = {n for n, _ in sizes}
        rows = []
        for chr_key, pos in table.items():
            mapped = transform_name(chr_key, args.chr_prefix, args.mt)
            if mapped in written_names:
                rows.append((mapped, pos))
        if rows:
            with open(os.path.join(args.out, "buildsplit.txt"), "w") as f:
                for name, pos in rows:
                    f.write(f"{name}\t{pos}\n")
            split_written = True

    # gor_config.txt -- relative paths, ready to drop into a project's config.
    with open(os.path.join(args.out, "gor_config.txt"), "w") as f:
        f.write("buildPath\tchromSeq\n")
        f.write("buildSizeFile\tbuildsize.gor\n")
        if split_written:
            f.write("buildSplitFile\tbuildsplit.txt\n")

    total = sum(length for _, length in sizes)
    print(f"\nWrote {len(sizes)} contig file(s), {total:,} bases -> {chromseq_dir}",
          file=sys.stderr)
    print(f"Metadata: buildsize.gor"
          + (", buildsplit.txt" if split_written else "")
          + ", gor_config.txt", file=sys.stderr)
    if skipped:
        print(f"Skipped {len(skipped)} contig(s): "
              + ", ".join(skipped[:10]) + (" ..." if len(skipped) > 10 else ""),
              file=sys.stderr)
    return 0


def main() -> int:
    p = argparse.ArgumentParser(
        description="Convert FASTA reference files into GOR chromSeq build format.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__.split("Example")[1] if "Example" in __doc__ else None,
    )
    p.add_argument("inputs", nargs="+",
                   help="FASTA file(s) (.fa/.fasta/.fna, optionally .gz) or a directory of them")
    p.add_argument("--out", "-o", required=True,
                   help="output build folder (chromSeq/ and metadata are written here)")
    p.add_argument("--build", choices=sorted(SPLIT_TABLES),
                   help="emit buildsplit.txt using gor's built-in split table for this build")
    p.add_argument("--chr-prefix", choices=("keep", "add", "strip"), default="keep",
                   help="contig naming: keep as-is, add a 'chr' prefix, or strip it (default: keep)")
    p.add_argument("--mt", choices=("keep", "M", "MT"), default="keep",
                   help="normalize the mitochondrial contig spelling (default: keep)")
    p.add_argument("--primary", action="store_true",
                   help="keep only the primary assembly (chr1-22, X, Y, M/MT)")
    p.add_argument("--include", metavar="REGEX",
                   help="keep only contigs whose (transformed) name matches this regex")
    p.add_argument("--exclude", metavar="REGEX",
                   help="drop contigs whose (transformed) name matches this regex")
    args = p.parse_args()
    return convert(args)


if __name__ == "__main__":
    sys.exit(main())
