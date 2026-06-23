#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BASE_URL="${1:-http://localhost:8080}"
OUTPUT="docs/api/openapi.yaml"
TEMP_FILE="$(mktemp)"

cleanup() {
  rm -f "$TEMP_FILE"
}
trap cleanup EXIT

curl --fail --silent --show-error \
  "${BASE_URL%/}/v3/api-docs.yaml" \
  -o "$TEMP_FILE"

if ! grep -q '^openapi:' "$TEMP_FILE"; then
  echo "SpringDoc response is not an OpenAPI YAML document." >&2
  exit 1
fi

mkdir -p "$(dirname "$OUTPUT")"
mv "$TEMP_FILE" "$OUTPUT"
trap - EXIT

echo "Updated $OUTPUT from ${BASE_URL%/}/v3/api-docs.yaml"
