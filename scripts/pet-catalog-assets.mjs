#!/usr/bin/env node

import { spawn } from "node:child_process";
import { existsSync } from "node:fs";
import { copyFile, mkdir, mkdtemp, readFile, rm, stat, writeFile } from "node:fs/promises";
import { request } from "node:https";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const CDN_BASE = "https://ddan-ddan-cdn.ddmz.org";
const MAX_LEVEL = 5;
const PETS = [
  { type: "CAT", species: "cat" },
  { type: "HAMSTER", species: "hamster" },
  { type: "PENGUIN", species: "penguin" },
  { type: "DOG", species: "dog" },
  { type: "MOLE", species: "mole" },
  { type: "FOX", species: "fox" },
  { type: "DUCK", species: "duck" },
  { type: "HEDGEHOG", species: "hedgehog" },
];

const USAGE = `
Usage:
  node scripts/pet-catalog-assets.mjs manifest --release <releaseId> [--dir <assetDir>]
  node scripts/pet-catalog-assets.mjs assemble-ios --dir <assetDir> --ios-root <ddan-ddan-ios-svg/DDanDDan>
  node scripts/pet-catalog-assets.mjs local-check --dir <assetDir>
  node scripts/pet-catalog-assets.mjs check --release <releaseId> [--cdn-base <url>]
  node scripts/pet-catalog-assets.mjs upload --release <releaseId> --dir <assetDir> --bucket <r2Bucket> [--dry-run] [--local] [--wrangler-bin <path>]

Expected local layout:
  <assetDir>/cat/level1.svg
  <assetDir>/cat/level1_default.json
  <assetDir>/cat/level1_play_eat.json
  ... same for levels 1..5 and species cat, hamster, penguin, dog, mole, fox, duck, hedgehog
`;

const IOS_ASSET_MAP = {
  cat: {
    imageSetPrefix: "pink",
    lottiePrefix: "cat",
  },
  hamster: {
    imageSetPrefix: "green",
    lottiePrefix: "hamster",
  },
  penguin: {
    imageSetPrefix: "blue",
    lottiePrefix: "penguin",
  },
  dog: {
    imageSetPrefix: "purple",
    lottiePrefix: "puppy",
  },
  mole: {
    imageSetPrefix: "gray",
    lottiePrefix: "mole",
  },
  fox: {
    imageSetPrefix: "fox",
    lottiePrefix: "fox",
  },
  duck: {
    imageSetPrefix: "duck",
    lottiePrefix: "duck",
  },
  hedgehog: {
    imageSetPrefix: "hedgehog",
    lottiePrefix: "hedgehog",
  },
};

function parseArgs(argv) {
  const [command, ...rest] = argv;
  const args = { command };
  for (let index = 0; index < rest.length; index += 1) {
    const token = rest[index];
    if (!token.startsWith("--")) {
      throw new Error(`Unexpected argument: ${token}`);
    }
    const key = token.slice(2);
    if (key === "dry-run") {
      args.dryRun = true;
    } else if (key === "local") {
      args.local = true;
    } else {
      const value = rest[index + 1];
      if (!value || value.startsWith("--")) {
        throw new Error(`Missing value for --${key}`);
      }
      args[toCamelCase(key)] = value;
      index += 1;
    }
  }
  return args;
}

function toCamelCase(value) {
  return value.replace(/-([a-z])/g, (_, char) => char.toUpperCase());
}

function validateRelease(release) {
  if (!release || !/^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/.test(release)) {
    throw new Error("--release must be 1..64 characters using letters, numbers, dot, underscore, or hyphen");
  }
  if (release.toLowerCase() === "latest") {
    throw new Error("--release latest is forbidden because asset URLs must be immutable");
  }
  return release;
}

function buildManifest(assetDir = "", release = "") {
  const files = [];
  for (const pet of PETS) {
    for (let level = 1; level <= MAX_LEVEL; level += 1) {
      files.push(assetFile(pet, `level${level}.svg`, "image/svg+xml", assetDir, release));
      files.push(assetFile(pet, `level${level}_default.json`, "application/json", assetDir, release));
      files.push(assetFile(pet, `level${level}_play_eat.json`, "application/json", assetDir, release));
    }
  }
  return files;
}

function assetFile(pet, relativePath, contentType, assetDir, release) {
  const localRelativePath = `${pet.species}/${relativePath}`;
  const key = release ? `releases/${release}/${localRelativePath}` : localRelativePath;
  return {
    type: pet.type,
    species: pet.species,
    key,
    contentType,
    localPath: assetDir ? path.join(assetDir, localRelativePath) : localRelativePath,
    url: `${CDN_BASE}/${key}`,
  };
}

async function checkLocalFiles(files) {
  const rows = [];
  for (const file of files) {
    const exists = existsSync(file.localPath);
    const size = exists ? (await stat(file.localPath)).size : 0;
    rows.push({ ...file, exists, size });
  }
  return rows;
}

function head(url, redirects = 0) {
  return new Promise((resolve) => {
    const req = request(url, { method: "HEAD", timeout: 8000 }, (res) => {
      const status = res.statusCode ?? 0;
      const location = res.headers.location;
      res.resume();
      if ([301, 302, 303, 307, 308].includes(status) && location && redirects < 5) {
        resolve(head(new URL(location, url).toString(), redirects + 1));
        return;
      }
      resolve({
        status,
        contentType: res.headers["content-type"] ?? "",
        contentLength: res.headers["content-length"] ?? "",
      });
    });
    req.on("timeout", () => req.destroy(new Error("timeout")));
    req.on("error", (error) => resolve({ status: "ERR", error: error.message }));
    req.end();
  });
}

function run(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: "inherit" });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`${command} ${args.join(" ")} exited with ${code}`));
      }
    });
  });
}

function runCaptured(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: ["ignore", "pipe", "pipe"] });
    let output = "";
    child.stdout.on("data", (chunk) => { output += chunk.toString(); });
    child.stderr.on("data", (chunk) => { output += chunk.toString(); });
    child.on("error", reject);
    child.on("close", (code) => resolve({ code: code ?? 1, output }));
  });
}

function wranglerInvocation(wranglerBin) {
  return {
    command: wranglerBin ?? "npx",
    prefix: wranglerBin ? [] : ["--yes", "wrangler@latest"],
  };
}

export function isR2ObjectNotFound(output) {
  return /(?:the specified key does not exist|the specified object does not exist)[.!]?\s*$/i.test(output.trim());
}

export async function assertRemoteKeysAbsent(files, bucket, wranglerBin, runner = runCaptured) {
  const tempRoot = await mkdtemp(path.join(tmpdir(), "pet-catalog-r2-preflight-"));
  const { command, prefix } = wranglerInvocation(wranglerBin);
  try {
    for (let index = 0; index < files.length; index += 1) {
      const file = files[index];
      const target = `${bucket}/${file.key}`;
      const destination = path.join(tempRoot, String(index));
      const result = await runner(command, [
        ...prefix,
        "r2",
        "object",
        "get",
        target,
        "--remote",
        "--file",
        destination,
      ]);
      if (result.code === 0) {
        throw new Error(`Immutable R2 key already exists: r2://${target}. Choose a new --release.`);
      }
      if (!isR2ObjectNotFound(result.output)) {
        throw new Error(`Could not authoritatively verify R2 key absence for r2://${target}: ${result.output.trim() || `exit ${result.code}`}`);
      }
    }
  } finally {
    await rm(tempRoot, { recursive: true, force: true });
  }
}

async function assembleFromIos(assetDir, iosRoot) {
  if (!existsSync(iosRoot)) {
    throw new Error(`iOS root does not exist: ${iosRoot}`);
  }

  const homeAssetsRoot = path.join(iosRoot, "Resource", "Assets.xcassets", "Home");
  const lottieRoot = path.join(iosRoot, "Resource", "Lottie");

  for (const pet of PETS) {
    const mapping = IOS_ASSET_MAP[pet.species];
    if (!mapping) throw new Error(`Missing iOS mapping for ${pet.species}`);

    for (let level = 1; level <= MAX_LEVEL; level += 1) {
      const imageSetName = level === 1 ? `${mapping.imageSetPrefix}_egg` : `${mapping.imageSetPrefix}_lv${level - 1}`;
      const sourceImage = await preferredImageFromSet(path.join(homeAssetsRoot, `${imageSetName}.imageset`));
      await writePngBackedSvg(sourceImage, path.join(assetDir, pet.species, `level${level}.svg`), 300, 300);

      await copyAsset(
        resolveLottiePath(lottieRoot, mapping.lottiePrefix, level, "default"),
        path.join(assetDir, pet.species, `level${level}_default.json`),
      );
      await copyAsset(
        resolveLottiePath(lottieRoot, mapping.lottiePrefix, level, "play+eat"),
        path.join(assetDir, pet.species, `level${level}_play_eat.json`),
      );
    }
  }
}

async function writePngBackedSvg(sourcePng, targetSvg, width, height) {
  const image = await readFile(sourcePng);
  const base64 = image.toString("base64");
  const svg = `<svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" fill="none" xmlns="http://www.w3.org/2000/svg">
<image width="${width}" height="${height}" href="data:image/png;base64,${base64}"/>
</svg>
`;
  await mkdir(path.dirname(targetSvg), { recursive: true });
  await writeFile(targetSvg, svg);
  console.log(`EMBED ${sourcePng} -> ${targetSvg}`);
}

async function preferredImageFromSet(imageSetPath) {
  const contentsPath = path.join(imageSetPath, "Contents.json");
  const contents = JSON.parse(await readFile(contentsPath, "utf8"));
  const images = contents.images ?? [];
  const preferred =
    images.find((image) => image.scale === "3x" && image.filename) ??
    images.find((image) => image.scale === "2x" && image.filename) ??
    images.find((image) => image.filename);
  if (!preferred?.filename) {
    throw new Error(`No image filename found in ${contentsPath}`);
  }
  return path.join(imageSetPath, preferred.filename);
}

function resolveLottiePath(lottieRoot, prefix, level, mode) {
  const candidates = [
    path.join(lottieRoot, `${prefix}_level${level}_${mode}.json`),
  ];
  if (prefix === "mole" && level === 5 && mode === "default") {
    candidates.push(path.join(lottieRoot, "mole_level5_dafault.json"));
  }
  const found = candidates.find((candidate) => existsSync(candidate));
  if (!found) {
    throw new Error(`Missing Lottie file. Tried: ${candidates.join(", ")}`);
  }
  return found;
}

async function copyAsset(source, target) {
  await mkdir(path.dirname(target), { recursive: true });
  await copyFile(source, target);
  console.log(`COPY ${source} -> ${target}`);
}

async function upload(files, bucket, dryRun, wranglerBin, local) {
  const localRows = await checkLocalFiles(files);
  const missing = localRows.filter((row) => !row.exists);
  if (missing.length > 0) {
    console.error(`Missing local files: ${missing.length}/${files.length}`);
    for (const row of missing) {
      console.error(`MISSING ${row.localPath}`);
    }
    process.exitCode = 1;
    return;
  }

  if (!dryRun && !local) await assertRemoteKeysAbsent(files, bucket, wranglerBin);

  for (const file of files) {
    const target = `${bucket}/${file.key}`;
    const { command, prefix } = wranglerInvocation(wranglerBin);
    const args = [
      ...prefix,
      "r2",
      "object",
      "put",
      target,
      local ? "--local" : "--remote",
      "--file",
      file.localPath,
      "--content-type",
      file.contentType,
    ];
    console.log(`${dryRun ? "DRY-RUN" : "UPLOAD"} ${file.localPath} -> r2://${target}`);
    if (!dryRun) {
      await run(command, args);
    }
  }
}

function printManifest(files) {
  for (const file of files) {
    console.log(`${file.key}\t${file.contentType}\t${file.localPath}`);
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (!args.command || args.command === "help" || args.command === "--help") {
    console.log(USAGE.trim());
    return;
  }

  const repoRoot = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
  const assetDir = args.dir ? path.resolve(repoRoot, args.dir) : "";

  if (args.command === "manifest") {
    const files = buildManifest(assetDir, validateRelease(args.release));
    printManifest(files);
    return;
  }

  if (args.command === "assemble-ios") {
    const files = buildManifest(assetDir);
    if (!assetDir) throw new Error("assemble-ios requires --dir <assetDir>");
    if (!args.iosRoot) throw new Error("assemble-ios requires --ios-root <ddan-ddan-ios-svg/DDanDDan>");
    await assembleFromIos(assetDir, path.resolve(repoRoot, args.iosRoot));
    const rows = await checkLocalFiles(files);
    const missing = rows.filter((row) => !row.exists);
    console.log(`SUMMARY ok=${rows.length - missing.length} missing=${missing.length} total=${rows.length}`);
    process.exitCode = missing.length === 0 ? 0 : 1;
    return;
  }

  if (args.command === "local-check") {
    const files = buildManifest(assetDir);
    if (!assetDir) throw new Error("local-check requires --dir <assetDir>");
    const rows = await checkLocalFiles(files);
    const missing = rows.filter((row) => !row.exists);
    for (const row of rows) {
      console.log(`${row.exists ? "OK" : "MISSING"} ${row.localPath}${row.exists ? ` ${row.size}B` : ""}`);
    }
    console.log(`SUMMARY ok=${rows.length - missing.length} missing=${missing.length} total=${rows.length}`);
    process.exitCode = missing.length === 0 ? 0 : 1;
    return;
  }

  if (args.command === "check") {
    const files = buildManifest(assetDir, validateRelease(args.release));
    const cdnBase = args.cdnBase ?? CDN_BASE;
    let ok = 0;
    let missing = 0;
    let other = 0;
    for (const file of files) {
      const url = `${cdnBase.replace(/\/$/, "")}/${file.key}`;
      const result = await head(url);
      if (result.status === 200) ok += 1;
      else if (result.status === 404) missing += 1;
      else other += 1;
      console.log(
        `${result.status} ${url}${result.contentType ? ` ${result.contentType}` : ""}${
          result.contentLength ? ` ${result.contentLength}B` : ""
        }${result.error ? ` ${result.error}` : ""}`,
      );
    }
    console.log(`SUMMARY ok=${ok} missing=${missing} other=${other} total=${files.length}`);
    process.exitCode = missing === 0 && other === 0 ? 0 : 1;
    return;
  }

  if (args.command === "upload") {
    const files = buildManifest(assetDir, validateRelease(args.release));
    if (!assetDir) throw new Error("upload requires --dir <assetDir>");
    if (!args.bucket) throw new Error("upload requires --bucket <r2Bucket>");
    await upload(
      files,
      args.bucket,
      Boolean(args.dryRun),
      args.wranglerBin ?? process.env.WRANGLER_BIN,
      Boolean(args.local),
    );
    return;
  }

  throw new Error(`Unknown command: ${args.command}`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(error.message);
    console.error(USAGE.trim());
    process.exitCode = 1;
  });
}
