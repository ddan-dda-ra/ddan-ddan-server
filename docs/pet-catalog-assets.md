# Pet Catalog Assets

New asset releases use immutable, versioned URLs:

```text
https://ddan-ddan-cdn.ddmz.org/releases/{release}/{species}/level{1..5}.svg
https://ddan-ddan-cdn.ddmz.org/releases/{release}/{species}/level{1..5}_default.json
https://ddan-ddan-cdn.ddmz.org/releases/{release}/{species}/level{1..5}_play_eat.json
```

`release` is immutable. Never overwrite files under an existing release. When
any asset content changes, choose a new release id and update the affected
catalog URLs so clients can detect changed assets by URL comparison.

Species are `cat`, `hamster`, `penguin`, `dog`, `mole`, `fox`, `duck`, and `hedgehog`.

## Required Files

The complete catalog requires 120 files:

- 8 species
- 5 level SVG files per species
- 10 Lottie JSON files per species (`default`, `play_eat`)

Check the expected manifest:

```bash
node scripts/pet-catalog-assets.mjs manifest \
  --release 2026-07-01 \
  --dir tmp/pet-catalog-assets
```

## Build From iOS Bundle Assets

The current iOS app already contains pet level images and Lottie JSON files.
Level images are emitted as SVG files that embed the existing iOS PNG assets.

Build the full upload set from the sibling iOS repository:

```bash
node scripts/pet-catalog-assets.mjs assemble-ios \
  --dir tmp/pet-catalog-assets \
  --ios-root ../ddan-ddan-ios-svg/DDanDDan
```

This should finish with:

```text
SUMMARY ok=120 missing=0 total=120
```

## Figma Export Layout

Export the Figma assets into this local layout before upload:

```text
tmp/pet-catalog-assets/
  cat/
    level1.svg
    level1_default.json
    level1_play_eat.json
    ...
  hamster/
  penguin/
  dog/
  mole/
  fox/
  duck/
  hedgehog/
```

After export, verify the local file set:

```bash
node scripts/pet-catalog-assets.mjs local-check --dir tmp/pet-catalog-assets
```

## R2 Upload

Authenticate Wrangler first. In Codex/non-interactive shells, prefer an API
token:

```bash
export CLOUDFLARE_API_TOKEN=<token>
```

For an interactive local shell, browser login also works:

```bash
npx --yes wrangler@latest login
```

Dry-run the upload:

```bash
node scripts/pet-catalog-assets.mjs upload \
  --release 2026-07-01 \
  --dir tmp/pet-catalog-assets \
  --bucket <r2-bucket-name> \
  --dry-run
```

Upload:

```bash
node scripts/pet-catalog-assets.mjs upload \
  --release 2026-07-01 \
  --dir tmp/pet-catalog-assets \
  --bucket <r2-bucket-name>
```

Before a remote upload, the command queries every key directly from the R2
bucket with `wrangler r2 object get`. If any key already exists, or R2 cannot
authoritatively confirm that a key is absent, it aborts before uploading the
first file. CDN `HEAD` is used only by the separate post-upload `check` command.
The upload command targets remote R2 by default. For local Wrangler storage
tests only, pass `--local` explicitly (local mode skips the remote R2 preflight):

```bash
node scripts/pet-catalog-assets.mjs upload \
  --release local-test-1 \
  --dir tmp/pet-catalog-assets \
  --bucket <r2-bucket-name> \
  --local
```

If Wrangler is installed outside `PATH`, pass the binary directly:

```bash
node scripts/pet-catalog-assets.mjs upload \
  --release 2026-07-01 \
  --dir tmp/pet-catalog-assets \
  --bucket <r2-bucket-name> \
  --wrangler-bin /path/to/wrangler
```

Existing background objects in CDN/R2 are intentionally left untouched. This
pipeline no longer generates, validates, or uploads them.

The database field removal and deployment order are documented in the
[pet catalog backgrounds removal runbook](runbooks/pet-catalog-backgrounds-removal.md).

## CDN Verification

Verify the public CDN URLs after upload:

```bash
node scripts/pet-catalog-assets.mjs check --release 2026-07-01
```

The API asset work is complete only when the check summary is:

```text
SUMMARY ok=120 missing=0 other=0 total=120
```
