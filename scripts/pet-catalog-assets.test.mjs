import assert from "node:assert/strict";
import test from "node:test";

import { assertRemoteKeysAbsent, isR2ObjectNotFound } from "./pet-catalog-assets.mjs";

const files = [
  { key: "releases/v1/cat/level1.svg" },
  { key: "releases/v1/cat/level1_default.json" },
];

test("R2의 명확한 missing 응답만 새 release 업로드를 허용한다", async () => {
  const calls = [];
  await assertRemoteKeysAbsent(files, "bucket", "/fake/wrangler", async (command, args) => {
    calls.push({ command, args });
    return { code: 1, output: "The specified key does not exist." };
  });

  assert.equal(calls.length, files.length);
  assert.equal(calls[0].command, "/fake/wrangler");
  assert.deepEqual(calls[0].args.slice(0, 5), ["r2", "object", "get", "bucket/releases/v1/cat/level1.svg", "--remote"]);
});

test("R2 key가 존재하면 release 전체 업로드를 거부한다", async () => {
  await assert.rejects(
    assertRemoteKeysAbsent(files, "bucket", "/fake/wrangler", async () => ({ code: 0, output: "" })),
    /Immutable R2 key already exists/,
  );
});

test("인증이나 네트워크 오류는 missing으로 간주하지 않고 fail-closed 처리한다", async () => {
  for (const output of [
    "Authentication failed",
    "Authentication token not found",
    "Account not found (404)",
    "Bucket not found",
    "Endpoint returned 404 Not Found",
  ]) {
    await assert.rejects(
      assertRemoteKeysAbsent(files, "bucket", "/fake/wrangler", async () => ({ code: 1, output })),
      /Could not authoritatively verify R2 key absence/,
    );
  }
});

test("R2 조회 runner 자체 실패도 fail-closed 처리한다", async () => {
  await assert.rejects(
    assertRemoteKeysAbsent(files, "bucket", "/fake/wrangler", async () => {
      throw new Error("network unavailable");
    }),
    /network unavailable/,
  );
});

test("missing 판별은 알려진 not-found 응답만 허용한다", () => {
  assert.equal(isR2ObjectNotFound("The specified key does not exist."), true);
  assert.equal(isR2ObjectNotFound("The specified object does not exist"), true);
  assert.equal(isR2ObjectNotFound("404 Not Found"), false);
  assert.equal(isR2ObjectNotFound("Account not found (404)"), false);
  assert.equal(isR2ObjectNotFound("Authentication failed"), false);
});
