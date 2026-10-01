/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { c as createTar } from "tar";
import { afterEach, describe, expect, it } from "vitest";
import { createPackageSetManifest, verifyPackageSetManifest } from "../scripts/release-manifest.js";
import { getRuntimePackageName, RUNTIME_PLATFORMS } from "../src/runtimeArtifacts.js";

const roots: string[] = [];

afterEach(() => {
    for (const root of roots.splice(0)) {
        rmSync(root, { recursive: true, force: true });
    }
});

async function packageTarball(root: string, name: string, version: string): Promise<void> {
    const packageRoot = join(root, "staging", name.replaceAll("/", "-"));
    mkdirSync(join(packageRoot, "package"), { recursive: true });
    writeFileSync(join(packageRoot, "package", "package.json"), JSON.stringify({ name, version }));
    const filename = `${name.replace("@github/", "github-").replaceAll("/", "-")}-${version}.tgz`;
    await createTar({ cwd: packageRoot, file: join(root, filename), gzip: true }, ["package"]);
}

describe("release manifest", () => {
    it("freezes and verifies a direct nine-package release", async () => {
        const root = mkdtempSync(join(process.cwd(), ".sdk-package-set-manifest-"));
        roots.push(root);
        const version = "1.0.13-unstable.34640000001.gabcdef0";
        for (const name of [
            "@github/copilot-sdk",
            ...RUNTIME_PLATFORMS.map(getRuntimePackageName),
        ]) {
            await packageTarball(root, name, version);
        }

        const manifest = await createPackageSetManifest(root, version);
        expect(manifest).toMatchObject({
            schemaVersion: 1,
            sdk: { version },
            packages: expect.arrayContaining([
                expect.objectContaining({ name: "@github/copilot-sdk" }),
            ]),
        });
        expect(manifest.packages).toHaveLength(9);
        expect(() => verifyPackageSetManifest(manifest, root)).not.toThrow();

        const damaged = join(root, manifest.packages[0].filename);
        writeFileSync(damaged, Buffer.concat([readFileSync(damaged), Buffer.from("tampered")]));
        expect(() => verifyPackageSetManifest(manifest, root)).toThrow("Size mismatch");
    });
});
