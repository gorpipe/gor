# Building a GOR reference build (chromSeq) from FASTA

GOR's reference-build commands and functions — `VARJOIN`, `VARMERGE`, `VARNORM` and the
`refbase()` / `refbases()` parser functions — need a **reference build folder**. GOR does
*not* read FASTA directly. It reads a folder with **one file per chromosome/contig**, named
`<contig>.txt`, where each file is the raw base sequence stored **one byte per base with no
newlines** — the byte at file offset `pos-1` is the base at 1-based position `pos`. Case is
preserved (lowercase = soft-masked repeats); a zero byte is read as `N`.

The folder is pointed at by `buildPath` in the gor config, alongside two sibling metadata
files:

```
<out>/
  chromSeq/
    chr1.txt  chr2.txt ... chrX.txt chrY.txt chrM.txt   # one byte per base, no newlines
  buildsize.gor      # contig<TAB>size   (measured from the FASTA)
  buildsplit.txt     # contig<TAB>split  (GOR's built-in table for the build)
  gor_config.txt     # buildPath / buildSizeFile / buildSplitFile
```

Scripts in this folder:

| Script | Purpose |
|--------|---------|
| `download_build.sh`    | One-shot: download hg38/hg19 and convert to a chromSeq build. |
| `fasta_to_chromseq.py` | FASTA → chromSeq build folder (+ metadata + config). |
| `chromseq_to_gor.py`   | chromSeq folder → `(Chrom,Pos,Ref)` GOR rows, for self-validation. |
| `validate.sh`          | Drive gorpipe against a build to prove GOR reads it correctly. |

### Where the data lives

Only these scripts and the download URLs belong in the git repo — **not** the reference data
(a build is ~3 GB). Keep builds **outside** the repo, in a reference root of your choosing
(referred to below as `$REF_ROOT`, e.g. `/data/gor_ref`):

```
$REF_ROOT/
  .download/                 # cached FASTA downloads (re-runs skip re-fetching)
  hg38/  chromSeq/ buildsize.gor buildsplit.txt gor_config.txt
  hg19/  chromSeq/ buildsize.gor buildsplit.txt gor_config.txt
```

The quickest way to populate it is `download_build.sh`, which bundles the curl URLs and the
right conversion flags per source (out-root defaults to `./gor_ref`; pass one to place it
elsewhere):

```bash
./download_build.sh hg38 "$REF_ROOT"            # Ensembl GRCh38 -> $REF_ROOT/hg38
./download_build.sh hg19 "$REF_ROOT"            # Ensembl GRCh37 -> $REF_ROOT/hg19
./download_build.sh hg38 "$REF_ROOT" ucsc       # UCSC hg38 instead of Ensembl
```

Sections 1–2 below spell out the URLs and conversion by hand if you'd rather do it stepwise.

## 1. Download a reference build

### Ensembl / GRCh (primary path)

Ensembl publishes soft-masked (`dna_sm`) DNA per assembly. Names contigs `1`,`2`,…,`X`,`Y`,
`MT` (no `chr`), so convert with `--chr-prefix add --mt M` to line up with GOR's built-in
tables (`chr1`,…,`chrM`).

```bash
# GRCh38 (hg38) — whole-genome primary assembly (~840 MB gz)
curl -O https://ftp.ensembl.org/pub/current_fasta/homo_sapiens/dna/Homo_sapiens.GRCh38.dna_sm.primary_assembly.fa.gz

# GRCh37 (hg19)
curl -O https://ftp.ensembl.org/pub/grch37/current/fasta/homo_sapiens/dna/Homo_sapiens.GRCh37.dna_sm.primary_assembly.fa.gz

# Just one chromosome (handy for testing) — e.g. chr21 (~13 MB gz)
curl -O https://ftp.ensembl.org/pub/current_fasta/homo_sapiens/dna/Homo_sapiens.GRCh38.dna_sm.chromosome.21.fa.gz
```

`dna_sm.primary_assembly` includes the main chromosomes plus unplaced/unlocalized scaffolds
but excludes alt haplotypes and patches — pair it with `--primary` to keep only chr1–22, X,
Y, M. You do **not** need to decompress; the script reads gzip directly.

### UCSC (alternative)

UCSC "bigZips" are already `chr`-prefixed and soft-masked, so they need no renaming — a good
alternative when you want UCSC-style contigs. Use the plain `.fa.gz` (soft-masked); avoid the
`.masked` (hard-masked to N) variants.

```bash
curl -O https://hgdownload.soe.ucsc.edu/goldenPath/hg38/bigZips/hg38.fa.gz   # ~950 MB
curl -O https://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/hg19.fa.gz   # ~900 MB
```

> **Mitochondrion caveat.** Ensembl `MT` is the rCRS (16,569 bp) for both GRCh37 and GRCh38.
> UCSC **hg19** `chrM` is the older NC_001807 (16,571 bp) — so hg19 mtDNA differs between the
> two sources. `buildsize.gor` is measured from whichever FASTA you feed in, so the build is
> internally consistent either way; just keep your variant data on the same source.

## 2. Convert to chromSeq

```bash
# Ensembl GRCh38, primary assembly, chr-prefixed, MT -> chrM
python3 fasta_to_chromseq.py Homo_sapiens.GRCh38.dna_sm.primary_assembly.fa.gz \
        --out ref/hg38 --build hg38 --chr-prefix add --mt M --primary

# Ensembl GRCh37 (hg19)
python3 fasta_to_chromseq.py Homo_sapiens.GRCh37.dna_sm.primary_assembly.fa.gz \
        --out ref/hg19 --build hg19 --chr-prefix add --mt M --primary

# UCSC hg38 (already chr-prefixed — no renaming needed)
python3 fasta_to_chromseq.py hg38.fa.gz --out ref/hg38 --build hg38 --primary
```

Drop `--primary` to keep every contig (scaffolds, alts, decoy, …). The script streams the
input, so memory stays flat regardless of genome size; expect it to be I/O-bound (a few
minutes for a whole genome).

### Options

| Flag | Purpose |
|------|---------|
| `--out, -o DIR` | Output build folder (required). |
| `--build {hg19,hg38,hg18,generic}` | Emit `buildsplit.txt` from GOR's built-in split table. Omit to skip it (GOR falls back to its hard-coded defaults). |
| `--chr-prefix {keep,add,strip}` | `keep` (default) leaves names as-is; `add`/`strip` add or remove the `chr` prefix. Use `add` for Ensembl. |
| `--mt {keep,M,MT}` | Canonicalize the mitochondrial contig spelling. GOR's tables use `chrM` → use `M` for Ensembl. |
| `--primary` | Keep only chr1–22, X, Y, M/MT. |
| `--include REGEX` / `--exclude REGEX` | Keep/drop contigs by (post-transform) name. |

The two metadata files come from very different places — see below.

### The two metadata files: `buildsize.gor` vs `buildsplit.txt`

**`buildsize.gor`** (`contig<TAB>size`) is **measured directly from the FASTA** — one line per
converted contig, giving the exact base count. It is always correct for whatever you fed in,
including partial or custom references. GOR uses it to bounds-check positions.

**`buildsplit.txt`** (`contig<TAB>splitPosition`) is **not derived from the sequence at all**.
It lists a single split position — the **centromere** — for the ~13 largest chromosomes
(chr1–12 and X). Split positions are assembly constants that can't be read out of the bases,
so the script carries GOR's own values, mirrored from the Java source
(`ReferenceBuildDefaults.java`: `buildSplitHg38()` / `buildSplitHg18()`), and writes the subset
matching the contigs it actually converted. It is emitted **only** when you pass `--build`.

- **What GOR uses it for:** PGOR/PARTGOR's `SplitManager` splits each listed chromosome into
  two partitions (p-arm / q-arm) at the split point, for finer-grained parallelism. Contigs
  not listed are small enough to run as a single partition. It has **no effect on the base
  data** — it is purely a parallelization hint, and is never consulted by `refbase()`,
  `refbases()`, `VARJOIN`, `VARMERGE` or `VARNORM`.
- **hg19 == hg18:** GOR uses one split table for both (`buildSplit_hg19 = buildSplit_hg18`), so
  `--build hg19` and `--build hg18` write identical split values (the script backs both with a
  single `_SPLIT_HG19_18` table); `hg38` has its own.
- **Optional:** if you omit `--build`, no `buildsplit.txt` is written and GOR falls back to its
  hard-coded hg19/hg38 split defaults anyway — PGOR still splits correctly; the file just makes
  the build self-contained.

## 3. Point GOR at the build

Classic `gorpipe` takes `-config <file>` and `-gorroot <project>`; a relative `buildPath` is
resolved against the project root. The generated `gor_config.txt` uses relative paths:

```
buildPath	chromSeq
buildSizeFile	buildsize.gor
buildSplitFile	buildsplit.txt
```

The build already ships a `gor_config.txt` with those relative paths, so the simplest
invocation points `-gorroot` at the build folder itself:

```bash
GORPIPE=…/gorscripts/build/install/gorscripts/bin/gorpipe
BUILD="$REF_ROOT/hg38"

# read reference bases straight out of the build
$GORPIPE "gorrow chr21,5100001,5100010 | calc base refbase('chr21',5100001) \
                                        | calc win  refbases('chr21',5100001,5100010)" \
         -config "$BUILD/gor_config.txt" -gorroot "$BUILD"
# -> base G   win GCCTGCCCAG
```

(For a project config elsewhere, either merge the three keys in — resolving the relative
paths against where that config lives — or write an absolute `buildPath` to
`$BUILD/chromSeq`.)

## 4. Validate

`validate.sh` runs both checks below against a build folder (writes a temp config for you):

```bash
# self-consistency only
./validate.sh ref/hg38

# self-consistency + external VCF cross-check
./validate.sh ref/hg38 giab.chr21.vcf.gz
# env: GORPIPE=<launcher>  STEP=<self-check stride, default 1000>
```

Both queries use `throwif`, which aborts the whole query on the first mismatch — so a clean
run means every row matched.

**a. Self-consistency — GOR agrees with the on-disk format.** Emit each stored base as a row
and confirm `refbase()` returns the same base (catches off-by-one / wrong offset):

```bash
python3 chromseq_to_gor.py ref/hg38/chromSeq --step 1000 > bases.gor
$GORPIPE "gor bases.gor | calc x refbase(Chrom,Pos) | throwif Ref != x" \
         -config gor_config.txt -gorroot .
```

**b. External truth — the build is a correct reference.** For any VCF called against the
same assembly, every REF allele must equal the build's reference bases:

```bash
$GORPIPE "gor giab.vcf.gz | throwif REF != upper(refbases(CHROM,POS,POS+len(REF)-1))" \
         -config gor_config.txt -gorroot .
```

`refbases(chrom, pos1, pos2)` returns the inclusive `pos1..pos2` window, so a REF of length L
at POS spans `POS .. POS+len(REF)-1` — covering SNVs and multi-base indels alike. `upper(...)`
normalizes soft-masked (lowercase) reference bases to match the uppercase VCF alleles.

### GIAB sample (worked example)

[GIAB](https://www.nist.gov/programs-projects/genome-bottle) publishes high-confidence
benchmark VCFs. Fetch just one chromosome with `tabix` (streams the region using the remote
index — no full download):

```bash
URL=https://ftp-trace.ncbi.nlm.nih.gov/ReferenceSamples/giab/release/NA12878_HG001/NISTv4.2.1/GRCh38/HG001_GRCh38_1_22_v4.2.1_benchmark.vcf.gz
tabix -h "$URL" chr21 | bgzip > giab.chr21.vcf.gz
./validate.sh ref/hg38 giab.chr21.vcf.gz
```

This exact flow is verified end-to-end: an Ensembl GRCh38 chr21 build validated **all 54,828**
GIAB HG001 chr21 REF alleles (SNVs and indels) and every sampled `refbase()` position, with no
mismatch. The GIAB GRCh38 benchmark is `chr`-prefixed, matching a build made with
`--chr-prefix add`.

> GIAB also ships a GRCh37 benchmark under `.../NISTv4.2.1/GRCh37/` — validate that one against
> your GRCh37/hg19 build. Always match the VCF's assembly to the build.

## 5. Verify (round-trip)

`fasta_to_chromseq.py` is round-trip verified against GOR's own test build: reconstructing
FASTA from `tests/data/ref_mini/chromSeq/` and running it back through the script reproduces
all 25 contig files byte-for-byte.

## Format reference (for the curious)

- **Reader:** `model/.../gor/iterators/RefSeqFromChromSeq.scala` — seeks to `(pos-1)`, reads
  one byte per base; positions are 1-based.
- **Functions:** `gortools/.../parser/GenomeFunctions.scala` — `REFBASE(chrom,pos)`,
  `REFBASES(chrom,pos1,pos2)`, `REFBASES_WITH_BUILD(chrom,pos1,pos2,build)`.
- **Config keys:** `model/.../gor/reference/ReferenceBuild.java` — `buildPath`,
  `buildSizeFile`, `buildSplitFile`.
- **Built-in size/split tables:** `model/.../gor/reference/ReferenceBuildDefaults.java`.
- **The separate CRAM reference path** (`cramReferencePath`, MD5-indexed FASTA) is unrelated
  to this chromSeq build and is not produced here.
