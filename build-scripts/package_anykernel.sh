#!/usr/bin/env bash
set -e
IMAGE_PATH="$1"
ANYKERNEL_DIR="$2"
OUT_DIR="$(pwd)/out"
mkdir -p "$OUT_DIR"
if [ -f "$IMAGE_PATH" ]; then
  cp "$IMAGE_PATH" "$ANYKERNEL_DIR/Image.gz-dtb"
  cd "$ANYKERNEL_DIR"
  zip -r "$OUT_DIR/anykernel-$(date +%s).zip" ./*
  echo "Packaged $OUT_DIR/*.zip"
else
  echo "Image not found at $IMAGE_PATH; skipping packaging"
fi
