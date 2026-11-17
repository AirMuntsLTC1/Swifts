#!/usr/bin/env bash
# Copies the four Inter weights the app uses into assets/fonts.
# Usage: bash tools/install_fonts.sh [path/to/Inter-4.1.zip]
set -euo pipefail

ZIP="${1:-$HOME/storage/downloads/Inter-4.1.zip}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/android/app/src/main/assets/fonts"
TMP="$HOME/inter_extract"

[ -f "$ZIP" ] || { echo "Zip not found: $ZIP"; exit 1; }
rm -rf "$TMP" "$DEST"; mkdir -p "$TMP" "$DEST"
unzip -q "$ZIP" -d "$TMP"

canon() {
  local out="$1"; shift
  for name in "$@"; do
    src="$(find "$TMP" -type f -iname "$name" | head -n 1 || true)"
    if [ -n "$src" ]; then cp "$src" "$DEST/$out"; echo "installed $out"; return 0; fi
  done
  echo "missing weight for $out"; exit 1
}
canon inter_regular.ttf  "Inter-Regular.ttf"  "Inter_24pt-Regular.ttf"  "Inter_18pt-Regular.ttf"
canon inter_medium.ttf   "Inter-Medium.ttf"   "Inter_24pt-Medium.ttf"   "Inter_18pt-Medium.ttf"
canon inter_semibold.ttf "Inter-SemiBold.ttf" "Inter_24pt-SemiBold.ttf" "Inter_18pt-SemiBold.ttf"
canon inter_bold.ttf     "Inter-Bold.ttf"     "Inter_24pt-Bold.ttf"     "Inter_18pt-Bold.ttf"

rm -rf "$ROOT/android/app/src/main/res/font" "$TMP"
echo "Done. Fonts are in $DEST"
