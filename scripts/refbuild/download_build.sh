#!/usr/bin/env bash
#
# Download a human reference genome and convert it into a GOR chromSeq build.
#
#   download_build.sh <hg38|hg19> [out-root] [ensembl|ucsc]
#
#   out-root  where builds are written, one subdir per build (default: ./gor_ref)
#   source    ensembl (default, GRCh naming -> renamed to chr*) or ucsc (already chr*)
#
# The reference data is large (~840 MB download, ~3 GB per build) and is intentionally kept
# OUT of the git repo -- only this script and the URLs live in the repo. Downloads are cached
# under <out-root>/.download so re-runs don't re-fetch. Pass an out-root outside the repo.
#
# Examples:
#   ./download_build.sh hg38 /data/gor_ref         # Ensembl GRCh38 -> /data/gor_ref/hg38
#   ./download_build.sh hg19 /data/gor_ref         # Ensembl GRCh37 -> /data/gor_ref/hg19
#   ./download_build.sh hg38 /data/gor_ref ucsc    # UCSC hg38
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
build="${1:?usage: download_build.sh <hg38|hg19> [out-root] [ensembl|ucsc]}"
outroot="${2:-./gor_ref}"
source="${3:-ensembl}"

# --- resolve the download URL for (build, source) ---
case "$source:$build" in
  ensembl:hg38) url="https://ftp.ensembl.org/pub/current_fasta/homo_sapiens/dna/Homo_sapiens.GRCh38.dna_sm.primary_assembly.fa.gz" ;;
  ensembl:hg19) url="https://ftp.ensembl.org/pub/grch37/current/fasta/homo_sapiens/dna/Homo_sapiens.GRCh37.dna_sm.primary_assembly.fa.gz" ;;
  ucsc:hg38)    url="https://hgdownload.soe.ucsc.edu/goldenPath/hg38/bigZips/hg38.fa.gz" ;;
  ucsc:hg19)    url="https://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/hg19.fa.gz" ;;
  *) echo "unsupported combination: source=$source build=$build (want hg38|hg19 and ensembl|ucsc)"; exit 1 ;;
esac

# Ensembl names contigs 1,2,...,MT -> add 'chr' and rename MT->M. UCSC is already chr-prefixed.
conv_flags=(--build "$build" --primary)
[ "$source" = ensembl ] && conv_flags+=(--chr-prefix add --mt M)

dldir="$outroot/.download"
outdir="$outroot/$build"
fa="$dldir/$(basename "$url")"
mkdir -p "$dldir" "$outdir"

echo ">> source=$source build=$build"
echo ">> URL: $url"
if [ -s "$fa" ]; then
  echo ">> using cached $fa"
else
  echo ">> curl -L -o $fa \\"
  echo "        $url"
  curl -fL --retry 3 -o "$fa.part" "$url"
  mv "$fa.part" "$fa"
fi
echo ">> $(du -h "$fa" | cut -f1) downloaded"

echo ">> converting -> $outdir"
python3 "$here/fasta_to_chromseq.py" "$fa" --out "$outdir" "${conv_flags[@]}"

echo ">> done. contigs: $(wc -l < "$outdir/buildsize.gor"), config: $outdir/gor_config.txt"
echo ">> validate with:  $here/validate.sh $outdir [some.vcf.gz]"
