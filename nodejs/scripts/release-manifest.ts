/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { basename, dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { globSync } from "glob";
import * as semver from "semver";
import { x as extractTar } from "tar";
import {
    packageIntegrity,
    SDK_PACKAGE_NAMES,
    verifyPackageSetManifestFiles,
} from "./package-set-manifest.js";
import { getRuntimePackageName, RUNTIME_PLATFORMS } from "../src/runtimeArtifacts.js";

export interface ReleaseManifestPackage {
    filename: string;
    integrity: string;
    name: string;
    size: number;
}

export interface PackageSetManifest {
    packages: ReleaseManifestPackage[];
    schemaVersion: 1;
    sdk: {
        version: string;
    };
}

const expectedPackageNames = new Set(SDK_PACKAGE_NAMES);
assert.deepEqual(
    [...expectedPackageNames].sort(),
    ["@github/copilot-sdk", ...RUNTIME_PLATFORMS.map(getRuntimePackageName)].sort(),
    "Shared package manifest names must match the supported runtime platforms"
);

async function readPackedManifest(archive: string): Promise<{ name: string; version: string }> {
    const root = mkdtempSync(join(dirname(archive), ".copilot-sdk-release-manifest-"));
    try {
        await extractTar({
            cwd: root,
            file: archive,
            strict: true,
            filter: (entryPath) => entryPath === "package/package.json",
        });
        return JSON.parse(readFileSync(join(root, "package", "package.json"), "utf8")) as {
            name: string;
            version: string;
        };
    } finally {
        rmSync(root, { recursive: true, force: true });
    }
}

export async function createPackageSetManifest(
    packageDirectory: string,
    sdkVersion: string
): Promise<PackageSetManifest> {
    assert.equal(semver.valid(sdkVersion), sdkVersion, "Invalid SDK version");
    const packages: ReleaseManifestPackage[] = [];
    for (const archive of globSync("github-copilot-sdk-*.tgz", {
        cwd: packageDirectory,
        absolute: true,
    })) {
        const packed = await readPackedManifest(archive);
        if (packed.version !== sdkVersion || !expectedPackageNames.has(packed.name)) {
            continue;
        }
        const bytes = readFileSync(archive);
        packages.push({
            filename: basename(archive),
            integrity: packageIntegrity(bytes),
            name: packed.name,
            size: bytes.length,
        });
    }
    packages.sort((left, right) => left.name.localeCompare(right.name));
    assert.deepEqual(
        packages.map(({ name }) => name),
        [...expectedPackageNames].sort(),
        "Release artifact must contain exactly the nine expected Node packages"
    );
    return {
        schemaVersion: 1,
        sdk: {
            version: sdkVersion,
        },
        packages,
    };
}

export function verifyPackageSetManifest(
    manifest: PackageSetManifest,
    packageDirectory: string
): void {
    verifyPackageSetManifestFiles(manifest, packageDirectory);
    assert(semver.valid(manifest.sdk.version), "Invalid SDK version");
}

function requiredEnvironment(name: string): string {
    const value = process.env[name]?.trim();
    if (!value) {
        throw new Error(`${name} is required.`);
    }
    return value;
}

async function main(): Promise<void> {
    const [command, manifestPath = "release-manifest.json", packageDirectory = "."] =
        process.argv.slice(2);
    if (command === "create-package-set") {
        const manifest = await createPackageSetManifest(
            packageDirectory,
            requiredEnvironment("SDK_VERSION")
        );
        writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
        verifyPackageSetManifest(manifest, packageDirectory);
        return;
    }
    if (command === "verify-package-set") {
        const manifest = JSON.parse(readFileSync(manifestPath, "utf8")) as PackageSetManifest;
        verifyPackageSetManifest(manifest, packageDirectory);
        return;
    }
    throw new Error(
        "Usage: release-manifest.ts create-package-set|verify-package-set [manifest-path] [package-directory]"
    );
}

const scriptPath = process.argv[1]
    ? fileURLToPath(import.meta.url) === resolve(process.argv[1])
    : false;
if (scriptPath) {
    main().catch((error) => {
        console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    });
}
