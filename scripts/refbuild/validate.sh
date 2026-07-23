#!/usr/bin/env bash
#
# Validate a GOR chromSeq build by driving gorpipe against it.
#
#   validate.sh <build-dir> [variants.vcf.gz]
#
# <build-dir> is a folder produced by fasta_to_chromseq.py (contains chromSeq/,
# buildsize.gor, ...). Runs two checks:
#
#   1. Self-consistency: emit (Chrom,Pos,Ref) rows straight from the chromSeq bytes and
#      confirm GOR's refbase() returns the same base at every position (catches off-by-one
#      in GOR's reader / a wrong offset convention in the build).
#
#   2. External truth (optional): for a VCF, confirm every REF allele equals the build's
#      reference bases -- REF == upper(refbases(CHROM,POS,POS+len(REF)-1)). A real GIAB /
#      dbSNP / any correctly-built VCF must match the reference it was called against.
#
# throwif aborts the whole query on the first mismatch, so a clean exit == all rows matched.
#
# Env: GORPIPE (path to the gorpipe launcher), STEP (self-check sampling stride, default 1000).
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
build="${1:?usage: validate.sh <build-dir> [variants.vcf.gz]}"
vcf="${2:-}"
step="${STEP:-1000}"
gorpipe="${GORPIPE:-$here/../../gorscripts/build/install/gorscripts/bin/gorpipe}"

build="$(cd "$build" && pwd)"
[ -x "$gorpipe" ] || { echo "gorpipe not found at $gorpipe (set GORPIPE=)"; exit 1; }
[ -d "$build/chromSeq" ] || { echo "no chromSeq/ under $build"; exit 1; }

work="$(mktemp -d)"; trap 'rm -rf "$work"' EXIT
cfg="$work/gor_config.txt"
{
  printf 'buildPath\t%s/chromSeq\n' "$build"
  printf 'buildSizeFile\t%s/buildsize.gor\n' "$build"
  [ -f "$build/buildsplit.txt" ] && printf 'buildSplitFile\t%s/buildsplit.txt\n' "$build"
} > "$cfg"

echo ">> [1/2] self-consistency: refbase() vs stored bytes (step=$step)"
python3 "$here/chromseq_to_gor.py" "$build/chromSeq" --step "$step" > "$work/bases.gor"
rows=$(( $(wc -l < "$work/bases.gor") - 1 ))
"$gorpipe" "gor $work/bases.gor | throwif Ref != refbase(Chrom,Pos) | group genome -count" \
    -config "$cfg" -gorroot "$work" >/dev/null
echo "   OK: $rows positions, refbase() matched every one"

if [ -n "$vcf" ]; then
  vcf="$(cd "$(dirname "$vcf")" && pwd)/$(basename "$vcf")"
  echo ">> [2/2] external: REF vs upper(refbases(...)) for $(basename "$vcf")"
  "$gorpipe" "gor $vcf | throwif REF != upper(refbases(CHROM,POS,POS+len(REF)-1)) | group genome -count" \
      -config "$cfg" -gorroot "$work" >/dev/null
  echo "   OK: every REF allele matched the build's reference bases"
else
  echo ">> [2/2] skipped (no VCF given)"
fi

echo "ALL CHECKS PASSED"
