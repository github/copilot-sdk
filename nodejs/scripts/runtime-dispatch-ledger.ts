import assert from "node:assert/strict";
import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

export interface RuntimeDispatchMarker {
    canonicalRunId: string;
    channel: "canary" | "unstable";
    createdAt: string;
    mode: "internal" | "tests-only";
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
        versionOverride: string;
        sha: string;
    };
    workflow: ".github/workflows/runtime-sdk.yml";
}

interface ArtifactApiResponse {
    expired: boolean;
    workflow_run?: { id?: number };
}

interface WorkflowRunApiResponse {
    event: string;
    head_branch: string;
    head_sha: string;
    id: number;
    name: string;
    path: string;
    repository: { full_name: string };
}

export interface ExpectedDispatch {
    channel: RuntimeDispatchMarker["channel"];
    currentRunId: string;
    mode: RuntimeDispatchMarker["mode"];
    resumeRunId: string;
    runtimeRunId: string;
    runtimeSha: string;
    runtimeSource: RuntimeDispatchMarker["runtime"]["source"];
    runtimeVersion: string;
    sdkRef: string;
    sdkSha: string;
    versionOverride: string;
}

export type DispatchRole = "duplicate" | "owner" | "recovery";

const workflowPath = ".github/workflows/runtime-sdk.yml";
const workflowName = "Runtime-driven Node SDK";

function validateInputs(expected: ExpectedDispatch): void {
    assert.match(expected.currentRunId, /^[0-9]+$/, "Current workflow run ID must be numeric");
    assert.match(expected.runtimeRunId, /^[0-9]+$/, "Runtime workflow run ID must be numeric");
    assert.match(expected.runtimeSha, /^[0-9a-f]{40}$/, "Runtime SHA must be lowercase full SHA");
    assert.match(expected.sdkSha, /^[0-9a-f]{40}$/, "SDK SHA must be lowercase full SHA");
    assert(expected.sdkRef.length > 0, "SDK ref is required");
    assert(
        expected.channel === "canary"
            ? expected.runtimeSource === "azure" &&
                  (expected.mode === "tests-only" || expected.mode === "internal") &&
                  expected.resumeRunId === ""
            : expected.runtimeSource === "github-packages" && expected.mode === "internal",
        "Invalid channel, runtime source, mode, or recovery combination"
    );
    if (expected.resumeRunId) {
        assert.match(expected.resumeRunId, /^[0-9]+$/, "Resume workflow run ID must be numeric");
    }
}

export function createRuntimeDispatchMarker(expected: ExpectedDispatch): RuntimeDispatchMarker {
    validateInputs(expected);
    return {
        schemaVersion: 1,
        canonicalRunId: expected.currentRunId,
        channel: expected.channel,
        mode: expected.mode,
        runtime: {
            repository: "github/copilot-agent-runtime",
            runId: expected.runtimeRunId,
            sha: expected.runtimeSha,
            source: expected.runtimeSource,
            version: expected.runtimeVersion,
        },
        sdk: {
            repository: "github/copilot-sdk",
            ref: expected.sdkRef,
            sha: expected.sdkSha,
            versionOverride: expected.versionOverride,
        },
        workflow: workflowPath,
        createdAt: new Date().toISOString(),
    };
}

export function validateRuntimeDispatchMarker(
    marker: RuntimeDispatchMarker,
    artifact: ArtifactApiResponse,
    workflowRun: WorkflowRunApiResponse,
    expected: ExpectedDispatch
): DispatchRole {
    validateInputs(expected);
    assert.equal(marker.schemaVersion, 1, "Unsupported dispatch marker schema");
    assert.match(marker.canonicalRunId, /^[0-9]+$/, "Canonical workflow run ID must be numeric");
    assert.equal(artifact.expired, false, "Dispatch marker artifact is expired");
    assert.equal(
        String(artifact.workflow_run?.id),
        marker.canonicalRunId,
        "Artifact workflow run ID does not match its marker"
    );
    assert.equal(String(workflowRun.id), marker.canonicalRunId, "Workflow run provenance mismatch");
    assert.equal(workflowRun.repository.full_name, "github/copilot-sdk");
    assert.equal(workflowRun.path, workflowPath);
    assert.equal(workflowRun.name, workflowName);
    assert.equal(workflowRun.event, "workflow_dispatch");
    assert.equal(workflowRun.head_sha, marker.sdk.sha);
    assert.equal(workflowRun.head_branch, marker.sdk.ref.replace(/^refs\/(heads|tags)\//, ""));
    assert.deepEqual(
        {
            channel: marker.channel,
            mode: marker.mode,
            runtime: marker.runtime,
            sdk: marker.sdk,
            workflow: marker.workflow,
        },
        {
            channel: expected.channel,
            mode: expected.mode,
            runtime: {
                repository: "github/copilot-agent-runtime",
                runId: expected.runtimeRunId,
                sha: expected.runtimeSha,
                source: expected.runtimeSource,
                version: expected.runtimeVersion,
            },
            sdk: {
                repository: "github/copilot-sdk",
                ref: expected.sdkRef,
                sha: expected.sdkSha,
                versionOverride: expected.versionOverride,
            },
            workflow: workflowPath,
        },
        "runtime_run_id is already claimed by a different release tuple"
    );

    if (marker.canonicalRunId === expected.currentRunId) {
        return "owner";
    }
    if (expected.resumeRunId) {
        assert.equal(
            expected.resumeRunId,
            marker.canonicalRunId,
            "resume_run_id must identify the canonical workflow run"
        );
        return "recovery";
    }
    return "duplicate";
}

function requiredEnvironment(name: string): string {
    const value = process.env[name]?.trim();
    if (!value) {
        throw new Error(`${name} is required.`);
    }
    return value;
}

function expectedFromEnvironment(): ExpectedDispatch {
    return {
        channel: requiredEnvironment("CHANNEL") as ExpectedDispatch["channel"],
        currentRunId: requiredEnvironment("CURRENT_RUN_ID"),
        mode: requiredEnvironment("MODE") as ExpectedDispatch["mode"],
        resumeRunId: process.env.RESUME_RUN_ID?.trim() ?? "",
        runtimeRunId: requiredEnvironment("RUNTIME_RUN_ID"),
        runtimeSha: requiredEnvironment("RUNTIME_SHA"),
        runtimeSource: requiredEnvironment("RUNTIME_SOURCE") as ExpectedDispatch["runtimeSource"],
        runtimeVersion: requiredEnvironment("RUNTIME_VERSION"),
        sdkRef: requiredEnvironment("SDK_REF"),
        sdkSha: requiredEnvironment("SDK_SHA"),
        versionOverride: process.env.VERSION_OVERRIDE?.trim() ?? "",
    };
}

function main(): void {
    const [command, markerPath, artifactPath, runPath] = process.argv.slice(2);
    const expected = expectedFromEnvironment();
    if (command === "create" && markerPath) {
        writeFileSync(
            markerPath,
            `${JSON.stringify(createRuntimeDispatchMarker(expected), null, 2)}\n`
        );
        return;
    }
    if (command === "validate" && markerPath && artifactPath && runPath) {
        const marker = JSON.parse(readFileSync(markerPath, "utf8")) as RuntimeDispatchMarker;
        const artifact = JSON.parse(readFileSync(artifactPath, "utf8")) as ArtifactApiResponse;
        const run = JSON.parse(readFileSync(runPath, "utf8")) as WorkflowRunApiResponse;
        const role = validateRuntimeDispatchMarker(marker, artifact, run, expected);
        if (process.env.GITHUB_OUTPUT) {
            writeFileSync(
                process.env.GITHUB_OUTPUT,
                `role=${role}\ncanonical_run_id=${marker.canonicalRunId}\n`,
                {
                    flag: "a",
                }
            );
        } else {
            console.log(role);
        }
        return;
    }
    throw new Error(
        "Usage: runtime-dispatch-ledger.ts create <marker> | validate <marker> <artifact> <run>"
    );
}

const scriptPath = process.argv[1]
    ? fileURLToPath(import.meta.url) === resolve(process.argv[1])
    : false;
if (scriptPath) {
    try {
        main();
    } catch (error) {
        console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    }
}
