/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { mkdir, rm, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { afterEach, expect, it } from "vitest";
import type { CopilotSession } from "../../src/index.js";
import { RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext, getLegacyCliPathForTests } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

const cliPath = await getLegacyCliPathForTests();
const cliDistDirectory = process.env.COPILOT_EXTENSION_SDK_PATH
    ? dirname(process.env.COPILOT_EXTENSION_SDK_PATH)
    : dirname(cliPath);
const sdkDistDirectory = resolve(dirname(fileURLToPath(import.meta.url)), "..", "..", "dist");
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
const { createClient, env, workDir } = await createSdkTestContext({
    copilotClientOptions: {
        connection: RuntimeConnection.forStdio({ path: cliPath }),
        env: { COPILOT_CLI_ENABLED_FEATURE_FLAGS: "EXTENSIONS" },
        extensionLaunchProvider,
    },
});

afterEach(async () => {
    await rm(join(env.COPILOT_HOME, "extensions"), { recursive: true, force: true });
});

async function installExtension(
    directory: string,
    name: string,
    body = `import { joinSession } from "@github/copilot-sdk/extension";
await joinSession({});
setInterval(() => {}, 60_000);`
): Promise<string> {
    const extensionDir = join(directory, name);
    await mkdir(extensionDir, { recursive: true });
    await writeFile(join(extensionDir, "extension.mjs"), body);
    return name;
}

async function withExtensionSession(
    action: (session: CopilotSession) => Promise<void>
): Promise<void> {
    const client = createClient({ extensionLaunchProvider });
    try {
        const session = await client.createSession({
            enableConfigDiscovery: true,
            extensionSdkPath: sdkDistDirectory,
            disabledMcpServers: ["github-mcp-server"],
            onPermissionRequest: () => ({
                kind: "approve-once",
                approvedInteractively: true,
            }),
        });
        try {
            await action(session);
        } finally {
            await session.disconnect();
        }
    } finally {
        expect(await client.stop()).toEqual([]);
    }
}

async function waitForExtension(session: CopilotSession, id: string, status: string) {
    let found:
        | Awaited<ReturnType<typeof session.rpc.extensions.list>>["extensions"][number]
        | undefined;
    await retry(
        `observe ${id} in ${status} status (last: ${found?.status ?? "absent"})`,
        async () => {
            found = (await session.rpc.extensions.list()).extensions.find((ext) => ext.id === id);
            expect(found?.status).toBe(status);
        },
        300,
        100
    );
    return found!;
}

it(
    "discovers and runs project and user extensions independently",
    { timeout: 120_000 },
    async () => {
        execFileSync("git", ["init", "--quiet"], { cwd: workDir });
        const projectName = await installExtension(
            join(workDir, ".github", "extensions"),
            `project-${randomUUID()}`
        );
        const userName = await installExtension(
            join(env.COPILOT_HOME, "extensions"),
            `user-${randomUUID()}`
        );
        await withExtensionSession(async (session) => {
            const project = await waitForExtension(session, `project:${projectName}`, "running");
            const user = await waitForExtension(session, `user:${userName}`, "running");
            expect(project).toMatchObject({ name: projectName, source: "project" });
            expect(user).toMatchObject({ name: userName, source: "user" });
            expect(project.pid).toBeGreaterThan(0);
            expect(user.pid).toBeGreaterThan(0);
            expect(project.pid).not.toBe(user.pid);
        });
    }
);

it(
    "disables, re-enables, and preserves disabled status across reload",
    { timeout: 120_000 },
    async () => {
        const name = await installExtension(
            join(env.COPILOT_HOME, "extensions"),
            `toggle-${randomUUID()}`
        );
        const id = `user:${name}`;
        await withExtensionSession(async (session) => {
            await waitForExtension(session, id, "running");
            await session.rpc.extensions.disable({ id });
            expect((await waitForExtension(session, id, "disabled")).pid ?? null).toBeNull();
            await session.rpc.extensions.reload();
            expect((await waitForExtension(session, id, "disabled")).pid ?? null).toBeNull();
            await session.rpc.extensions.enable({ id });
            expect((await waitForExtension(session, id, "running")).pid).toBeGreaterThan(0);
        });
    }
);

it("reload discovers an extension added after session creation", { timeout: 120_000 }, async () => {
    await withExtensionSession(async (session) => {
        const name = await installExtension(
            join(env.COPILOT_HOME, "extensions"),
            `new-${randomUUID()}`
        );
        await retry("reload after extension controller initialization", async () => {
            await session.rpc.extensions.reload();
        });
        expect((await waitForExtension(session, `user:${name}`, "running")).source).toBe("user");
    });
});

it("reports a failed extension without losing other extensions", { timeout: 120_000 }, async () => {
    const base = join(env.COPILOT_HOME, "extensions");
    const bad = await installExtension(
        base,
        `failing-${randomUUID()}`,
        "throw new Error('intentional startup failure');"
    );
    const good = await installExtension(base, `running-${randomUUID()}`);
    await withExtensionSession(async (session) => {
        expect((await waitForExtension(session, `user:${bad}`, "failed")).source).toBe("user");
        expect((await waitForExtension(session, `user:${good}`, "running")).pid).toBeGreaterThan(0);
    });
});

it(
    "persists server extension enablement only for future sessions",
    { timeout: 120_000 },
    async () => {
        const name = await installExtension(
            join(env.COPILOT_HOME, "extensions"),
            `persistent-${randomUUID()}`
        );
        const id = `user:${name}`;
        const client = createClient({ extensionLaunchProvider });
        const config = {
            enableConfigDiscovery: true,
            extensionSdkPath: sdkDistDirectory,
            disabledMcpServers: ["github-mcp-server"],
            onPermissionRequest: () => ({
                kind: "approve-once" as const,
                approvedInteractively: true,
            }),
        };
        try {
            const active = await client.createSession(config);
            try {
                await waitForExtension(active, id, "running");
                await client.rpc.user.settings.reload();
                expect((await client.rpc.extensions.discover()).extensions).toEqual(
                    expect.arrayContaining([
                        expect.objectContaining({ id, enabled: true, source: "user" }),
                    ])
                );
                expect((await client.rpc.plugins.list()).plugins).toEqual([]);

                await client.rpc.extensions.disable({ ids: [id] });
                expect((await waitForExtension(active, id, "running")).pid).toBeGreaterThan(0);

                const disabledSession = await client.createSession(config);
                try {
                    expect(
                        (await waitForExtension(disabledSession, id, "disabled")).pid ?? null
                    ).toBeNull();
                    await client.rpc.extensions.enable({ ids: [id] });
                    expect(
                        (await waitForExtension(disabledSession, id, "disabled")).pid ?? null
                    ).toBeNull();

                    const enabledSession = await client.createSession(config);
                    try {
                        expect(
                            (await waitForExtension(enabledSession, id, "running")).pid
                        ).toBeGreaterThan(0);
                    } finally {
                        await enabledSession.disconnect();
                    }
                } finally {
                    await disabledSession.disconnect();
                }
            } finally {
                await active.disconnect();
            }
        } finally {
            expect(await client.stop()).toEqual([]);
        }
    }
);
