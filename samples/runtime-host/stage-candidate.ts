/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { createReadStream } from "node:fs";
import { copyFile, mkdir, realpath, rm, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { dirname, isAbsolute, join, relative, resolve, sep } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { getRuntimePlatform, materializeRuntimeBundle } from "../../nodejs/src/runtimeArtifacts.js";
import { localCandidateSources } from "./candidate-sources.js";

const [runtimeArgument, outputArgument] = process.argv.slice(2);
assert(
    runtimeArgument && outputArgument && process.argv.length === 4,
    "Usage: tsx samples/runtime-host/stage-candidate.ts RUNTIME_CHECKOUT OUTPUT_DIRECTORY",
);
assert.equal(process.platform, "linux", "This integration candidate currently targets Linux");
const sdk = await realpath(fileURLToPath(new URL("../../", import.meta.url)));
const runtime = await realpath(resolve(runtimeArgument));
const output = resolve(outputArgument);
const platform = getRuntimePlatform();
const require = createRequire(join(sdk, "nodejs/package.json"));
const { extract } = require("tar");

const sources = localCandidateSources(runtime, sdk);
await mkdir(output);

async function digest(path: string) {
    const hash = createHash("sha256");
    for await (const chunk of createReadStream(path)) hash.update(chunk);
    return hash.digest("hex");
}

const artifacts: Record<string, { path: string; sourcePath: string; sha256: string }> = {};
for (const [name, variable, checkout, filename] of [
    ["runtime", "COPILOT_CLI_PATH", runtime, "copilot-runtime"],
    ["provider", "COPILOT_RUNTIME_PROVIDER_LIB", runtime, "runtime.node"],
]) {
    const value = process.env[variable];
    assert(value && isAbsolute(value), `${variable} must select an absolute local build output`);
    const sourcePath = await realpath(value);
    const suffix = relative(checkout, sourcePath);
    assert(suffix && !isAbsolute(suffix) && suffix !== ".." && !suffix.startsWith(`..${sep}`));
    assert(!sourcePath.includes(`${sep}node_modules${sep}`), `${variable} cannot select a release`);
    artifacts[name] = {
        path: join("prebuilds", platform, filename),
        sourcePath,
        sha256: await digest(sourcePath),
    };
}

const assembly = join(output, "assembly");
const packed = join(output, "packed");
await mkdir(packed);
const { createRuntimePackageMetadata, verifyExtractedRuntimePackage } = await import(
    pathToFileURL(join(runtime, "script/runtime-platform-package.mjs")).href
);
// Use the same asset selector as Node platform-package assembly. dist-cli must
// already have been built locally; no release acquisition runs in this script.
const wrapper = materializeRuntimeBundle({ packageRoot: join(runtime, "dist-cli"), platform }, assembly, platform);
const assemblyRoot = resolve(dirname(wrapper), "../..");
for (const name of ["runtime", "provider"]) {
    await copyFile(artifacts[name].sourcePath, join(assemblyRoot, artifacts[name].path));
}
// The release metadata helper requires its normal version grammar. r1 here is
// a local-only placeholder, not a claim of an actual GitHub Actions release.
const version = `0.0.0-canary.r1.g${sources.runtime.commit.slice(0, 7)}.unsigned`;
await writeFile(
    join(assemblyRoot, "package.json"),
    JSON.stringify(
        {
            ...createRuntimePackageMetadata(platform, version, sources.runtime.commit),
            name: `@github/copilot-sdk-${platform}`,
            private: true,
        },
        null,
        2,
    ) + "\n",
);
await verifyExtractedRuntimePackage(assemblyRoot, platform);
const packResult = JSON.parse(
    execFileSync("npm", ["pack", assemblyRoot, "--ignore-scripts", "--json", "--pack-destination", packed], {
        cwd: sdk,
        encoding: "utf8",
        maxBuffer: 10 * 1024 * 1024,
    }),
);
const tarball = join(packed, packResult[0].filename);
const packageRoot = join(output, "node_modules", "@github", `copilot-sdk-${platform}`);
await mkdir(packageRoot, { recursive: true });
await extract({ file: tarball, cwd: packageRoot, strip: 1, strict: true });
await verifyExtractedRuntimePackage(packageRoot, platform);
for (const artifact of Object.values(artifacts)) {
    assert.equal(await digest(join(packageRoot, artifact.path)), artifact.sha256);
}
const manifestPath = join(output, "candidate.json");
await writeFile(
    manifestPath,
    JSON.stringify(
        {
            schemaVersion: 3,
            kind: "local-runtime-host-candidate",
            packageRoot,
            platform,
            sources,
            artifacts,
            packageArchive: { path: tarball, sha256: await digest(tarball) },
        },
        null,
        2,
    ) + "\n",
);
await rm(assembly, { recursive: true });
console.log(manifestPath);
