#!/usr/bin/env bash
set -euo pipefail

# Fetches datadog-iot-agent .deb Filename + SHA256 from apt.datadoghq.com
# for arm64, armhf, and amd64.
# Usage: ./fetch-deb-metadata.sh [version]   (e.g. 7.85.5; defaults to latest per arch)

TARGET_VER="${1:-}"
ARCHES=("arm64" "armhf" "amd64")
BASE_URL="https://apt.datadoghq.com"

for arch in "${ARCHES[@]}"; do
    echo "=== ${arch} ==="
    curl -fsSL "${BASE_URL}/dists/stable/7/binary-${arch}/Packages" | awk -v target="${TARGET_VER}" '
        /^Package: datadog-iot-agent$/ { in_pkg=1; ver=""; file=""; sha="" }
        in_pkg && /^Version:/  { ver=$2 }
        in_pkg && /^Filename:/ { file=$2 }
        in_pkg && /^SHA256:/   { sha=$2 }
        in_pkg && /^$/ {
            if (ver != "" && (target == "" || ver == "1:" target "-1" || ver == target)) {
                printf "%s\t%s\t%s\n", ver, file, sha
            }
            in_pkg=0
        }
    ' | sort -V | tail -n 1 | awk -F'\t' -v base="${BASE_URL}" '
        NF == 3 {
            printf "Version:  %s\nURL:      %s/%s\nSHA256:   %s\n", $1, base, $2, $3
        }
        NF == 0 {
            print "Not found"
        }
    '
done
