#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

MASK_DIR="$ROOT/src/main/resources/assets/orbital_railgun/textures/mask"
MASK_PNG="$MASK_DIR/mask.png"
PACK_SRC="$ROOT/shaderpacks/OrbitalRailgun-Addon"
PACK_ZIP="$ROOT/shaderpacks/OrbitalRailgun-Addon.zip"

# === 1) Decode mask.png from embedded Base64 ===
echo ">> Ensuring mask directory: $MASK_DIR"
mkdir -p "$MASK_DIR"

echo ">> Writing mask.png"
base64 --decode > "$MASK_PNG" <<'B64'
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4////fwAJ+wP9KobjigAAAABJRU5ErkJggg==
B64

# Sanity check: file type
file "$MASK_PNG" || true

# === 2) Zip the shader-pack folder ===
if [[ ! -d "$PACK_SRC" ]]; then
  echo "!! Shader-pack source not found: $PACK_SRC"
  echo "   Make sure the text files were added in the previous PR."
  exit 1
fi

echo ">> Creating shader pack zip: $PACK_ZIP"
cd "$PACK_SRC/.."
# Remove old zip if present
rm -f "$(basename "$PACK_ZIP")"
zip -r "$(basename "$PACK_ZIP")" "OrbitalRailgun-Addon" >/dev/null

# === 3) Print SHA256 checksums ===
cd "$ROOT"
echo ">> SHA256 checksums:"
if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$MASK_PNG" "$PACK_ZIP"
else
  shasum -a 256 "$MASK_PNG" "$PACK_ZIP"
fi

echo ">> Done."
