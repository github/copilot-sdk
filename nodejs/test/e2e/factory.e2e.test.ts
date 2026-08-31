import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { copyFile, mkdir, rm } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { expect, it, vi } from "vitest";
import { approveAll, FactoryResumeError } from "../../src/index.js";
import {
    createSdkTestContext,
    DEFAULT_GITHUB_TOKEN,
    isInProcessTransport,
} from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const factoryTestContext = isInProcessTransport
    ? undefined
    : await createSdkTestContext({
          copilotClientOptions: {
              env: {
                  COPILOT_CLI_ENABLED_FEATURE_FLAGS: "EXTENSIONS,AGENT_FACTORIES",
              },
          },
      });

async function setupFactoryExtension(workDir: string, onPermissionRequest = approveAll) {
    if (!factoryTestContext) {
        throw new Error("Factory E2E requires the stdio transport");
    }

    const { copilotClient, openAiEndpoint } = factoryTestContext;
    const extensionDir = join(workDir, ".github", "extensions", "factory-smoke");
    const readyFile = join(extensionDir, "ready");
    await rm(join(workDir, ".github"), { recursive: true, force: true });
    await mkdir(extensionDir, { recursive: true });
    await copyFile(
        join(__dirname, "fixtures", "factory-extension.mjs"),
        join(extensionDir, "extension.mjs")
    );
    execFileSync("git", ["init", "--quiet"], { cwd: workDir });

    await openAiEndpoint.setCopilotUserByToken(DEFAULT_GITHUB_TOKEN, {
        login: "factory-e2e-user",
        copilot_plan: "individual_pro",
        token_based_billing: true,
        is_mcp_enabled: true,
        endpoints: {
            api: openAiEndpoint.url,
            telemetry: "https://localhost:1/telemetry",
        },
        analytics_tracking_id: "e2e-test-tracking-id",
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

    await retry(
        "wait for the factory extension to join the session",
        async () => {
            expect(existsSync(readyFile)).toBe(true);
        },
        300,
        100
    );

    return session;
}

it.skipIf(isInProcessTransport)(
    "runs an extension-authored factory across the SDK process boundary",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        await using session = await setupFactoryExtension(workDir);

        const result = await session.factory.run("argument-echo", {
            args: { source: "sdk-e2e", count: 11 },
            notifyOnComplete: false,
        });

        expect(result).toMatchObject({
            status: "completed",
            result: { source: "sdk-e2e", count: 11 },
        });
    }
);

// TODO(cli-1.0.81-2): the subagent request is rejected downstream under CLI 1.0.81-2, so the
// fixture reports didThrow: true. Re-enable once the runtime fix ships.
//
// The timeout is generous because the factory abandons its subagent once the runtime has
// accepted the request, so the run settles only after the runtime drains that work.
it.skip("forwards every declared subagent option to the runtime", async () => {
    if (!factoryTestContext) {
        throw new Error("Factory E2E requires the stdio transport");
    }
    const { workDir } = factoryTestContext;
    await using session = await setupFactoryExtension(workDir);

    const result = await session.factory.run("forwards-subagent-options");

    expect(result).toMatchObject({
        status: "completed",
        result: { didThrow: false },
    });
}, 60_000);

it.skipIf(isInProcessTransport)(
    "throws FactoryResumeError with not_found for an unknown run",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        await using session = await setupFactoryExtension(workDir);

        const error = await session.factory
            .resume("00000000-0000-0000-0000-000000000000")
            .catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(FactoryResumeError);
        expect((error as FactoryResumeError).code).toBe("not_found");
    }
);

it.skipIf(isInProcessTransport)(
    "throws FactoryResumeError with non_resumable for a completed run",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        await using session = await setupFactoryExtension(workDir);

        const run = await session.factory.run("argument-echo", { notifyOnComplete: false });
        const error = await session.factory.resume(run.runId).catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(FactoryResumeError);
        expect((error as FactoryResumeError).code).toBe("non_resumable");
    }
);

it.skipIf(isInProcessTransport)(
    "forwards factory runtime controls across the SDK process boundary",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        await using session = await setupFactoryExtension(workDir);

        const suppressed = await session.factory.run("phased", {
            notifyOnComplete: false,
            logPhaseNames: false,
        });

        expect(suppressed).toMatchObject({
            status: "completed",
            result: "finished",
        });
        const progress = await session.factory.getRunProgress(suppressed.runId);
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
                    event.data.kind.type === "factory_completed" &&
                    event.data.kind.runId === suppressed.runId
            )
        ).toBe(false);
        expect(
            events.filter(
                (event) => event.type === "session.info" && event.data.infoType === "factory_phase"
            )
        ).toEqual([]);
    }
);

it.skipIf(isInProcessTransport)("pages factory runs and returns cursor metadata", async () => {
    if (!factoryTestContext) {
        throw new Error("Factory E2E requires the stdio transport");
    }
    const { workDir } = factoryTestContext;
    await using session = await setupFactoryExtension(workDir);

    const first = await session.factory.run("argument-echo", {
        args: { ordinal: 1 },
        notifyOnComplete: false,
    });
    const second = await session.factory.run("argument-echo", {
        args: { ordinal: 2 },
        notifyOnComplete: false,
    });
    const third = await session.factory.run("argument-echo", {
        args: { ordinal: 3 },
        notifyOnComplete: false,
    });

    const newest = await session.factory.listRuns({ limit: 1 });
    expect(newest).toMatchObject({
        runs: [expect.objectContaining({ runId: third.runId })],
        hasMoreNewer: false,
        omittedOlder: 2,
    });
    expect(newest.oldestSeq).toBe(newest.newestSeq);
    expect(newest.oldestSeq).not.toBeNull();

    const older = await session.factory.listRuns({
        beforeSeq: newest.oldestSeq!,
        limit: 1,
    });
    expect(older).toMatchObject({
        runs: [expect.objectContaining({ runId: second.runId })],
        hasMoreNewer: true,
        omittedOlder: 1,
    });

    const oldest = await session.factory.listRuns({
        beforeSeq: older.oldestSeq!,
        limit: 1,
    });
    expect(oldest).toMatchObject({
        runs: [expect.objectContaining({ runId: first.runId })],
        hasMoreNewer: true,
        omittedOlder: 0,
    });
});

it.skipIf(isInProcessTransport)(
    "runs a factory when its session denies every permission request",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        const denyPermissions = vi.fn(() => ({ kind: "reject" as const }));
        await using session = await setupFactoryExtension(workDir, denyPermissions);

        await expect(
            session.factory.run("argument-echo", { notifyOnComplete: false })
        ).resolves.toMatchObject({
            status: "completed",
        });
        expect(denyPermissions).not.toHaveBeenCalled();
    }
);

it.skipIf(isInProcessTransport)(
    "resumes a failed factory when its session denies every permission request",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        const denyPermissions = vi.fn(() => ({ kind: "reject" as const }));
        await using session = await setupFactoryExtension(workDir, denyPermissions);

        const failedRun = await session.factory.run("fails-once", { notifyOnComplete: false });
        expect(failedRun).toMatchObject({
            status: "error",
        });

        await expect(
            session.factory.resume(failedRun.runId, {
                notifyOnComplete: false,
                logPhaseNames: false,
            })
        ).resolves.toMatchObject({
            status: "completed",
            result: "resumed",
        });
        expect(denyPermissions).not.toHaveBeenCalled();
    }
);

it.skipIf(isInProcessTransport)(
    "refuses a factory started through the context session from a factory body",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        await using session = await setupFactoryExtension(workDir);

        const result = await session.factory.run("starts-from-context-session", {
            notifyOnComplete: false,
        });

        expect(result).toMatchObject({
            status: "completed",
            result: expect.stringContaining("factory.run and factory.resume"),
        });
        expect((result as { result: string }).result).toContain("factory body");
    }
);

it.skipIf(isInProcessTransport)(
    "refuses a factory started through the module session from a factory body",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        await using session = await setupFactoryExtension(workDir);

        const result = await session.factory.run("starts-from-module-session", {
            notifyOnComplete: false,
        });

        expect(result).toMatchObject({
            status: "completed",
            result: expect.stringContaining("factory.run and factory.resume"),
        });
        expect((result as { result: string }).result).toContain("factory body");
    }
);

it.skipIf(isInProcessTransport)(
    "allows a module-level extension watcher to start a factory while another body is parked",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        const extensionDir = join(workDir, ".github", "extensions", "factory-smoke");
        await using session = await setupFactoryExtension(workDir);

        const parked = session.factory.run("parked", { notifyOnComplete: false });
        await retry(
            "wait for the parked factory to enter its body",
            async () => {
                expect(existsSync(join(extensionDir, "entered"))).toBe(true);
            },
            100,
            100
        );

        writeFileSync(join(extensionDir, "start-b"), "start");
        const bResultFile = join(extensionDir, "b-result");
        await retry(
            "wait for the module-level watcher factory run to succeed",
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
    },
    60_000
);

it.skipIf(isInProcessTransport)(
    "returns an array result from an extension-authored factory",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        await using session = await setupFactoryExtension(workDir);

        const result = await session.factory.run("array-result", { notifyOnComplete: false });

        expect(result).toMatchObject({
            status: "completed",
            result: [1, "two", false],
        });
    }
);

it.skipIf(isInProcessTransport)(
    "passes array factory arguments across the SDK process boundary",
    async () => {
        if (!factoryTestContext) {
            throw new Error("Factory E2E requires the stdio transport");
        }
        const { workDir } = factoryTestContext;
        await using session = await setupFactoryExtension(workDir);

        const args = [1, "two", false];
        const result = await session.factory.run("argument-echo", {
            args,
            notifyOnComplete: false,
        });

        expect(result).toMatchObject({
            status: "completed",
            result: args,
        });
    }
);
