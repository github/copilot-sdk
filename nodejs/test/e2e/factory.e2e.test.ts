import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { copyFile, mkdir, rm } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
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

async function setupFactoryExtension(workDir: string) {
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
        onPermissionRequest: approveAll,
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
        });

        expect(result).toMatchObject({
            status: "completed",
            result: { source: "sdk-e2e", count: 11 },
        });
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

        const result = await session.factory.run("starts-from-context-session");

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

        const result = await session.factory.run("starts-from-module-session");

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

        const parked = session.factory.run("parked");
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

        const result = await session.factory.run("array-result");

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
        const result = await session.factory.run("argument-echo", { args });

        expect(result).toMatchObject({
            status: "completed",
            result: args,
        });
    }
);
