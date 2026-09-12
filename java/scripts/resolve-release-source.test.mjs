/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { execFileSync, spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

import { resolveReleaseSource } from "./resolve-release-source.mjs";

test("release sources must be commits from main's history", async (t) => {
  const parent = fileURLToPath(
    new URL("../target/release-source-tests/", import.meta.url),
  );
  fs.mkdirSync(parent, { recursive: true });
  const repositoryPath = fs.mkdtempSync(path.join(parent, "repository-"));
  t.after(() =>
    fs.rmSync(repositoryPath, { recursive: true, force: true, maxRetries: 3 }),
  );
  const git = (...args) =>
    execFileSync("git", args, {
      cwd: repositoryPath,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    }).trim();

  git("init", "--initial-branch=main");
  git("config", "user.name", "Release source fixture");
  git("config", "user.email", "fixture@example.invalid");
  git("config", "commit.gpgSign", "false");
  git("config", "tag.gpgSign", "false");
  const trailer =
    "Co-authored-by: Copilot App <223556219+Copilot@users.noreply.github.com>";
  git(
    "commit",
    "--allow-empty",
    "-m",
    "Previous release source",
    "-m",
    trailer,
  );
  const previous = git("rev-parse", "HEAD");
  git("commit", "--allow-empty", "-m", "Current main source", "-m", trailer);
  const current = git("rev-parse", "HEAD");
  git("update-ref", "refs/remotes/origin/main", current);
  git("switch", "--create", "unmerged", previous);
  git("commit", "--allow-empty", "-m", "Unmerged source", "-m", trailer);
  const unmerged = git("rev-parse", "HEAD");
  git("switch", "main");
  git(
    "tag",
    "--annotate",
    "release-fixture",
    previous,
    "-m",
    "Annotated fixture",
  );
  const annotatedTag = git("rev-parse", "refs/tags/release-fixture");

  const resolve = (sourceSha, workflowRef = "refs/heads/main") =>
    resolveReleaseSource({ sourceSha, workflowRef, repositoryPath });

  await t.test("accepts the current main commit", () => {
    assert.equal(resolve(current), current);
  });
  await t.test(
    "preserves an earlier main commit for an independent retry",
    () => {
      assert.equal(resolve(previous), previous);
      assert.equal(git("rev-parse", "HEAD"), current);
    },
  );
  await t.test("normalizes a full uppercase SHA", () => {
    assert.equal(resolve(previous.toUpperCase()), previous);
  });
  await t.test(
    "rejects an unmerged commit even when it is present locally",
    () => {
      assert.throws(() => resolve(unmerged), /not in main's history/);
    },
  );
  await t.test("rejects a dispatch from another branch", () => {
    assert.throws(
      () => resolve(previous, "refs/heads/unmerged"),
      /dispatched from main/,
    );
  });
  await t.test("rejects tag object IDs", () => {
    assert.throws(
      () => resolve(annotatedTag),
      /must identify a commit, not a tag/,
    );
  });
  await t.test("rejects tree object IDs", () => {
    assert.throws(() => resolve(git("rev-parse", "HEAD^{tree}")), /not a tree/);
  });
  await t.test("rejects an unknown commit", () => {
    assert.throws(() => resolve("f".repeat(40)));
  });
  for (const source of [
    "main",
    "HEAD",
    "release-fixture",
    previous.slice(0, 8),
    `${previous}\n`,
    `${previous}; echo unsafe`,
    "g".repeat(40),
    "",
  ]) {
    await t.test(`rejects non-SHA input ${JSON.stringify(source)}`, () => {
      assert.throws(() => resolve(source), /full 40-character commit SHA/);
    });
  }

  const script = fileURLToPath(
    new URL("./resolve-release-source.mjs", import.meta.url),
  );
  const run = (sourceSha) =>
    spawnSync(process.execPath, [script], {
      cwd: repositoryPath,
      encoding: "utf8",
      env: {
        ...process.env,
        REQUESTED_SOURCE: sourceSha,
        WORKFLOW_REF: "refs/heads/main",
      },
    });
  await t.test("emits only a validated commit through the CLI", () => {
    const result = run(previous);
    assert.equal(result.status, 0, result.stderr);
    assert.equal(result.stdout.trim(), previous);
  });
  await t.test(
    "fails without emitting a checkout target for an unmerged commit",
    () => {
      const result = run(unmerged);
      assert.equal(result.status, 1);
      assert.equal(result.stdout, "");
      assert.match(result.stderr, /not in main's history/);
      assert.equal(git("rev-parse", "HEAD"), current);
    },
  );
  await t.test(
    "fails closed if the trusted main reference is unavailable",
    () => {
      git("update-ref", "-d", "refs/remotes/origin/main");
      assert.throws(
        () => resolve(previous),
        /Could not validate main's history/,
      );
    },
  );
});
