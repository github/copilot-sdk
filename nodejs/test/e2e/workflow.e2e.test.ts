import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { copyFile, mkdir, rm } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { expect, it, vi } from "vitest";
import {
    createSdkTestContext,
    DEFAULT_GITHUB_TOKEN,
    getLegacyCliPathForTests,
} from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const sdkEntryPoint = process.env.COPILOT_CLI_PATH
    ? new URL("../../dist/index.js", import.meta.url).href
    : new URL("../../src/index.js", import.meta.url).href;
const { approveAll, RuntimeConnection } = (await import(
    sdkEntryPoint
)) as typeof import("../../src/index.js");
const cliPath = process.env.COPILOT_CLI_PATH ?? (await getLegacyCliPathForTests());
const cliDistDirectory = process.env.COPILOT_EXTENSION_SDK_PATH
    ? dirname(process.env.COPILOT_EXTENSION_SDK_PATH)
    : dirname(cliPath);
const workflowTestContext = await createSdkTestContext({
    copilotClientOptions: {
        connection: RuntimeConnection.forStdio({ path: cliPath }),
        env: {
            COPILOT_CLI_ENABLED_FEATURE_FLAGS: "EXTENSIONS,AGENT_FACTORIES",
        },
        extensionLaunchProvider: {
            resolve: async (request) => ({
                launch: {
                    executable: "node",
                    args: [join(cliDistDirectory, "preloads", "extension_bootstrap.mjs")],
                    env: {
                        COPILOT_CLI_DIST_DIR: cliDistDirectory,
                        EXTENSION_PATH: request.modulePath,
                    },
                },
            }),
        },
    },
});

async function setupWorkflowExtension(workDir: string, onPermissionRequest = approveAll) {
    const { copilotClient, openAiEndpoint } = workflowTestContext;
    const extensionDir = join(workDir, ".github", "extensions", "workflow-smoke");
    const readyFile = join(extensionDir, "ready");
    await rm(join(workDir, ".github"), { recursive: true, force: true });
    await mkdir(extensionDir, { recursive: true });
    await copyFile(
        join(__dirname, "fixtures", "workflow-extension.mjs"),
        join(extensionDir, "extension.mjs")
    );
    execFileSync("git", ["init", "--quiet"], { cwd: workDir });

    await openAiEndpoint.setCopilotUserByToken(DEFAULT_GITHUB_TOKEN, {
        login: "workflow-e2e-user",
        copilot_plan: "individual_pro",
        token_based_billing: true,
        is_mcp_enabled: true,
        endpoints: {
            api: openAiEndpoint.url,
            telemetry: "https://localhost:1/telemetry",
        },
        analytics_tracking_id: "workflow-e2e-tracking-id",
    });

    const session = await copilotClient.createSession({
        requestExtensions: true,
        extensionSdkPath: resolve(__dirname, "..", "..", "dist"),
        onPermissionRequest,
        onElicitationRequest: async () => ({
            action: "accept",
            content: { action: "approve" },
        }),
    });

    try {
        await retry(
            "wait for the workflow extension to join the session",
            async () => {
                expect(existsSync(readyFile)).toBe(true);
            },
            300,
            100
        );
        return session;
    } catch (error) {
        await session.disconnect();
        throw error;
    }
}

it("runs an extension-authored workflow across the SDK process boundary", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const result = await session.workflow.run("argument-echo", {
        args: { source: "sdk-workflow-e2e", count: 12 },
        limits: { timeoutSeconds: 15 },
        notifyOnComplete: false,
    });

    expect(result).toMatchObject({
        status: "completed",
    });
    expect(result.result).toEqual({ source: "sdk-workflow-e2e", count: 12 });
}, 45_000);

it.skip("forwards every declared subagent option to the runtime", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const result = await session.workflow.run("forwards-subagent-options");

    expect(result).toMatchObject({
        status: "completed",
        result: { didThrow: false },
    });
}, 60_000);

it("throws WorkflowResumeError with not_found for an unknown run", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const error = await session.workflow
        .resume("00000000-0000-0000-0000-000000000000")
        .catch((caught: unknown) => caught);

    expect(error).toMatchObject({
        name: "WorkflowResumeError",
        code: "not_found",
    });
});

it("throws WorkflowResumeError with non_resumable for a completed run", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const run = await session.workflow.run("argument-echo", { notifyOnComplete: false });
    const error = await session.workflow.resume(run.runId).catch((caught: unknown) => caught);

    expect(error).toMatchObject({
        name: "WorkflowResumeError",
        code: "non_resumable",
    });
});

it("forwards workflow runtime controls across the SDK process boundary", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const suppressed = await session.workflow.run("phased", {
        notifyOnComplete: false,
        logPhaseNames: false,
    });

    expect(suppressed).toMatchObject({
        status: "completed",
        result: "finished",
    });
    const progress = await session.workflow.getRunProgress(suppressed.runId);
    expect(progress.records).toEqual(
        expect.arrayContaining([
            expect.objectContaining({ kind: "phase", text: "Collect" }),
            expect.objectContaining({ kind: "log", text: "Collected" }),
            expect.objectContaining({ kind: "phase", text: "Summarize" }),
            expect.objectContaining({ kind: "log", text: "Summarized" }),
        ])
    );

    const events = await session.getEvents();
    expect(
        events.some(
            (event) =>
                event.type === "system.notification" &&
                event.data.kind.type === "workflow_completed" &&
                event.data.kind.runId === suppressed.runId
        )
    ).toBe(false);
    expect(
        events.filter(
            (event) => event.type === "session.info" && event.data.infoType === "workflow_phase"
        )
    ).toEqual([]);
});

it("pages workflow runs and returns cursor metadata", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const first = await session.workflow.run("argument-echo", {
        args: { ordinal: 1 },
        notifyOnComplete: false,
    });
    const second = await session.workflow.run("argument-echo", {
        args: { ordinal: 2 },
        notifyOnComplete: false,
    });
    const third = await session.workflow.run("argument-echo", {
        args: { ordinal: 3 },
        notifyOnComplete: false,
    });

    const newest = await session.workflow.listRuns({ limit: 1 });
    expect(newest).toMatchObject({
        runs: [expect.objectContaining({ runId: third.runId })],
        hasMoreNewer: false,
        omittedOlder: 2,
    });
    expect(newest.oldestSeq).toBe(newest.newestSeq);
    expect(newest.oldestSeq).not.toBeNull();

    const older = await session.workflow.listRuns({
        beforeSeq: newest.oldestSeq!,
        limit: 1,
    });
    expect(older).toMatchObject({
        runs: [expect.objectContaining({ runId: second.runId })],
        hasMoreNewer: true,
        omittedOlder: 1,
    });

    const oldest = await session.workflow.listRuns({
        beforeSeq: older.oldestSeq!,
        limit: 1,
    });
    expect(oldest).toMatchObject({
        runs: [expect.objectContaining({ runId: first.runId })],
        hasMoreNewer: true,
        omittedOlder: 0,
    });
});

it("runs a workflow when its session denies every permission request", async () => {
    const { workDir } = workflowTestContext;
    const denyPermissions = vi.fn(() => ({ kind: "reject" as const }));
    await using session = await setupWorkflowExtension(workDir, denyPermissions);

    await expect(
        session.workflow.run("argument-echo", { notifyOnComplete: false })
    ).resolves.toMatchObject({
        status: "completed",
    });
    expect(denyPermissions).not.toHaveBeenCalled();
});

it("resumes a failed workflow when its session denies every permission request", async () => {
    const { workDir } = workflowTestContext;
    const denyPermissions = vi.fn(() => ({ kind: "reject" as const }));
    await using session = await setupWorkflowExtension(workDir, denyPermissions);

    const failedRun = await session.workflow.run("fails-once", { notifyOnComplete: false });
    expect(failedRun).toMatchObject({
        status: "error",
    });

    await expect(
        session.workflow.resume(failedRun.runId, {
            notifyOnComplete: false,
            logPhaseNames: false,
        })
    ).resolves.toMatchObject({
        status: "completed",
        result: "resumed",
    });
    expect(denyPermissions).not.toHaveBeenCalled();
});

it("pauses a running workflow through the session API", async () => {
    const { workDir } = workflowTestContext;
    const extensionDir = join(workDir, ".github", "extensions", "workflow-smoke");
    await using session = await setupWorkflowExtension(workDir);

    const execution = session.workflow.run("externally-paused", {
        notifyOnComplete: false,
    });
    await retry(
        "wait for the externally paused workflow to enter its body",
        async () => {
            expect(existsSync(join(extensionDir, "external-pause-entered"))).toBe(true);
        },
        100,
        100
    );

    let runId: string | undefined;
    await retry(
        "find the running workflow before pausing it",
        async () => {
            const running = (await session.workflow.listRuns()).find(
                (run) => run.workflowName === "externally-paused" && run.status === "running"
            );
            expect(running).toBeDefined();
            runId = running?.runId;
        },
        100,
        100
    );
    if (!runId) {
        throw new Error("Running workflow did not expose a run ID");
    }

    await expect(session.workflow.pause(runId)).resolves.toMatchObject({
        runId,
        status: "paused",
    });
    await expect(execution).resolves.toMatchObject({
        runId,
        status: "paused",
    });
});

it("pauses once at a durable checkpoint and continues after resume", async () => {
    const { workDir } = workflowTestContext;
    const extensionDir = join(workDir, ".github", "extensions", "workflow-smoke");
    await using session = await setupWorkflowExtension(workDir);

    const paused = await session.workflow.run("durable-pause-checkpoint", {
        notifyOnComplete: false,
    });
    expect(paused).toMatchObject({ status: "paused" });
    expect(readFileSync(join(extensionDir, "checkpoint-attempts"))).toHaveLength(1);
    expect(readFileSync(join(extensionDir, "checkpoint-preparations"))).toHaveLength(1);

    const resumed = await session.workflow.resume(paused.runId, {
        notifyOnComplete: false,
    });
    expect(resumed).toMatchObject({
        runId: paused.runId,
        status: "completed",
        result: { attempt: 2, prepared: 1 },
    });
    expect(readFileSync(join(extensionDir, "checkpoint-attempts"))).toHaveLength(2);
    expect(readFileSync(join(extensionDir, "checkpoint-preparations"))).toHaveLength(1);
});

it("refuses a workflow started through the context session from a workflow body", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const result = await session.workflow.run("starts-from-context-session", {
        notifyOnComplete: false,
    });

    expect(result).toMatchObject({
        status: "completed",
        result: expect.stringContaining("workflow.run, workflow.resume, and workflow.pause"),
    });
    expect((result as { result: string }).result).toContain("workflow body");
});

it("refuses a workflow started through the module session from a workflow body", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const result = await session.workflow.run("starts-from-module-session", {
        notifyOnComplete: false,
    });

    expect(result).toMatchObject({
        status: "completed",
        result: expect.stringContaining("workflow.run, workflow.resume, and workflow.pause"),
    });
    expect((result as { result: string }).result).toContain("workflow body");
});

it("allows a module-level extension watcher to start a workflow while another body is parked", async () => {
    const { workDir } = workflowTestContext;
    const extensionDir = join(workDir, ".github", "extensions", "workflow-smoke");
    await using session = await setupWorkflowExtension(workDir);

    const parked = session.workflow.run("parked", { notifyOnComplete: false });
    await retry(
        "wait for the parked workflow to enter its body",
        async () => {
            expect(existsSync(join(extensionDir, "entered"))).toBe(true);
        },
        100,
        100
    );

    writeFileSync(join(extensionDir, "start-b"), "start");
    const bResultFile = join(extensionDir, "b-result");
    await retry(
        "wait for the module-level watcher workflow run to succeed",
        async () => {
            expect(existsSync(bResultFile)).toBe(true);
            expect(JSON.parse(readFileSync(bResultFile, "utf8"))).toMatchObject({
                status: "success",
                result: {
                    status: "completed",
                    result: { source: "module-watcher" },
                },
            });
        },
        100,
        100
    );

    writeFileSync(join(extensionDir, "release"), "release");
    await expect(parked).resolves.toMatchObject({
        status: "completed",
        result: "released",
    });
}, 60_000);

it("returns an array result from an extension-authored workflow", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const result = await session.workflow.run("array-result", { notifyOnComplete: false });

    expect(result).toMatchObject({
        status: "completed",
        result: [1, "two", false],
    });
});

it("passes array workflow arguments across the SDK process boundary", async () => {
    const { workDir } = workflowTestContext;
    await using session = await setupWorkflowExtension(workDir);

    const args = [1, "two", false];
    const result = await session.workflow.run("argument-echo", {
        args,
        notifyOnComplete: false,
    });

    expect(result).toMatchObject({
        status: "completed",
        result: args,
    });
});
