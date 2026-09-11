import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFileSync, statSync } from "node:fs";
import { basename, dirname, resolve } from "node:path";

export const SDK_PACKAGE_NAMES = [
    "@github/copilot-sdk",
    "@github/copilot-sdk-darwin-arm64",
    "@github/copilot-sdk-darwin-x64",
    "@github/copilot-sdk-linux-arm64",
    "@github/copilot-sdk-linux-x64",
    "@github/copilot-sdk-linuxmusl-arm64",
    "@github/copilot-sdk-linuxmusl-x64",
    "@github/copilot-sdk-win32-arm64",
    "@github/copilot-sdk-win32-x64",
];

export function packageIntegrity(bytes) {
    return `sha512-${createHash("sha512").update(bytes).digest("base64")}`;
}

export function verifyPackageSetManifestFiles(manifest, packageDirectory) {
    assert.equal(manifest?.schemaVersion, 1, "Unsupported release manifest schema");
    assert.equal(typeof manifest.sdk?.version, "string", "Invalid SDK version");
    assert(Array.isArray(manifest.packages), "Release manifest packages must be an array");
    assert.equal(manifest.packages.length, 9, "Release manifest must contain nine packages");

    const packageNames = new Set();
    for (const packed of manifest.packages) {
        assert.equal(typeof packed?.name, "string", "Invalid release package name");
        assert.equal(typeof packed.filename, "string", "Invalid release package filename");
        assert.equal(typeof packed.integrity, "string", "Invalid release package integrity");
        assert.equal(typeof packed.size, "number", "Invalid release package size");
        assert(
            !packageNames.has(packed.name),
            `Duplicate package in release manifest: ${packed.name}`
        );
        packageNames.add(packed.name);

        const archive = resolve(packageDirectory, packed.filename);
        assert.equal(
            dirname(archive),
            resolve(packageDirectory),
            `Unsafe release filename: ${packed.filename}`
        );
        assert.equal(
            basename(archive),
            packed.filename,
            `Unsafe release filename: ${packed.filename}`
        );
        const bytes = readFileSync(archive);
        assert.equal(statSync(archive).size, packed.size, `Size mismatch for ${packed.filename}`);
        assert.equal(
            packageIntegrity(bytes),
            packed.integrity,
            `Integrity mismatch for ${packed.filename}`
        );
    }

    assert.deepEqual(
        [...packageNames].sort(),
        [...SDK_PACKAGE_NAMES].sort(),
        "Release manifest package names do not match the expected package set"
    );
}
