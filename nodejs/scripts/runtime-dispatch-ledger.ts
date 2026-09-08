import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
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

export interface ArtifactApiResponse {
    expired: boolean;
    id: number;
    name: string;
    workflow_run?: { id?: number };
}

export interface WorkflowRunApiResponse {
    display_title: string;
    event: string;
    head_branch: string;
    head_sha: string;
    id: number;
    name: string;
    path: string;
    repository: { full_name: string };
    status: string;
}

export interface ExpectedDispatch {
    channel: RuntimeDispatchMarker["channel"];
    currentRunId: string;
    mode: RuntimeDispatchMarker["mode"];
    runtimeRunId: string;
    runtimeSha: string;
    runtimeSource: RuntimeDispatchMarker["runtime"]["source"];
    runtimeVersion: string;
    sdkRef: string;
    sdkSha: string;
    versionOverride: string;
}

export type DispatchRole = "duplicate" | "owner";

export interface DispatchClaim {
    canonicalRunId: string;
    created: boolean;
    marker: RuntimeDispatchMarker;
    role: DispatchRole;
}

export interface DispatchLedgerClient {
    downloadMarker(artifactId: number): Promise<RuntimeDispatchMarker>;
    getWorkflowRun(runId: number): Promise<WorkflowRunApiResponse>;
    listArtifacts(markerName: string): Promise<ArtifactApiResponse[]>;
    listWorkflowRuns(): Promise<WorkflowRunApiResponse[]>;
}

export interface ClaimOptions {
    attempts?: number;
    delay?: (milliseconds: number) => Promise<void>;
    delayMilliseconds?: number;
    onWait?: (attempt: number, attempts: number) => void;
}

const workflowPath = ".github/workflows/runtime-sdk.yml";
const workflowName = "Runtime-driven Node SDK";
const canonicalNumericIdPattern = /^(0|[1-9][0-9]*)$/;
const runtimeVersionPattern =
    /^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$/;

function validateInputs(expected: ExpectedDispatch): void {
    for (const [name, value] of Object.entries(expected)) {
        assert.equal(value, value.trim(), `${name} must not contain surrounding whitespace`);
    }
    assert.match(
        expected.currentRunId,
        canonicalNumericIdPattern,
        "Current workflow run ID must be canonical numeric"
    );
    assert.match(
        expected.runtimeRunId,
        canonicalNumericIdPattern,
        "Runtime workflow run ID must be canonical numeric"
    );
    assert.match(expected.runtimeSha, /^[0-9a-f]{40}$/, "Runtime SHA must be lowercase full SHA");
    assert.match(
        expected.runtimeVersion,
        runtimeVersionPattern,
        "Runtime version must be exact SemVer"
    );
    assert.match(expected.sdkSha, /^[0-9a-f]{40}$/, "SDK SHA must be lowercase full SHA");
    assert(expected.sdkRef.length > 0, "SDK ref is required");
    assert(
        expected.channel === "canary"
            ? expected.runtimeSource === "azure" &&
                  (expected.mode === "tests-only" || expected.mode === "internal")
            : expected.channel === "unstable" &&
                  expected.runtimeSource === "github-packages" &&
                  expected.mode === "internal",
        "Invalid channel, runtime source, or mode combination"
    );
    assert(
        expected.channel !== "canary" || expected.versionOverride === "",
        "Canary runs do not accept a version override"
    );
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
    assert.match(
        marker.canonicalRunId,
        canonicalNumericIdPattern,
        "Canonical workflow run ID must be canonical numeric"
    );
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
    return "duplicate";
}

export async function claimRuntimeDispatch(
    expected: ExpectedDispatch,
    client: DispatchLedgerClient,
    options: ClaimOptions = {}
): Promise<DispatchClaim> {
    validateInputs(expected);
    const attempts = options.attempts ?? 6;
    const delayMilliseconds = options.delayMilliseconds ?? 10_000;
    const delay =
        options.delay ??
        ((milliseconds: number) =>
            new Promise((resolveDelay) => setTimeout(resolveDelay, milliseconds)));
    const markerName = `sdk-runtime-dispatch-${expected.runtimeRunId}`;
    const runTitle = `Runtime-driven SDK from runtime run ${expected.runtimeRunId}`;
    let earlierRuns: WorkflowRunApiResponse[] = [];

    for (let attempt = 1; attempt <= attempts; attempt += 1) {
        const artifacts = (await client.listArtifacts(markerName)).filter(
            (artifact) => artifact.name === markerName && !artifact.expired
        );
        assert(artifacts.length <= 1, `More than one unexpired ${markerName} artifact exists.`);
        const artifact = artifacts[0];
        if (artifact) {
            const marker = await client.downloadMarker(artifact.id);
            const canonicalRunId = Number(marker.canonicalRunId);
            const workflowRun = await client.getWorkflowRun(canonicalRunId);
            return {
                canonicalRunId: marker.canonicalRunId,
                created: false,
                marker,
                role: validateRuntimeDispatchMarker(marker, artifact, workflowRun, expected),
            };
        }

        earlierRuns = (await client.listWorkflowRuns()).filter(
            (run) => run.display_title === runTitle && run.id < Number(expected.currentRunId)
        );
        if (earlierRuns.length === 0) {
            const marker = createRuntimeDispatchMarker(expected);
            return {
                canonicalRunId: expected.currentRunId,
                created: true,
                marker,
                role: "owner",
            };
        }
        if (attempt < attempts) {
            options.onWait?.(attempt, attempts);
            await delay(delayMilliseconds);
        }
    }

    assert(
        !earlierRuns.some((run) => run.status !== "completed"),
        "An earlier matching run is still initializing without a visible marker. Retry this run later."
    );
    const marker = createRuntimeDispatchMarker(expected);
    return {
        canonicalRunId: expected.currentRunId,
        created: true,
        marker,
        role: "owner",
    };
}

function requiredEnvironment(name: string): string {
    const value = process.env[name];
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
        runtimeRunId: requiredEnvironment("RUNTIME_RUN_ID"),
        runtimeSha: requiredEnvironment("RUNTIME_SHA"),
        runtimeSource: requiredEnvironment("RUNTIME_SOURCE") as ExpectedDispatch["runtimeSource"],
        runtimeVersion: requiredEnvironment("RUNTIME_VERSION"),
        sdkRef: requiredEnvironment("SDK_REF"),
        sdkSha: requiredEnvironment("SDK_SHA"),
        versionOverride: process.env.VERSION_OVERRIDE ?? "",
    };
}

function githubClient(): DispatchLedgerClient {
    const apiUrl = requiredEnvironment("GITHUB_API_URL");
    const repository = requiredEnvironment("GITHUB_REPOSITORY");
    const token = requiredEnvironment("GH_TOKEN");
    const temporaryDirectory = requiredEnvironment("RUNNER_TEMP");

    async function request(path: string): Promise<Response> {
        const response = await fetch(`${apiUrl}${path}`, {
            headers: {
                Accept: "application/vnd.github+json",
                Authorization: `Bearer ${token}`,
                "X-GitHub-Api-Version": "2022-11-28",
            },
        });
        if (!response.ok) {
            throw new Error(`GitHub API request failed (${response.status}): ${path}`);
        }
        return response;
    }

    return {
        async listArtifacts(markerName) {
            const response = await request(
                `/repos/${repository}/actions/artifacts?name=${encodeURIComponent(markerName)}&per_page=100`
            );
            return ((await response.json()) as { artifacts: ArtifactApiResponse[] }).artifacts;
        },
        async listWorkflowRuns() {
            const response = await request(
                `/repos/${repository}/actions/workflows/runtime-sdk.yml/runs?event=workflow_dispatch&per_page=100`
            );
            return ((await response.json()) as { workflow_runs: WorkflowRunApiResponse[] })
                .workflow_runs;
        },
        async downloadMarker(artifactId) {
            const zipPath = join(temporaryDirectory, "dispatch-marker.zip");
            const markerDirectory = join(temporaryDirectory, "dispatch-marker");
            rmSync(markerDirectory, { force: true, recursive: true });
            mkdirSync(markerDirectory, { recursive: true });
            const response = await request(
                `/repos/${repository}/actions/artifacts/${artifactId}/zip`
            );
            writeFileSync(zipPath, Buffer.from(await response.arrayBuffer()));
            execFileSync("unzip", ["-q", zipPath, "-d", markerDirectory]);
            return JSON.parse(
                readFileSync(join(markerDirectory, "marker.json"), "utf8")
            ) as RuntimeDispatchMarker;
        },
        async getWorkflowRun(runId) {
            const response = await request(`/repos/${repository}/actions/runs/${runId}`);
            return (await response.json()) as WorkflowRunApiResponse;
        },
    };
}

async function main(): Promise<void> {
    const [command, markerPath] = process.argv.slice(2);
    if (command !== "claim" || !markerPath) {
        throw new Error("Usage: runtime-dispatch-ledger.ts claim <marker-path>");
    }
    const claim = await claimRuntimeDispatch(expectedFromEnvironment(), githubClient(), {
        onWait: (attempt, attempts) =>
            console.log(
                `An earlier matching run is visible; waiting for its marker (attempt ${attempt}/${attempts}).`
            ),
    });
    if (claim.created) {
        mkdirSync(dirname(markerPath), { recursive: true });
        writeFileSync(markerPath, `${JSON.stringify(claim.marker, null, 2)}\n`);
    }
    const output = `role=${claim.role}\ncanonical_run_id=${claim.canonicalRunId}\ncreated=${claim.created}\n`;
    if (process.env.GITHUB_OUTPUT) {
        writeFileSync(process.env.GITHUB_OUTPUT, output, { flag: "a" });
    } else {
        process.stdout.write(output);
    }
}

async function runMain(): Promise<void> {
    try {
        await main();
    } catch (error) {
        console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    }
}

const scriptPath = process.argv[1]
    ? fileURLToPath(import.meta.url) === resolve(process.argv[1])
    : false;
if (scriptPath) {
    void runMain();
}
