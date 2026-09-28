/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { mkdir, rm, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { expect, it } from "vitest";
import type { PermissionRequest } from "../../src/index.js";
import { RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext, getLegacyCliPathForTests } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const cliPath = await getLegacyCliPathForTests();
const cliDistDirectory = process.env.COPILOT_EXTENSION_SDK_PATH
    ? dirname(process.env.COPILOT_EXTENSION_SDK_PATH)
    : dirname(cliPath);
const sdkDistDirectory = resolve(__dirname, "..", "..", "dist");
const extensionLaunchProvider = {
    resolve: async (request: { modulePath: string }) => ({
        launch: {
            executable: "node",
            args: [join(cliDistDirectory, "preloads", "extension_bootstrap.mjs")],
            env: {
                COPILOT_CLI_DIST_DIR: cliDistDirectory,
                EXTENSION_PATH: request.modulePath,
            },
        },
    }),
};
const { copilotClient, createClient, workDir } = await createSdkTestContext({
    copilotClientOptions: {
        connection: RuntimeConnection.forStdio({ path: cliPath }),
        env: {
            COPILOT_CLI_ENABLED_FEATURE_FLAGS: "EXTENSIONS",
            SDK_ASYNC_EXTENSION_INITIALIZATION: "false",
            COPILOT_EXP_COPILOT_SDK_ASYNC_EXTENSION_INITIALIZATION: "false",
        },
        extensionLaunchProvider,
    },
});

async function installHookExtension(name: string): Promise<string> {
    const extensionDir = join(workDir, ".github", "extensions", name);
    const readyFile = join(extensionDir, "joined");
    await mkdir(extensionDir, { recursive: true });
    await writeFile(
        join(extensionDir, "extension.mjs"),
        `
import { writeFileSync } from "node:fs";
import { joinSession } from "@github/copilot-sdk/extension";

await joinSession({
    tools: [{
        name: "${name}_tool",
        description: "Proves the extension joined after permission approval.",
        parameters: { type: "object", properties: {} },
        handler: async () => "ready",
    }],
    hooks: { onPreToolUse: async () => ({}) },
});
writeFileSync(${JSON.stringify(readyFile)}, "joined");
setInterval(() => {}, 60_000);
`
    );
    return readyFile;
}

it("routes legacy create and explicit resume startup permissions through the Node SDK", async () => {
    execFileSync("git", ["init", "--quiet"], { cwd: workDir });
    const createReadyFile = await installHookExtension("create_hook");
    const createPermissions: Array<{
        request: PermissionRequest;
        sessionId: string;
        beforeResponse: boolean;
    }> = [];
    let createReturned = false;
    const createPromise = copilotClient
        .createSession({
            // Explicit extension requests initialize asynchronously; legacy discovery keeps create waiting.
            enableConfigDiscovery: true,
            extensionSdkPath: sdkDistDirectory,
            disabledMcpServers: ["github-mcp-server"],
            onPermissionRequest: (request, invocation) => {
                createPermissions.push({
                    request,
                    sessionId: invocation.sessionId,
                    beforeResponse: !createReturned,
                });
                return { kind: "approve-once", approvedInteractively: true };
            },
        })
        .finally(() => {
            createReturned = true;
        });
    const created = await createPromise;
    await retry(
        "wait for create extension to join",
        async () => {
            expect(existsSync(createReadyFile) && readFileSync(createReadyFile, "utf8")).toBe(
                "joined"
            );
        },
        300,
        100
    );
    expect(createPermissions).toEqual([
        {
            request: expect.objectContaining({
                kind: "extension-permission-access",
                extensionName: "project:create_hook",
            }),
            sessionId: created.sessionId,
            beforeResponse: true,
        },
    ]);
    expect((await created.rpc.tools.getCurrentMetadata()).tools.map((tool) => tool.name)).toContain(
        "create_hook_tool"
    );
    const sessionId = created.sessionId;
    await created.disconnect();
    expect(await copilotClient.stop()).toEqual([]);

    await rm(join(workDir, ".github", "extensions", "create_hook"), {
        recursive: true,
        force: true,
    });
    const resumeReadyFile = await installHookExtension("resume_hook");
    const resumeClient = createClient({ extensionLaunchProvider });
    try {
        const resumePermissions: Array<{
            request: PermissionRequest;
            sessionId: string;
        }> = [];
        const resumed = await resumeClient.resumeSession(sessionId, {
            requestExtensions: true,
            extensionSdkPath: sdkDistDirectory,
            disabledMcpServers: ["github-mcp-server"],
            onPermissionRequest: (request, invocation) => {
                resumePermissions.push({ request, sessionId: invocation.sessionId });
                return { kind: "approve-once", approvedInteractively: true };
            },
        });
        try {
            expect(resumed.sessionId).toBe(sessionId);
            await retry(
                "wait for resume extension to join",
                async () => {
                    expect(
                        existsSync(resumeReadyFile) && readFileSync(resumeReadyFile, "utf8")
                    ).toBe("joined");
                },
                300,
                100
            );
            expect(resumePermissions).toEqual([
                {
                    request: expect.objectContaining({
                        kind: "extension-permission-access",
                        extensionName: "project:resume_hook",
                    }),
                    sessionId,
                },
            ]);
            expect(
                (await resumed.rpc.tools.getCurrentMetadata()).tools.map((tool) => tool.name)
            ).toContain("resume_hook_tool");
        } finally {
            await resumed.disconnect();
        }
    } finally {
        expect(await resumeClient.stop()).toEqual([]);
    }
});
