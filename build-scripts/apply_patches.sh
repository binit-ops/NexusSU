#!/usr/bin/env bash
set -e
KERNEL_DIR="$1"
PATCH_DIR="$2"
cd "$KERNEL_DIR"
for p in ../../$PATCH_DIR/*.patch; do
  [ -f "$p" ] || continue
  echo "Applying patch $p"
  git apply "$p" || { echo "Patch $p failed"; exit 1; }
done
