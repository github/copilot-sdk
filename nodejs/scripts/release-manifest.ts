import assert from "node:assert/strict";
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { basename, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { globSync } from "glob";
import * as semver from "semver";
import { x as extractTar } from "tar";
import {
    packageIntegrity,
    SDK_PACKAGE_NAMES,
    verifyPackageSetManifestFiles,
} from "./package-set-manifest.js";
import { validateRuntimeVersionChannel } from "./runtime-release-identity.js";
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

export interface ReleaseManifest extends PackageSetManifest {
    channel: "canary" | "unstable";
    runtime: {
        repository: "github/copilot-agent-runtime";
        runId: string;
        sha: string;
        source: "github-packages";
        version: string;
    };
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
    runtimeRunId: string;
    runtimeVersion: string;
    sdkRef: string;
    sdkSha: string;
    sdkVersion: string;
    workflowRunId: string;
    workflowRunNumber: string;
}

const expectedPackageNames = new Set(SDK_PACKAGE_NAMES);
assert.deepEqual(
    [...expectedPackageNames].sort(),
    ["@github/copilot-sdk", ...RUNTIME_PLATFORMS.map(getRuntimePackageName)].sort(),
    "Shared package manifest names must match the supported runtime platforms"
);

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
    validateRuntimeVersionChannel(metadata.runtimeVersion, metadata.channel);
    assert(Number.isFinite(Date.parse(metadata.createdAt)), "Workflow creation time is invalid");
    const packageSet = await createPackageSetManifest(packageDirectory, metadata.sdkVersion);
    return {
        ...packageSet,
        channel: metadata.channel,
        sdk: {
            ...packageSet.sdk,
            sha: metadata.sdkSha,
            ref: metadata.sdkRef,
            repository: "github/copilot-sdk",
        },
        runtime: {
            version: metadata.runtimeVersion,
            sha: metadata.runtimeSha,
            source: "github-packages",
            repository: "github/copilot-agent-runtime",
            runId: metadata.runtimeRunId,
        },
        workflow: {
            runId: metadata.workflowRunId,
            runNumber: metadata.workflowRunNumber,
            createdAt: metadata.createdAt,
        },
    };
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

export function verifyReleaseManifest(manifest: ReleaseManifest, packageDirectory: string): void {
    verifyPackageSetManifest(manifest, packageDirectory);
    assert(
        manifest.channel === "canary" || manifest.channel === "unstable",
        "Invalid release channel"
    );
    validateFullSha(manifest.sdk.sha, "SDK SHA");
    validateFullSha(manifest.runtime.sha, "Runtime SHA");
    validateRuntimeVersionChannel(manifest.runtime.version, manifest.channel);
    assert.match(manifest.workflow.runId, /^[0-9]+$/, "Invalid SDK workflow run ID");
    assert.match(manifest.workflow.runNumber, /^[0-9]+$/, "Invalid SDK workflow run number");
    assert.match(manifest.runtime.runId, /^[0-9]+$/, "Invalid runtime workflow run ID");
    assert(
        Number.isFinite(Date.parse(manifest.workflow.createdAt)),
        "Invalid workflow creation time"
    );
    assert.equal(manifest.sdk.repository, "github/copilot-sdk");
    assert.equal(manifest.runtime.repository, "github/copilot-agent-runtime");
    assert.equal(manifest.runtime.source, "github-packages", "Invalid runtime package source");
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
    if (command === "create") {
        const manifest = await createReleaseManifest(packageDirectory, {
            channel: requiredEnvironment("RELEASE_CHANNEL") as ReleaseManifest["channel"],
            createdAt: requiredEnvironment("WORKFLOW_CREATED_AT"),
            runtimeSha: requiredEnvironment("RUNTIME_SHA"),
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
    if (command === "verify-package-set") {
        const manifest = JSON.parse(readFileSync(manifestPath, "utf8")) as PackageSetManifest;
        verifyPackageSetManifest(manifest, packageDirectory);
        return;
    }
    throw new Error(
        "Usage: release-manifest.ts create|verify|create-package-set|verify-package-set [manifest-path] [package-directory]"
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
