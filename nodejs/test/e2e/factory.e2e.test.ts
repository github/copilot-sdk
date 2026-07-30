import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";
import { copyFile, mkdir } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const factoryTestContext = await createSdkTestContext({
    copilotClientOptions: {
        env: {
            COPILOT_CLI_ENABLED_FEATURE_FLAGS: "EXTENSIONS,AGENT_FACTORIES",
        },
    },
});

it("runs an extension-authored factory across the SDK process boundary", async () => {
    const { copilotClient, openAiEndpoint, workDir } = factoryTestContext;

    await openAiEndpoint.setCopilotUserByToken(DEFAULT_GITHUB_TOKEN, {
        login: "factory-e2e-user",
        copilot_plan: "individual_pro",
        token_based_billing: true,
    });

    const extensionDir = join(workDir, ".github", "extensions", "factory-smoke");
    const readyFile = join(extensionDir, "ready");
    await mkdir(extensionDir, { recursive: true });
    await copyFile(
        join(__dirname, "fixtures", "factory-extension.mjs"),
        join(extensionDir, "extension.mjs")
    );
    execFileSync("git", ["init", "--quiet"], { cwd: workDir });

    await using session = await copilotClient.createSession({
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

    const result = await session.factory.run("argument-echo", {
        args: { source: "sdk-e2e", count: 11 },
    });

    expect(result).toMatchObject({
        status: "completed",
        result: { source: "sdk-e2e", count: 11 },
    });
});
