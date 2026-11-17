#!/usr/bin/env bash
# Copies Inter font files from the Inter-4.1.zip into the Android project.
# Usage: bash tools/install_fonts.sh [path/to/Inter-4.1.zip]
set -euo pipefail

ZIP="${1:-$HOME/storage/downloads/Inter-4.1.zip}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/android/app/src/main/res/font"
TMP="$HOME/inter_extract"

[ -f "$ZIP" ] || { echo "Zip not found: $ZIP"; exit 1; }
rm -rf "$TMP"; mkdir -p "$TMP" "$DEST"
unzip -q "$ZIP" -d "$TMP"

# Inter ships several file names depending on the release; try each in order.
install_weight() {
  local out="$1"; shift
  for name in "$@"; do
    src="$(find "$TMP" -type f -iname "$name" | head -n 1 || true)"
    if [ -n "$src" ]; then
      cp "$src" "$DEST/$out"
      echo "installed $out  <-  $name"
      return 0
    fi
  done
  echo "could not find a file for $out (tried: $*)"; exit 1
}

install_weight inter_regular.ttf  "Inter-Regular.ttf"  "Inter_24pt-Regular.ttf"  "Inter_18pt-Regular.ttf"
install_weight inter_medium.ttf   "Inter-Medium.ttf"   "Inter_24pt-Medium.ttf"   "Inter_18pt-Medium.ttf"
install_weight inter_semibold.ttf "Inter-SemiBold.ttf" "Inter_24pt-SemiBold.ttf" "Inter_18pt-SemiBold.ttf"
install_weight inter_bold.ttf     "Inter-Bold.ttf"     "Inter_24pt-Bold.ttf"     "Inter_18pt-Bold.ttf"

rm -rf "$TMP"
echo "Done. Fonts are in $DEST"
