import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { c as createTar } from "tar";
import { afterEach, describe, expect, it } from "vitest";
import {
    createPackageSetManifest,
    createReleaseManifest,
    verifyPackageSetManifest,
    verifyReleaseManifest,
} from "../scripts/release-manifest.js";
import { getRuntimePackageName, RUNTIME_PLATFORMS } from "../src/runtimeArtifacts.js";

const roots: string[] = [];
const sdkSha = "abcdef0123456789abcdef0123456789abcdef01";
const runtimeSha = "123456789abcdef0123456789abcdef012345678";

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
        const root = mkdtempSync(join(tmpdir(), "copilot-sdk-package-set-manifest-"));
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

    it("freezes and verifies the exact nine-package release identity", async () => {
        const root = mkdtempSync(join(tmpdir(), "copilot-sdk-manifest-"));
        roots.push(root);
        const version = "1.0.13-unstable.34640000001.gabcdef0";
        for (const name of [
            "@github/copilot-sdk",
            ...RUNTIME_PLATFORMS.map(getRuntimePackageName),
        ]) {
            await packageTarball(root, name, version);
        }
        const manifest = await createReleaseManifest(root, {
            channel: "unstable",
            createdAt: "2026-09-04T00:00:00Z",
            runtimeRunId: "9001",
            runtimeSha,
            runtimeVersion: "1.0.83-5.unstable.123.g1234567+build.42",
            sdkRef: "feature/unstable",
            sdkSha,
            sdkVersion: version,
            workflowRunId: "812300",
            workflowRunNumber: "8123",
        });

        expect(manifest.packages).toHaveLength(9);
        expect(manifest.runtime.runId).toBe("9001");
        expect(manifest.runtime.source).toBe("github-packages");
        expect(() => verifyReleaseManifest(manifest, root)).not.toThrow();

        const mismatched = structuredClone(manifest);
        mismatched.runtime.version = "1.0.83-5.canary.123.g1234567.unsigned";
        expect(() => verifyReleaseManifest(mismatched, root)).toThrow(
            "does not belong to the 'unstable' channel"
        );
        await expect(
            createReleaseManifest(root, {
                channel: "canary",
                createdAt: "2026-09-04T00:00:00Z",
                runtimeRunId: "9001",
                runtimeSha,
                runtimeVersion: "1.0.83-5.unstable.123.g1234567",
                sdkRef: "feature/unstable",
                sdkSha,
                sdkVersion: version,
                workflowRunId: "812300",
                workflowRunNumber: "8123",
            })
        ).rejects.toThrow("does not belong to the 'canary' channel");

        const damaged = join(root, manifest.packages[0].filename);
        writeFileSync(damaged, Buffer.concat([readFileSync(damaged), Buffer.from("tampered")]));
        expect(() => verifyReleaseManifest(manifest, root)).toThrow("Size mismatch");
    });

    it("enforces the isolated unstable test workflow identity", async () => {
        const root = mkdtempSync(join(tmpdir(), "copilot-sdk-unstable-test-manifest-"));
        roots.push(root);
        const version = "1.0.13-unstable.812300.gabcdef0.test.812300";
        for (const name of [
            "@github/copilot-sdk",
            ...RUNTIME_PLATFORMS.map(getRuntimePackageName),
        ]) {
            await packageTarball(root, name, version);
        }

        const manifest = await createReleaseManifest(root, {
            channel: "unstable",
            createdAt: "2026-09-04T00:00:00Z",
            runtimeRunId: "9001",
            runtimeSha,
            runtimeVersion: "1.0.83-5.unstable.123.g1234567.test.9001",
            sdkRef: "feature/unstable",
            sdkSha,
            sdkVersion: version,
            workflowRunId: "812300",
            workflowRunNumber: "8123",
            workflowPath: ".github/workflows/sdk-canary.yml",
        });

        expect(manifest.workflow).toMatchObject({
            name: "TEST ONLY - Runtime-driven Node SDK",
            path: ".github/workflows/sdk-canary.yml",
        });
        expect(() =>
            verifyReleaseManifest(manifest, root, ".github/workflows/sdk-canary.yml")
        ).not.toThrow();

        const wrongWorkflow = structuredClone(manifest);
        Reflect.set(wrongWorkflow.workflow, "path", ".github/workflows/runtime-sdk.yml");
        expect(() =>
            verifyReleaseManifest(wrongWorkflow, root, ".github/workflows/sdk-canary.yml")
        ).toThrow("Unexpected test workflow path");

        const wrongNamespace = structuredClone(manifest);
        Reflect.set(wrongNamespace.sdk, "version", "1.0.13-unstable.812300.gabcdef0");
        expect(() =>
            verifyReleaseManifest(wrongNamespace, root, ".github/workflows/sdk-canary.yml")
        ).toThrow("deterministic workflow run namespace");

        const staleRunNumberIdentity = structuredClone(manifest);
        Reflect.set(
            staleRunNumberIdentity.sdk,
            "version",
            "1.0.13-unstable.8123.gabcdef0.test.812300"
        );
        expect(() =>
            verifyReleaseManifest(staleRunNumberIdentity, root, ".github/workflows/sdk-canary.yml")
        ).toThrow("Test unstable SDK version must use the expected workflow run identity");
    });

    it("accepts a matching canary runtime and test release identity", async () => {
        const root = mkdtempSync(join(tmpdir(), "copilot-sdk-canary-manifest-"));
        roots.push(root);
        const version = "1.0.13-canary.8123.gabcdef0.test.812300";
        for (const name of [
            "@github/copilot-sdk",
            ...RUNTIME_PLATFORMS.map(getRuntimePackageName),
        ]) {
            await packageTarball(root, name, version);
        }

        const manifest = await createReleaseManifest(root, {
            channel: "canary",
            createdAt: "2026-09-04T00:00:00Z",
            runtimeRunId: "9001",
            runtimeSha,
            runtimeVersion: "1.0.83-5.canary.123.g1234567.test.9001",
            sdkRef: "feature/canary",
            sdkSha,
            sdkVersion: version,
            workflowRunId: "812300",
            workflowRunNumber: "8123",
            workflowPath: ".github/workflows/sdk-canary.yml",
        });

        expect(() =>
            verifyReleaseManifest(manifest, root, ".github/workflows/sdk-canary.yml")
        ).not.toThrow();
    });
});
