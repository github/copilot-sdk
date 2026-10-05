/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { GENERATED_ROOTS, matchesGeneratedPath, SCHEMA_FILES } from "../build-prerequisites.mjs";
import { findRuntimeRoot } from "../runtime-layout.mjs";

export const PROTOCOL_FILES = [
    "nodejs/src/sdkProtocolVersion.ts",
    "python/copilot/_sdk_protocol_version.py",
    "go/sdk_protocol_version.go",
    "dotnet/src/SdkProtocolVersion.cs",
    "java/sdk/src/main/java/com/github/copilot/SdkProtocolVersion.java",
    "rust/src/sdk_protocol_version.rs",
];

// Keep these aligned with projection srcs in src/sdk/BUILD.bazel. Go and .NET
// generators also scan handwritten declarations to avoid duplicate types.
const SDK_INPUTS = [
    /^scripts\/codegen\//,
    /^java\/scripts\/codegen\//,
    /^scripts\/(?:build-prerequisites|run-tasks|runtime-layout)\.mjs$/,
    /^scripts\/ci\/check-generated\.mjs$/,
    /^(?:BUILD\.bazel|package\.json|\.editorconfig)$/,
    /^nodejs\/(?:scripts\/releaseArtifacts\.ts|src\/cliVersion\.ts|package(?:-lock)?\.json)$/,
    /^go\/[^/]+\.go$/,
    /^dotnet\/(?:global\.json|src\/GitHub\.Copilot\.SDK\.csproj)$/,
    /^dotnet\/src\/(?!(?:.*\/)?(?:Generated|bin|obj)\/).*\.cs$/,
    /^rust\/(?:\.rustfmt(?:\.nightly)?\.toml|rust-toolchain\.toml)$/,
];
const RUNTIME_INPUTS = [
    /^\.github\/workflows\/sdk(?:-java)?\.yml$/,
    /^\.github\/actions\/(?:setup-node|setup-bazel)\//,
    /^(?:\.nvmrc|rust-toolchain\.toml|MODULE\.bazel(?:\.lock)?|\.bazelrc|\.bazelversion)$/,
    /^(?:script\/bazel\.ts|tools\/bazel\/|bazel\/)/,
];

function git(root, args) {
    return execFileSync("git", args, { cwd: root, encoding: "utf8", windowsHide: true });
}

export function generationBaseline(root, eventName, event = {}) {
    let baseline;
    switch (eventName) {
        case "pull_request":
            return git(root, ["rev-parse", "HEAD^1"]).trim();
        case "merge_group":
            baseline = event.merge_group?.base_sha;
            break;
        case "push":
            baseline = event.before;
            if (typeof baseline === "string" && /^0+$/.test(baseline)) return undefined;
            break;
        case "workflow_dispatch":
            return undefined;
        default:
            throw new Error(`Unsupported SDK generation event: ${eventName}`);
    }
    if (!baseline) {
        throw new Error(`Missing SDK generation baseline for ${eventName}`);
    }
    return git(root, ["rev-parse", "--verify", `${baseline}^{commit}`]).trim();
}

export function requiresSdkGeneration(changedPaths) {
    return changedPaths.some((file) => {
        if (SCHEMA_FILES.some((name) => file === `generated/${name}`)) return true;
        if (RUNTIME_INPUTS.some((pattern) => pattern.test(file))) return true;
        if (!file.startsWith("src/sdk/")) return false;
        const sdkPath = file.slice("src/sdk/".length);
        return matchesGeneratedPath(sdkPath) || SDK_INPUTS.some((pattern) => pattern.test(sdkPath));
    });
}

export function assertGeneratedClean(root, paths, remedy) {
    const changes = git(root, ["status", "--porcelain", "--untracked-files=all", "--", ...paths]);
    if (changes.trim()) {
        throw new Error(`Generated files are out of date.

From the runtime repository root, run:
  ${remedy}

Commit all resulting generated changes, including added/deleted files,
then push the updated branch. Do not hand-edit generated files.

Changed files:
${changes.trimEnd()}`);
    }
}

export function checkGenerated({ root, baseline, generateSchemas, generateSdk, generateProtocol }) {
    generateSchemas();
    assertGeneratedClean(
        root,
        SCHEMA_FILES.map((name) => `generated/${name}`),
        "pnpm run generate:sdk",
    );

    const changedPaths =
        baseline === undefined
            ? undefined
            : git(root, ["diff", "--name-only", "--no-renames", "-z", baseline, "HEAD"]).split("\0").filter(Boolean);
    const required = changedPaths === undefined || requiresSdkGeneration(changedPaths);
    if (required) {
        generateSdk();
        const outputs = Object.values(GENERATED_ROOTS)
            .flat()
            .map((output) => (output.includes("*") ? `:(glob)src/sdk/${output}` : `src/sdk/${output}`));
        assertGeneratedClean(root, outputs, "pnpm run generate:sdk");
    } else {
        console.log("Public schemas and SDK generation inputs/outputs are unchanged; skipping SDK projections.");
    }

    generateProtocol();
    assertGeneratedClean(
        root,
        PROTOCOL_FILES.map((file) => `src/sdk/${file}`),
        "npm --prefix src/sdk/nodejs run update:protocol-version",
    );
    return required;
}

function main() {
    if (process.argv.includes("--help")) {
        console.log(`Usage: node src/sdk/scripts/ci/check-generated.mjs

Checks public schema freshness before conditionally checking all six SDK projections.
Always checks all six protocol constants. Leaves regenerated outputs for inspection.
GITHUB_EVENT_NAME and GITHUB_EVENT_PATH select the CI baseline; without them,
all projections are checked. This command never commits or pushes.`);
        return;
    }
    if (process.argv.length !== 2) throw new Error("Unexpected arguments; use --help");
    const sdkRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
    const root = findRuntimeRoot(sdkRoot);
    if (!root) throw new Error("Schema freshness validation requires a runtime checkout");
    const event = process.env.GITHUB_EVENT_PATH
        ? JSON.parse(fs.readFileSync(process.env.GITHUB_EVENT_PATH, "utf8"))
        : {};
    const baseline = generationBaseline(root, process.env.GITHUB_EVENT_NAME ?? "workflow_dispatch", event);
    const run = (args) => execFileSync(process.execPath, args, { cwd: root, stdio: "inherit", windowsHide: true });
    checkGenerated({
        root,
        baseline,
        generateSchemas: () =>
            run(["src/sdk/scripts/run-tasks.mjs", "generate:schemas", "--runtime-source", "checkout"]),
        generateSdk: () => run(["src/sdk/scripts/run-tasks.mjs", "generate", "--runtime-source", "checkout"]),
        generateProtocol: () => run(["src/sdk/nodejs/scripts/update-protocol-version.ts"]),
    });
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try {
        main();
    } catch (error) {
        console.error(error instanceof Error ? error.message : error);
        process.exitCode = 1;
    }
}
