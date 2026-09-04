import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { mkdtempSync, readFileSync, rmSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { basename, dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { globSync } from "glob";
import * as semver from "semver";
import { x as extractTar } from "tar";
import { getRuntimePackageName, RUNTIME_PLATFORMS } from "../src/runtimeArtifacts.js";

export interface ReleaseManifestPackage {
    filename: string;
    integrity: string;
    name: string;
    size: number;
}

export interface ReleaseManifest {
    channel: "canary" | "unstable";
    packages: ReleaseManifestPackage[];
    runtime: {
        repository: "github/copilot-agent-runtime";
        runId: string;
        sha: string;
        source: "azure" | "github-packages";
        version: string;
    };
    schemaVersion: 1;
    sdk: {
        ref: string;
        repository: "github/copilot-sdk";
        sha: string;
        version: string;
    };
    workflow: {
        createdAt: string;
        runId: string;
        runNumber: string;
    };
}

export interface ReleaseManifestMetadata {
    channel: ReleaseManifest["channel"];
    createdAt: string;
    runtimeSha: string;
    runtimeSource: ReleaseManifest["runtime"]["source"];
    runtimeRunId: string;
    runtimeVersion: string;
    sdkRef: string;
    sdkSha: string;
    sdkVersion: string;
    workflowRunId: string;
    workflowRunNumber: string;
}

const expectedPackageNames = new Set([
    "@github/copilot-sdk",
    ...RUNTIME_PLATFORMS.map(getRuntimePackageName),
]);

function integrity(buffer: Buffer): string {
    return `sha512-${createHash("sha512").update(buffer).digest("base64")}`;
}

async function readPackedManifest(archive: string): Promise<{ name: string; version: string }> {
    const root = mkdtempSync(join(tmpdir(), "copilot-sdk-release-manifest-"));
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

function validateFullSha(value: string, label: string): void {
    assert.match(value, /^[0-9a-f]{40}$/i, `${label} must be a full 40-character SHA`);
}

export async function createReleaseManifest(
    packageDirectory: string,
    metadata: ReleaseManifestMetadata
): Promise<ReleaseManifest> {
    validateFullSha(metadata.sdkSha, "SDK SHA");
    validateFullSha(metadata.runtimeSha, "Runtime SHA");
    assert(Number.isFinite(Date.parse(metadata.createdAt)), "Workflow creation time is invalid");
    const packages: ReleaseManifestPackage[] = [];
    for (const archive of globSync("github-copilot-sdk-*.tgz", {
        cwd: packageDirectory,
        absolute: true,
    })) {
        const packed = await readPackedManifest(archive);
        if (packed.version !== metadata.sdkVersion || !expectedPackageNames.has(packed.name)) {
            continue;
        }
        const bytes = readFileSync(archive);
        packages.push({
            filename: basename(archive),
            integrity: integrity(bytes),
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
        channel: metadata.channel,
        sdk: {
            version: metadata.sdkVersion,
            sha: metadata.sdkSha,
            ref: metadata.sdkRef,
            repository: "github/copilot-sdk",
        },
        runtime: {
            version: metadata.runtimeVersion,
            sha: metadata.runtimeSha,
            source: metadata.runtimeSource,
            repository: "github/copilot-agent-runtime",
            runId: metadata.runtimeRunId,
        },
        workflow: {
            runId: metadata.workflowRunId,
            runNumber: metadata.workflowRunNumber,
            createdAt: metadata.createdAt,
        },
        packages,
    };
}

export function verifyReleaseManifest(manifest: ReleaseManifest, packageDirectory: string): void {
    assert.equal(manifest.schemaVersion, 1, "Unsupported release manifest schema");
    assert(
        manifest.channel === "canary" || manifest.channel === "unstable",
        "Invalid release channel"
    );
    validateFullSha(manifest.sdk.sha, "SDK SHA");
    validateFullSha(manifest.runtime.sha, "Runtime SHA");
    assert(semver.valid(manifest.sdk.version), "Invalid SDK version");
    assert(semver.valid(manifest.runtime.version), "Invalid runtime version");
    assert.match(manifest.workflow.runId, /^[0-9]+$/, "Invalid SDK workflow run ID");
    assert.match(manifest.workflow.runNumber, /^[0-9]+$/, "Invalid SDK workflow run number");
    assert.match(manifest.runtime.runId, /^[0-9]+$/, "Invalid runtime workflow run ID");
    assert(
        Number.isFinite(Date.parse(manifest.workflow.createdAt)),
        "Invalid workflow creation time"
    );
    assert.equal(manifest.sdk.repository, "github/copilot-sdk");
    assert.equal(manifest.runtime.repository, "github/copilot-agent-runtime");
    assert.equal(
        manifest.runtime.source,
        manifest.channel === "canary" ? "azure" : "github-packages",
        "Runtime source does not match the release channel"
    );
    assert.equal(manifest.packages.length, 9, "Release manifest must contain nine packages");
    assert.deepEqual(
        manifest.packages.map(({ name }) => name).sort(),
        [...expectedPackageNames].sort(),
        "Release manifest package names do not match the expected package set"
    );
    for (const packed of manifest.packages) {
        const archive = resolve(packageDirectory, packed.filename);
        assert.equal(
            dirname(archive),
            resolve(packageDirectory),
            `Unsafe release filename: ${packed.filename}`
        );
        const bytes = readFileSync(archive);
        assert.equal(statSync(archive).size, packed.size, `Size mismatch for ${packed.filename}`);
        assert.equal(
            integrity(bytes),
            packed.integrity,
            `Integrity mismatch for ${packed.filename}`
        );
    }
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
    if (command === "create") {
        const manifest = await createReleaseManifest(packageDirectory, {
            channel: requiredEnvironment("RELEASE_CHANNEL") as ReleaseManifest["channel"],
            createdAt: requiredEnvironment("WORKFLOW_CREATED_AT"),
            runtimeSha: requiredEnvironment("RUNTIME_SHA"),
            runtimeSource: requiredEnvironment(
                "RUNTIME_SOURCE"
            ) as ReleaseManifest["runtime"]["source"],
            runtimeRunId: requiredEnvironment("RUNTIME_RUN_ID"),
            runtimeVersion: requiredEnvironment("RUNTIME_VERSION"),
            sdkRef: requiredEnvironment("SDK_REF"),
            sdkSha: requiredEnvironment("SDK_SHA"),
            sdkVersion: requiredEnvironment("SDK_VERSION"),
            workflowRunId: requiredEnvironment("WORKFLOW_RUN_ID"),
            workflowRunNumber: requiredEnvironment("WORKFLOW_RUN_NUMBER"),
        });
        writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
        verifyReleaseManifest(manifest, packageDirectory);
        return;
    }
    if (command === "verify") {
        const manifest = JSON.parse(readFileSync(manifestPath, "utf8")) as ReleaseManifest;
        verifyReleaseManifest(manifest, packageDirectory);
        return;
    }
    throw new Error("Usage: release-manifest.ts create|verify [manifest-path] [package-directory]");
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
