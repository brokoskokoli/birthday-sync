#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Stefan Richter
# SPDX-License-Identifier: MIT
# Renders the website's images from the app icon (SVG; PNG or .icns also work, scaled with sips):
#   website/favicon.png (64), website/apple-touch-icon.png (180), website/images/icon.png (512)
# and the link preview website/images/social-preview.png (1280 x 640) from the template
# packaging/brand/social-preview.svg. Only needed after changing the icon or the template; the
# PNGs are committed. Requires resvg (brew install resvg / cargo install resvg). The preview's
# text uses a system font (Helvetica Neue on macOS); render on the same OS for identical results.
#
#   make_website_images.sh [--root <project dir>] [<icon.svg>] [<social-preview.svg>]
#
# Works from the skill folder or copied to <project>/tools/. Paths are relative to the project root:
# --root, else the git top level of the current directory.
set -euo pipefail
ROOT=""
if [[ "${1:-}" == "--root" ]]; then
  [[ -n "${2:-}" ]] || { echo "--root needs a directory" >&2; exit 2; }
  ROOT="$2"; shift 2
fi
[[ -n "$ROOT" ]] || ROOT=$(git rev-parse --show-toplevel 2>/dev/null) \
  || { echo "not inside a git repository - pass --root <project dir>" >&2; exit 2; }
cd "$ROOT"
[[ -d /opt/homebrew/bin ]] && export PATH="/opt/homebrew/bin:$PATH"

ICON="${1:-packaging/brand/icon.svg}"
PREVIEW="${2:-packaging/brand/social-preview.svg}"
command -v resvg >/dev/null || { echo "resvg missing (brew install resvg)" >&2; exit 1; }
[[ -f "$ICON" ]] || { echo "icon not found: $ICON" >&2; exit 1; }
[[ -f "$PREVIEW" ]] || { echo "social preview template not found: $PREVIEW" >&2; exit 1; }

mkdir -p website/images
case "$ICON" in
  *.svg)
    resvg -w 64 -h 64 "$ICON" website/favicon.png
    resvg -w 180 -h 180 "$ICON" website/apple-touch-icon.png
    resvg -w 512 -h 512 "$ICON" website/images/icon.png
    ;;
  *.png|*.icns)
    # Apps without an SVG icon (Xcode/SwiftPM): use the largest bitmap. .icns → iconset first.
    SRC="$ICON"
    if [[ "$ICON" == *.icns ]]; then
      TMP_ICONSET=$(mktemp -d)/icon.iconset
      iconutil -c iconset "$ICON" -o "$TMP_ICONSET"
      SRC="$TMP_ICONSET/icon_512x512@2x.png"
      [[ -f "$SRC" ]] || SRC=$(find "$TMP_ICONSET" -name '*.png' -exec stat -f '%z %N' {} + | sort -rn | head -n1 | cut -d' ' -f2-)
    fi
    for spec in 64:website/favicon.png 180:website/apple-touch-icon.png 512:website/images/icon.png; do
      sips -s format png -Z "${spec%%:*}" "$SRC" --out "${spec#*:}" >/dev/null
    done
    ;;
  *) echo "icon must be .svg, .png or .icns: $ICON" >&2; exit 1 ;;
esac
# resvg resolves the <image> href relative to the template's directory (an SVG or PNG icon).
resvg -w 1280 -h 640 --resources-dir "$(dirname "$PREVIEW")" "$PREVIEW" website/images/social-preview.png

# Optional: shrink the PNGs (lossless) if oxipng is installed.
if command -v oxipng >/dev/null; then
  oxipng -q -o 4 --strip safe website/favicon.png website/apple-touch-icon.png \
    website/images/icon.png website/images/social-preview.png
fi
echo "wrote website/favicon.png, website/apple-touch-icon.png, website/images/icon.png, website/images/social-preview.png"
