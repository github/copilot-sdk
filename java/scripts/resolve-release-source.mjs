/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync, spawnSync } from "node:child_process";
import { pathToFileURL } from "node:url";

export function resolveReleaseSource({
  sourceSha,
  workflowRef,
  repositoryPath,
}) {
  if (workflowRef !== "refs/heads/main") {
    throw new Error("Java publication must be dispatched from main.");
  }
  if (
    typeof sourceSha !== "string" ||
    sourceSha.length !== 40 ||
    !/^[0-9a-f]{40}$/i.test(sourceSha)
  ) {
    throw new Error("sourceSha must be a full 40-character commit SHA.");
  }

  const requested = sourceSha.toLowerCase();
  const options = {
    cwd: repositoryPath,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  };
  const type = execFileSync(
    "git",
    ["cat-file", "-t", requested],
    options,
  ).trim();
  if (type !== "commit") {
    throw new Error(`sourceSha must identify a commit, not a ${type}.`);
  }

  const ancestry = spawnSync(
    "git",
    ["merge-base", "--is-ancestor", requested, "refs/remotes/origin/main"],
    options,
  );
  if (ancestry.error) {
    throw ancestry.error;
  }
  if (ancestry.status === 1) {
    throw new Error(`Source ${requested} is not in main's history.`);
  }
  if (ancestry.status !== 0) {
    throw new Error(
      `Could not validate main's history: ${ancestry.stderr || ancestry.signal || ancestry.status}`,
    );
  }

  return execFileSync(
    "git",
    ["rev-parse", "--verify", `${requested}^{commit}`],
    options,
  ).trim();
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  try {
    console.log(
      resolveReleaseSource({
        sourceSha: process.env.REQUESTED_SOURCE,
        workflowRef: process.env.WORKFLOW_REF,
        repositoryPath: process.cwd(),
      }),
    );
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
