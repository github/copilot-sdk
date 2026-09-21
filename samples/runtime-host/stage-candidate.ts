/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { createReadStream } from "node:fs";
import {
  copyFile,
  mkdir,
  readFile,
  realpath,
  rm,
  writeFile,
} from "node:fs/promises";
import { createRequire } from "node:module";
import { dirname, isAbsolute, join, relative, resolve, sep } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import {
  getRuntimePlatform,
  materializeRuntimeBundle,
} from "../../nodejs/src/runtimeArtifacts.js";

const [runtimeArgument, hostArgument, outputArgument] = process.argv.slice(2);
assert(
  runtimeArgument && hostArgument && outputArgument,
  "Usage: tsx samples/runtime-host/stage-candidate.ts RUNTIME_CHECKOUT HOST_CHECKOUT OUTPUT_DIRECTORY",
);
assert.equal(
  process.platform,
  "linux",
  "This integration candidate currently targets Linux",
);
const sdk = await realpath(fileURLToPath(new URL("../../", import.meta.url)));
const runtime = await realpath(resolve(runtimeArgument));
const host = await realpath(resolve(hostArgument));
const output = resolve(outputArgument);
const platform = getRuntimePlatform();
const require = createRequire(join(sdk, "nodejs/package.json"));
const { extract } = require("tar");

function git(checkout: string, ...args: string[]) {
  return execFileSync("git", ["-C", checkout, ...args], {
    encoding: "utf8",
  }).trim();
}

const sources = Object.fromEntries(
  Object.entries({ runtime, host, sdk }).map(([name, checkout]) => {
    assert.equal(
      git(checkout, "status", "--porcelain", "--untracked-files=no"),
      "",
      `Commit tracked changes in ${name} before attesting a source revision`,
    );
    return [
      name,
      {
        repository: `github/copilot-${name === "runtime" ? "agent-runtime" : name}`,
        checkout,
        commit: git(checkout, "rev-parse", "HEAD"),
      },
    ];
  }),
);

const sdkOverride = `patch."https://github.com/github/copilot-sdk".github-copilot-sdk.path=${JSON.stringify(join(sdk, "rust"))}`;
// Resolve in a committed-source snapshot: Cargo's local patch may rewrite its
// lockfile, which must never dirty the original pinned host checkout.
await mkdir(output);
const hostSnapshot = join(output, ".host-source");
const hostArchive = join(output, ".host-source.tar");
await mkdir(hostSnapshot);
git(host, "archive", "--format=tar", `--output=${hostArchive}`, "HEAD");
await extract({ file: hostArchive, cwd: hostSnapshot, strict: true });
const cargo = JSON.parse(
  execFileSync(
    "cargo",
    ["metadata", "--offline", "--format-version", "1", "--config", sdkOverride],
    {
      cwd: hostSnapshot,
      encoding: "utf8",
      maxBuffer: 20 * 1024 * 1024,
    },
  ),
);
const sdkDependency = cargo.packages.find(
  (entry: { name: string }) => entry.name === "github-copilot-sdk",
);
assert(sdkDependency, "Host Cargo graph must contain the Rust SDK");
assert.equal(
  await realpath(sdkDependency.manifest_path),
  join(sdk, "rust/Cargo.toml"),
);
assert.equal(
  sdkDependency.source,
  null,
  "Host must resolve the checkout, not a released/git SDK",
);
await rm(hostSnapshot, { recursive: true });
await rm(hostArchive);

async function digest(path: string) {
  const hash = createHash("sha256");
  for await (const chunk of createReadStream(path)) hash.update(chunk);
  return hash.digest("hex");
}

const artifacts: Record<
  string,
  { path: string; sourcePath: string; sha256: string }
> = {};
for (const [name, variable, checkout, filename] of [
  ["runtime", "COPILOT_CLI_PATH", runtime, "copilot-runtime"],
  ["provider", "COPILOT_RUNTIME_PROVIDER_LIB", runtime, "runtime.node"],
  ["lite", "COPILOTD_LITE_PATH", host, "copilotd-lite"],
]) {
  const value = process.env[variable];
  assert(
    value && isAbsolute(value),
    `${variable} must select an absolute local build output`,
  );
  const sourcePath = await realpath(value);
  const suffix = relative(checkout, sourcePath);
  assert(
    suffix &&
      !isAbsolute(suffix) &&
      suffix !== ".." &&
      !suffix.startsWith(`..${sep}`),
  );
  assert(
    !sourcePath.includes(`${sep}node_modules${sep}`),
    `${variable} cannot select a release`,
  );
  artifacts[name] = {
    path: join("prebuilds", platform, filename),
    sourcePath,
    sha256: await digest(sourcePath),
  };
}

const assembly = join(output, "assembly");
const packed = join(output, "packed");
await mkdir(packed);
const { createRuntimePackageMetadata, verifyExtractedRuntimePackage } =
  await import(
    pathToFileURL(join(runtime, "script/runtime-platform-package.mjs")).href
  );
const { installCopilotdLite } = await import(
  pathToFileURL(join(runtime, "script/install-copilotd-lite.ts")).href
);
// Use the same asset selector as Node platform-package assembly. dist-cli must
// already have been built locally; no release acquisition runs in this script.
const wrapper = materializeRuntimeBundle(
  { packageRoot: join(runtime, "dist-cli"), platform },
  assembly,
  platform,
);
const assemblyRoot = resolve(dirname(wrapper), "../..");
for (const name of ["runtime", "provider"]) {
  await copyFile(
    artifacts[name].sourcePath,
    join(assemblyRoot, artifacts[name].path),
  );
}
await installCopilotdLite(dirname(wrapper), process.platform, process.arch, {
  candidate: artifacts.lite.sourcePath,
});
// The release metadata helper requires its normal version grammar. r1 here is
// a local-only placeholder, not a claim of an actual GitHub Actions release.
const version = `0.0.0-canary.r1.g${sources.runtime.commit.slice(0, 7)}.unsigned`;
await writeFile(
  join(assemblyRoot, "package.json"),
  JSON.stringify(
    {
      ...createRuntimePackageMetadata(
        platform,
        version,
        sources.runtime.commit,
      ),
      name: `@github/copilot-sdk-${platform}`,
      private: true,
    },
    null,
    2,
  ) + "\n",
);
await verifyExtractedRuntimePackage(assemblyRoot, platform);
const packResult = JSON.parse(
  execFileSync(
    "npm",
    [
      "pack",
      assemblyRoot,
      "--ignore-scripts",
      "--json",
      "--pack-destination",
      packed,
    ],
    {
      cwd: sdk,
      encoding: "utf8",
      maxBuffer: 10 * 1024 * 1024,
    },
  ),
);
const tarball = join(packed, packResult[0].filename);
const packageRoot = join(
  output,
  "node_modules",
  "@github",
  `copilot-sdk-${platform}`,
);
await mkdir(packageRoot, { recursive: true });
await extract({ file: tarball, cwd: packageRoot, strip: 1, strict: true });
await verifyExtractedRuntimePackage(packageRoot, platform);
for (const artifact of Object.values(artifacts)) {
  assert.equal(await digest(join(packageRoot, artifact.path)), artifact.sha256);
}
const liteProvenance = JSON.parse(
  await readFile(
    join(packageRoot, "prebuilds", platform, "copilotd-lite.provenance.json"),
    "utf8",
  ),
);
assert.equal(liteProvenance.source, "local-candidate");
assert.equal(liteProvenance.sha256, artifacts.lite.sha256);
const manifestPath = join(output, "candidate.json");
await writeFile(
  manifestPath,
  JSON.stringify(
    {
      schemaVersion: 1,
      kind: "local-runtime-host-candidate",
      packageRoot,
      platform,
      sources,
      artifacts,
      packageArchive: { path: tarball, sha256: await digest(tarball) },
      hostSdk: {
        manifestPath: sdkDependency.manifest_path,
        cargoConfig: sdkOverride,
      },
    },
    null,
    2,
  ) + "\n",
);
await rm(assembly, { recursive: true });
console.log(manifestPath);
