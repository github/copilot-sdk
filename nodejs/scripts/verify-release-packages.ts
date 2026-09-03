import assert from "node:assert/strict";
import { mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { globSync } from "glob";
import { t as listTar, x as extractTar } from "tar";
import { getRuntimePackageName, RUNTIME_PLATFORMS } from "../src/runtimeArtifacts.js";

interface PackedPackage {
    manifest: {
        name: string;
        version: string;
        optionalDependencies?: Record<string, string>;
    };
    entries: Set<string>;
}

const nodeRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
const sourceManifest = JSON.parse(
    readFileSync(join(nodeRoot, "package.json"), "utf8")
) as PackedPackage["manifest"];
const expectedRuntimePackages = Object.fromEntries(
    RUNTIME_PLATFORMS.map((platform) => [getRuntimePackageName(platform), sourceManifest.version])
);
const expectedPackageNames = new Set([
    sourceManifest.name,
    ...Object.keys(expectedRuntimePackages),
]);
const packages = new Map<string, PackedPackage>();

for (const archive of globSync("*.tgz", { cwd: nodeRoot, absolute: true })) {
    const entries = new Set<string>();
    await listTar({
        file: archive,
        onReadEntry(entry) {
            entries.add(entry.path);
            entry.resume();
        },
    });

    const manifestRoot = mkdtempSync(join(tmpdir(), "copilot-sdk-package-manifest-"));
    let manifest: PackedPackage["manifest"];
    try {
        await extractTar({
            cwd: manifestRoot,
            file: archive,
            strict: true,
            filter: (entryPath) => entryPath === "package/package.json",
        });
        manifest = JSON.parse(
            readFileSync(join(manifestRoot, "package", "package.json"), "utf8")
        ) as PackedPackage["manifest"];
    } finally {
        rmSync(manifestRoot, { recursive: true, force: true });
    }

    if (manifest.version !== sourceManifest.version || !expectedPackageNames.has(manifest.name)) {
        continue;
    }
    assert(!packages.has(manifest.name), `Duplicate tarball for ${manifest.name}`);
    packages.set(manifest.name, { manifest, entries });
}

assert.deepEqual(
    [...packages.keys()].sort(),
    [...expectedPackageNames].sort(),
    "Release packaging did not produce the expected main and platform packages"
);

const mainPackage = packages.get(sourceManifest.name);
assert(mainPackage, `Missing ${sourceManifest.name} tarball`);
assert.deepEqual(
    mainPackage.manifest.optionalDependencies,
    expectedRuntimePackages,
    "Main package optional dependencies do not match the platform packages"
);
assert(mainPackage.entries.has("package/dist/index.js"), "Main package is missing dist/index.js");
assert(
    mainPackage.entries.has("package/dist/cjs/index.js"),
    "Main package is missing dist/cjs/index.js"
);

for (const platform of RUNTIME_PLATFORMS) {
    const packageName = getRuntimePackageName(platform);
    const packed = packages.get(packageName);
    assert(packed, `Missing ${packageName} tarball`);
    const runtimeName = platform.startsWith("win32") ? "copilot-runtime.exe" : "copilot-runtime";
    for (const requiredPath of [
        `package/prebuilds/${platform}/${runtimeName}`,
        `package/prebuilds/${platform}/runtime.node`,
        "package/copilot-sdk/extension.js",
        "package/preloads/extension_bootstrap.mjs",
        "package/sdk/index.js",
    ]) {
        assert(packed.entries.has(requiredPath), `${packageName} is missing ${requiredPath}`);
    }
}

console.log(`Verified ${packages.size} release package tarballs.`);
