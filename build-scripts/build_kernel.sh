#!/usr/bin/env bash
set -e
KERNEL_DIR="$1"
DEFCONFIG="$2"
TOOLCHAIN_DIR="$3"
export PATH="$(pwd)/$TOOLCHAIN_DIR/bin:$PATH"
cd "$KERNEL_DIR"
make O=out ARCH=arm64 $DEFCONFIG || true
make -j$(nproc) O=out ARCH=arm64 CC=clang || true
