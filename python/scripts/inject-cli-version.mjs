#!/usr/bin/env node
/**
 * inject-cli-version.mjs
 *
 * Reads the pinned Copilot CLI version from nodejs/package.json and
 * writes it into python/copilot/_cli_version.py, replacing the `CLI_VERSION = None`
 * sentinel with the concrete version string.
 *
 * Run from the repository root:
 *   node python/scripts/inject-cli-version.mjs
 */

import { readFileSync, writeFileSync } from "fs";
import { join, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, "..", "..");

const packagePath = join(repoRoot, "nodejs", "package.json");
const packageJson = JSON.parse(readFileSync(packagePath, "utf-8"));
const version = packageJson.copilotCliVersion;
if (!version) {
  console.error("Error: Could not find copilotCliVersion in nodejs/package.json");
  process.exit(1);
}
console.log(`Injecting CLI_VERSION = "${version}"`);

// Patch _cli_version.py
const versionFile = join(__dirname, "..", "copilot", "_cli_version.py");
let content = readFileSync(versionFile, "utf-8");

const sentinel = 'CLI_VERSION: str | None = None';
const replacement = `CLI_VERSION: str | None = "${version}"`;

if (!content.includes(sentinel)) {
  // Check if already injected
  if (content.includes(`CLI_VERSION: str | None = "`)) {
    console.log("CLI_VERSION already injected, updating...");
    content = content.replace(/CLI_VERSION: str \| None = ".*?"/, `CLI_VERSION: str | None = "${version}"`);
  } else {
    console.error(`Error: Could not find sentinel '${sentinel}' in _cli_version.py`);
    process.exit(1);
  }
} else {
  content = content.replace(sentinel, replacement);
}

writeFileSync(versionFile, content);
console.log(`Done. _cli_version.py now has CLI_VERSION = "${version}"`);
