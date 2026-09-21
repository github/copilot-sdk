/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { MemoryProvider } from "@platformatic/vfs";
import { existsSync } from "node:fs";
import { mkdir, mkdtemp, readFile, realpath, rm, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { createRequire } from "node:module";
import { dirname, isAbsolute, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
    approveAll,
    CopilotClient,
    RuntimeConnection,
    type CopilotClientOptions,
    type CopilotSession,
    type SessionConfig,
    type SessionFsProvider,
} from "../../src/index.js";

const testDirectory = dirname(fileURLToPath(import.meta.url));
const startupPrompts = ["HOST_SETTINGS_STARTUP_SENTINEL", "HOST_DIRECTORY_STARTUP_SENTINEL"];
const rejectedInference = "Host hook regression intentionally has no model backend";
const require = createRequire(import.meta.url);
type Transport = "stdio" | "external" | "inprocess";

// Unlike the replay harness, this suite never downloads or falls back to a bundled
// runtime. Point COPILOT_CLI_PATH at the locally built native copilot-runtime wrapper
// with the matching runtime.node beside it (also used by the FFI transport).
function nativeRuntimePath(): string {
    const path = process.env.COPILOT_CLI_PATH;
    if (!path || !isAbsolute(path) || !existsSync(path) || /\.(?:[cm]?js)$/i.test(path)) {
        throw new Error(
            "host_user_hooks requires COPILOT_CLI_PATH to be an absolute path to the " +
                "locally modified native copilot-runtime executable, not a bundled or JS runtime."
        );
    }
    if (!existsSync(join(dirname(path), "runtime.node"))) {
        throw new Error("Build/stage the matching runtime.node beside COPILOT_CLI_PATH.");
    }
    return path;
}

async function createFixture(transport: Transport) {
    const runtimePath = nativeRuntimePath();
    // Keep even scratch homes inside the checkout, never in the developer's home.
    const root = await realpath(await mkdtemp(join(testDirectory, ".host-user-hooks-")));
    const home = join(root, "home");
    const config = join(home, ".copilot");
    const work = join(root, "work");
    const scratch = join(root, "scratch");
    const marker = join(root, "commands.jsonl");
    const httpHooks: string[] = [];
    const inferenceRequests: string[] = [];
    const server = createServer((request, response) => {
        request.resume();
        if (request.url?.startsWith("/hooks/")) {
            httpHooks.push(request.url);
            response.writeHead(200, { "Content-Type": "application/json" });
            response.end("{}");
        } else {
            inferenceRequests.push(request.url ?? "");
            response.writeHead(400, { "Content-Type": "application/json" });
            response.end(
                JSON.stringify({
                    error: { message: rejectedInference, type: "invalid_request_error" },
                })
            );
        }
    });
    const clients: CopilotClient[] = [];
    const savedEnvironment = { ...process.env };
    let environmentChanged = false;
    async function dispose() {
        try {
            for (const client of clients.toReversed()) {
                await client.forceStop();
            }
        } finally {
            server.closeAllConnections();
            if (server.listening) {
                await new Promise<void>((done, reject) =>
                    server.close((error) => (error ? reject(error) : done()))
                );
            }
            if (environmentChanged) {
                for (const key of Object.keys(process.env)) delete process.env[key];
                Object.assign(process.env, savedEnvironment);
            }
            await rm(root, { recursive: true, force: true });
        }
    }
    try {
        await Promise.all([
            mkdir(join(config, "hooks"), { recursive: true }),
            mkdir(work, { recursive: true }),
            mkdir(scratch, { recursive: true }),
        ]);
        await new Promise<void>((done, reject) => {
            server.once("error", reject);
            server.listen(0, "127.0.0.1", done);
        });
        const address = server.address();
        if (!address || typeof address === "string") throw new Error("Missing fixture port");
        const endpoint = `http://127.0.0.1:${address.port}`;
        const script = join(root, "mark.cjs");
        await writeFile(
            script,
            "require('node:fs').appendFileSync(process.argv[2], process.argv[3] + '\\n');\n"
        );
        const hooks = (source: string, prompt: string) => ({
            sessionStart: [{ type: "prompt", prompt }],
            userPromptSubmitted: [
                {
                    type: "command",
                    exec: process.execPath,
                    args: [script, marker, source],
                    timeoutSec: 5,
                },
                { type: "http", url: `${endpoint}/hooks/${source}`, timeoutSec: 5 },
            ],
        });
        await Promise.all([
            writeFile(
                join(config, "settings.json"),
                JSON.stringify({ hooks: hooks("settings", startupPrompts[0]) })
            ),
            writeFile(
                join(config, "hooks", "host-fixture.json"),
                JSON.stringify({ version: 1, hooks: hooks("directory", startupPrompts[1]) })
            ),
        ]);
        // An allowlist, not {...process.env}: no real tokens, proxy credentials,
        // credential-helper configuration, or ambient runtime settings reach the host.
        const env: Record<string, string> = {};
        for (const name of ["PATH", "SystemRoot", "WINDIR", "COMSPEC", "PATHEXT", "LANG"]) {
            if (process.env[name]) env[name] = process.env[name]!;
        }
        Object.assign(env, {
            HOME: home,
            USERPROFILE: home,
            COPILOT_HOME: config,
            COPILOT_CLI_PATH: runtimePath,
            COPILOT_DISABLE_KEYTAR: "1",
            COPILOT_HOOK_ALLOW_LOCALHOST: "1",
            GH_CONFIG_DIR: join(home, ".config", "gh"),
            XDG_CONFIG_HOME: join(home, ".config"),
            XDG_STATE_HOME: join(home, ".state"),
            XDG_CACHE_HOME: join(home, ".cache"),
            TMPDIR: scratch,
            TMP: scratch,
            TEMP: scratch,
            GIT_CONFIG_NOSYSTEM: "1",
            GIT_CONFIG_GLOBAL: join(home, ".gitconfig"),
            COPILOT_API_URL: endpoint,
            COPILOT_DEBUG_GITHUB_API_URL: endpoint,
            NO_PROXY: "127.0.0.1,localhost",
        });
        if (transport === "inprocess") {
            environmentChanged = true;
            for (const key of Object.keys(process.env)) delete process.env[key];
            Object.assign(process.env, env);
        }
        const provider = {
            type: "openai" as const,
            wireApi: "completions" as const,
            baseUrl: `${endpoint}/v1`,
            apiKey: "synthetic-local-only",
        };
        const common: SessionConfig = {
            onPermissionRequest: approveAll,
            availableTools: [],
            model: "gpt-4.1",
            provider,
            workingDirectory: work,
            configDirectory: config,
            enableConfigDiscovery: false,
            enableFileHooks: false,
            enableSkills: false,
            enableSessionStore: false,
            enableHostGitOperations: false,
            enableSessionTelemetry: false,
            enableManagedSettings: false,
            infiniteSessions: { enabled: false },
        };
        async function createClient(
            mode: CopilotClientOptions["mode"] = "empty",
            sessionFs = false
        ) {
            let connection =
                transport === "inprocess"
                    ? RuntimeConnection.forInProcess()
                    : RuntimeConnection.forStdio({ path: runtimePath });
            if (transport === "external") {
                const connectionToken = "host-hook-fixture-token";
                const host = new CopilotClient({
                    connection: RuntimeConnection.forTcp({ path: runtimePath, connectionToken }),
                    env,
                    useLoggedInUser: false,
                    logLevel: "error",
                });
                clients.push(host);
                await host.start();
                // Same SDK-owned TCP fixture pattern as session_fs.e2e.test.ts.
                const { runtimePort } = host as unknown as { runtimePort: number };
                connection = RuntimeConnection.forUri(`127.0.0.1:${runtimePort}`, {
                    connectionToken,
                });
            }
            const client = new CopilotClient({
                connection,
                ...(transport === "inprocess" ? {} : { env }),
                mode,
                baseDirectory: config,
                ...(transport === "external" ? {} : { useLoggedInUser: false }),
                logLevel: "error",
                ...(sessionFs
                    ? {
                          sessionFs: {
                              initialCwd: work.replaceAll("\\", "/"),
                              sessionStatePath: join(root, "virtual-state").replaceAll("\\", "/"),
                              conventions: "posix" as const,
                          },
                      }
                    : {}),
            });
            clients.push(client);
            await client.start();
            return client;
        }
        return {
            root,
            transport,
            config,
            work,
            common,
            createClient,
            dispose,
            httpHooks,
            inferenceRequests,
            async commands() {
                return existsSync(marker)
                    ? (await readFile(marker, "utf8")).trim().split("\n")
                    : [];
            },
        };
    } catch (error) {
        await dispose();
        throw error;
    }
}

function callbackLog() {
    const calls: { sessionId: string; prompt: string }[] = [];
    const hooks: NonNullable<SessionConfig["hooks"]> = {
        onUserPromptSubmitted: (input, invocation) => {
            calls.push({ sessionId: invocation.sessionId, prompt: input.prompt });
        },
    };
    return { calls, hooks };
}

async function expectNativeHookState(transport: Transport, sessionId: string, enabled: boolean) {
    if (transport !== "inprocess") return;
    // This existing export inspects the same Rust state used by the FFI session.
    const native = require(join(dirname(nativeRuntimePath()), "runtime.node")) as {
        hookSessionSnapshot(sessionId: string): Promise<string>;
    };
    const snapshot = JSON.parse(await native.hookSessionSnapshot(sessionId));
    expect(snapshot.hookCount).toBe(enabled ? 4 : 0);
    expect(snapshot.startupPrompts.sort()).toEqual(enabled ? [...startupPrompts].sort() : []);
}

async function expectHookTurn(
    fixture: Awaited<ReturnType<typeof createFixture>>,
    session: CopilotSession,
    callbacks: ReturnType<typeof callbackLog>,
    prompt: string,
    enabled: boolean
) {
    const commandsBefore = await fixture.commands();
    const httpBefore = [...fixture.httpHooks];
    const callbacksBefore = callbacks.calls.length;
    const inferenceBefore = fixture.inferenceRequests.length;
    await expectNativeHookState(fixture.transport, session.sessionId, enabled);
    let idle = false;
    const unsubscribe = session.on("session.idle", () => {
        idle = true;
    });
    try {
        await expect(session.sendAndWait({ prompt })).rejects.toThrow(rejectedInference);
        // session.error precedes session.idle. Do not resume, patch, or start the
        // next turn until the rejected turn has fully drained.
        await expect.poll(() => idle, { timeout: 10_000 }).toBe(true);
    } finally {
        unsubscribe();
    }
    const commandsAfter = await fixture.commands();
    expect(commandsAfter.slice(0, commandsBefore.length)).toEqual(commandsBefore);
    expect(commandsAfter.slice(commandsBefore.length).sort()).toEqual(
        enabled ? ["directory", "settings"] : []
    );
    expect(fixture.httpHooks.slice(0, httpBefore.length)).toEqual(httpBefore);
    expect(fixture.httpHooks.slice(httpBefore.length).sort()).toEqual(
        enabled ? ["/hooks/directory", "/hooks/settings"] : []
    );
    expect(callbacks.calls.slice(callbacksBefore)).toEqual([
        { sessionId: session.sessionId, prompt },
    ]);
    expect(fixture.inferenceRequests.slice(inferenceBefore)).toEqual(["/v1/chat/completions"]);
}

function memoryFs(): { provider: SessionFsProvider; writes: string[] } {
    const memory = new MemoryProvider();
    const writes: string[] = [];
    return {
        writes,
        provider: {
            readFile: async (path) => (await memory.readFile(path, "utf8")) as string,
            writeFile: async (path, content) => {
                writes.push(path);
                await memory.writeFile(path, content);
            },
            appendFile: async (path, content) => {
                writes.push(path);
                await memory.appendFile(path, content);
            },
            exists: (path) => memory.exists(path),
            stat: async (path) => {
                const stat = await memory.stat(path);
                return {
                    isFile: stat.isFile(),
                    isDirectory: stat.isDirectory(),
                    size: stat.size,
                    mtime: new Date(stat.mtimeMs).toISOString(),
                    birthtime: new Date(stat.birthtimeMs).toISOString(),
                };
            },
            mkdir: async (path, recursive, mode) => {
                await memory.mkdir(path, { recursive, mode });
            },
            readdir: async (path) => (await memory.readdir(path)) as string[],
            readdirWithTypes: async (path) =>
                Promise.all(
                    ((await memory.readdir(path)) as string[]).map(async (name) => ({
                        name,
                        type: (await memory.stat(`${path}/${name}`)).isDirectory()
                            ? ("directory" as const)
                            : ("file" as const),
                    }))
                ),
            rm: async (path, _recursive, force) => {
                if (!force || (await memory.exists(path))) await memory.unlink(path);
            },
            rename: (from, to) => memory.rename(from, to),
        },
    };
}

describe.each<Transport>(["stdio", "external", "inprocess"])(
    "Host user hooks (%s)",
    (transport) => {
        it.each([
            { mode: "empty" as const, enableHostUserHooks: undefined, enabled: false },
            { mode: "empty" as const, enableHostUserHooks: false, enabled: false },
            { mode: "empty" as const, enableHostUserHooks: true, enabled: true },
            { mode: "copilot-cli" as const, enableHostUserHooks: undefined, enabled: true },
            { mode: "copilot-cli" as const, enableHostUserHooks: false, enabled: false },
            { mode: "copilot-cli" as const, enableHostUserHooks: true, enabled: true },
        ])(
            "$mode + SessionFs honors enableHostUserHooks=$enableHostUserHooks and SDK callbacks",
            async ({ mode, enableHostUserHooks, enabled }) => {
                const fixture = await createFixture(transport);
                try {
                    const client = await fixture.createClient(mode, true);
                    const fs = memoryFs();
                    await fs.provider.mkdir(fixture.work.replaceAll("\\", "/"), true);
                    const callbacks: { sessionId: string; prompt: string }[] = [];
                    const discovery = await client.rpc.hooks.discover({});
                    // Discovery is server-scoped, NOT a session admission check. Even
                    // disabled sessions have a discoverable positive-control fixture.
                    expect(discovery.hooks.filter((hook) => hook.origin === "user")).toHaveLength(
                        6
                    );
                    const session = await client.createSession({
                        ...fixture.common,
                        ...(enableHostUserHooks === undefined ? {} : { enableHostUserHooks }),
                        createSessionFsProvider: () => fs.provider,
                        hooks: {
                            onUserPromptSubmitted: (input, invocation) => {
                                callbacks.push({
                                    sessionId: invocation.sessionId,
                                    prompt: input.prompt,
                                });
                            },
                        },
                    });
                    await expectNativeHookState(transport, session.sessionId, enabled);
                    // A local HTTP error is not a recorded/fabricated model response.
                    // The turn exercises the real Rust hook runner before inference fails.
                    await expect(
                        session.sendAndWait({ prompt: "Exercise hooks without model inference." })
                    ).rejects.toThrow(rejectedInference);
                    expect(callbacks).toEqual([
                        {
                            sessionId: session.sessionId,
                            prompt: "Exercise hooks without model inference.",
                        },
                    ]);
                    expect((await fixture.commands()).sort()).toEqual(
                        enabled ? ["directory", "settings"] : []
                    );
                    expect(fixture.httpHooks.sort()).toEqual(
                        enabled ? ["/hooks/directory", "/hooks/settings"] : []
                    );
                    expect(fixture.inferenceRequests).toEqual(["/v1/chat/completions"]);
                    await session.disconnect();
                    expect(fs.writes.length).toBeGreaterThan(0);
                    expect(existsSync(join(fixture.root, "virtual-state"))).toBe(false);
                } finally {
                    await fixture.dispose();
                }
            }
        );

        it(
            "isolates interleaved sessions through live updates, unrelated patches, and reloads",
            { timeout: 60_000 },
            async () => {
                const fixture = await createFixture(transport);
                try {
                    const client = await fixture.createClient();
                    const callbacks = callbackLog();
                    const initiallyEnabled = await client.createSession({
                        ...fixture.common,
                        enableHostUserHooks: true,
                        hooks: callbacks.hooks,
                    });
                    const initiallyDisabled = await client.createSession({
                        ...fixture.common,
                        enableHostUserHooks: false,
                        hooks: callbacks.hooks,
                    });
                    await expectHookTurn(
                        fixture,
                        initiallyEnabled,
                        callbacks,
                        "enabled positive control",
                        true
                    );
                    await expectHookTurn(
                        fixture,
                        initiallyDisabled,
                        callbacks,
                        "disabled neighbor",
                        false
                    );
                    await expectHookTurn(
                        fixture,
                        initiallyEnabled,
                        callbacks,
                        "enabled after disabled neighbor",
                        true
                    );

                    expect(
                        await initiallyEnabled.rpc.options.update({ enableHostUserHooks: false })
                    ).toMatchObject({ success: true });
                    expect(
                        await initiallyDisabled.rpc.options.update({ enableHostUserHooks: true })
                    ).toMatchObject({ success: true });
                    await expectHookTurn(
                        fixture,
                        initiallyEnabled,
                        callbacks,
                        "disabled by live patch",
                        false
                    );
                    await expectHookTurn(
                        fixture,
                        initiallyDisabled,
                        callbacks,
                        "enabled by live patch",
                        true
                    );

                    for (const session of [initiallyEnabled, initiallyDisabled]) {
                        expect(
                            await session.rpc.options.update({ clientName: "host-hooks-patched" })
                        ).toMatchObject({ success: true });
                    }
                    await expectHookTurn(
                        fixture,
                        initiallyDisabled,
                        callbacks,
                        "unrelated patch preserves true",
                        true
                    );
                    await expectHookTurn(
                        fixture,
                        initiallyEnabled,
                        callbacks,
                        "unrelated patch preserves false",
                        false
                    );

                    for (const session of [initiallyEnabled, initiallyDisabled]) {
                        await session.rpc.plugins.reload({
                            reloadHooks: true,
                            reloadMcp: false,
                            reloadCustomAgents: false,
                            reloadExtensions: false,
                            deferRepoHooks: true,
                        });
                    }
                    await expectHookTurn(
                        fixture,
                        initiallyEnabled,
                        callbacks,
                        "reload preserves false",
                        false
                    );
                    await expectHookTurn(
                        fixture,
                        initiallyDisabled,
                        callbacks,
                        "reload preserves true",
                        true
                    );
                    await initiallyEnabled.disconnect();
                    await initiallyDisabled.disconnect();
                } finally {
                    await fixture.dispose();
                }
            }
        );

        it(
            "warm resume applies Empty's omitted false and then an explicit true",
            { timeout: 60_000 },
            async () => {
                const fixture = await createFixture(transport);
                try {
                    const client = await fixture.createClient("empty");
                    const callbacks = callbackLog();
                    const original = await client.createSession({
                        ...fixture.common,
                        enableHostUserHooks: true,
                        hooks: callbacks.hooks,
                    });
                    await expectHookTurn(fixture, original, callbacks, "before warm resume", true);
                    // Deliberately do not disconnect: resume must reconcile the
                    // already-live native session, not reconstruct it from disk.
                    const resumed = await client.resumeSession(original.sessionId, {
                        ...fixture.common,
                        hooks: callbacks.hooks,
                    });
                    expect(resumed.sessionId).toBe(original.sessionId);
                    await expectHookTurn(
                        fixture,
                        resumed,
                        callbacks,
                        "warm Empty default is false",
                        false
                    );
                    const reenabled = await client.resumeSession(resumed.sessionId, {
                        ...fixture.common,
                        enableHostUserHooks: true,
                        hooks: callbacks.hooks,
                    });
                    expect(reenabled.sessionId).toBe(original.sessionId);
                    await expectHookTurn(
                        fixture,
                        reenabled,
                        callbacks,
                        "warm explicit true wins",
                        true
                    );
                    await reenabled.disconnect();
                } finally {
                    await fixture.dispose();
                }
            }
        );

        it.each([false, true])(
            "sessions.open queues host startup prompts only when explicitly enabled=%s",
            async (enableHostUserHooks) => {
                const fixture = await createFixture(transport);
                try {
                    const client = await fixture.createClient();
                    // sessions.open exposes startupPrompts; SDK createSession intentionally
                    // does not. Exercise the native initialization route rather than infer
                    // prompt admission from a model's answer or server-scoped discovery.
                    const options = {
                        model: fixture.common.model,
                        provider: fixture.common.provider,
                        configDir: fixture.config,
                        workingDirectory: fixture.work,
                        enableHostUserHooks,
                        enableFileHooks: false,
                        enableManagedSettings: false,
                        enableSkills: false,
                        enableConfigDiscovery: false,
                        enableSessionTelemetry: false,
                    };
                    const opened = await client.rpc.sessions.open({ kind: "create", options });
                    expect(opened.status).toBe("created");
                    expect((opened.startupPrompts ?? []).sort()).toEqual(
                        enableHostUserHooks ? [...startupPrompts].sort() : []
                    );
                    expect(fixture.inferenceRequests).toEqual([]);
                    expect(await fixture.commands()).toEqual([]);
                    expect(fixture.httpHooks).toEqual([]);
                    await client.rpc.sessions.close({ sessionId: opened.sessionId! });
                } finally {
                    await fixture.dispose();
                }
            }
        );
    }
);

describe("Host user hooks (stdio cold resume)", () => {
    it.each([undefined, false])(
        "caller enableHostUserHooks=%s overrides the persisted true after a native restart",
        { timeout: 60_000 },
        async (enableHostUserHooks) => {
            const fixture = await createFixture("stdio");
            try {
                const firstClient = await fixture.createClient("empty");
                const callbacks = callbackLog();
                const session = await firstClient.createSession({
                    ...fixture.common,
                    enableHostUserHooks: true,
                    hooks: callbacks.hooks,
                });
                const originalPrompt = "persist this user message before cold restart";
                await expectHookTurn(fixture, session, callbacks, originalPrompt, true);
                await firstClient.rpc.sessions.save({ sessionId: session.sessionId });
                expect(await firstClient.stop()).toEqual([]);

                // A failed inference still persists the user message. Verify it
                // survives a new native process without inventing an assistant response.
                const restartedClient = await fixture.createClient("empty");
                const resumed = await restartedClient.resumeSession(session.sessionId, {
                    ...fixture.common,
                    ...(enableHostUserHooks === undefined ? {} : { enableHostUserHooks }),
                    hooks: callbacks.hooks,
                });
                expect(resumed.sessionId).toBe(session.sessionId);
                expect(await resumed.getEvents()).toEqual(
                    expect.arrayContaining([
                        expect.objectContaining({
                            type: "user.message",
                            data: expect.objectContaining({ content: originalPrompt }),
                        }),
                    ])
                );
                await expectHookTurn(
                    fixture,
                    resumed,
                    callbacks,
                    "cold caller overrides stored enablement",
                    false
                );
                await resumed.disconnect();
            } finally {
                await fixture.dispose();
            }
        }
    );
});
