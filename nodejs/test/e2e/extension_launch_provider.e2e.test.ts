/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { existsSync, readFileSync } from "node:fs";
import { copyFile, mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it, onTestFinished } from "vitest";
import {
    approveAll,
    CopilotClient,
    CopilotRequestHandler,
    RuntimeConnection,
    type CopilotSession,
    type CopilotWebSocketHandler,
    type ExtensionLaunchProvider,
    type ExtensionLaunchProviderResolveRequest,
    type ExtensionLaunchProviderResolveResult,
    type SessionConfig,
} from "../../src/index.js";
import { retry } from "./harness/sdkTestHelper.js";

// The published runtime does not yet promise this experimental contract. Opt in
// with an explicit Node CLI entry, not a version string or the standalone wrapper.
const runtimePath = process.env.COPILOT_EXTENSION_LAUNCH_TEST_CLI;
const sdkPath = fileURLToPath(new URL("../../dist/", import.meta.url));
const scratchPath = fileURLToPath(new URL("../../../.local/canvas-tests/", import.meta.url));
const extensionFixture = fileURLToPath(
    new URL("./fixtures/launch-provider-extension.mjs", import.meta.url)
);
const extensionId = "plugin:launch-provider:marker";

class OfflineRequests extends CopilotRequestHandler {
    requests = 0;

    protected override async sendRequest(): Promise<Response> {
        this.requests++;
        throw new Error("This test must not issue a model request");
    }

    protected override async openWebSocket(): Promise<CopilotWebSocketHandler> {
        this.requests++;
        throw new Error("This test must not open a model WebSocket");
    }
}

function readPids(directory: string, marker = "startup-pids"): number[] {
    const path = join(directory, marker);
    return existsSync(path)
        ? readFileSync(path, "utf8").trim().split("\n").filter(Boolean).map(Number)
        : [];
}

function isRunning(pid: number): boolean {
    try {
        process.kill(pid, 0);
        return true;
    } catch {
        return false;
    }
}

async function createLaunchContext() {
    if (!runtimePath) {
        throw new Error("Set COPILOT_EXTENSION_LAUNCH_TEST_CLI to the runtime Node CLI entry");
    }
    if (!existsSync(join(sdkPath, "extension.js"))) {
        throw new Error("Build the SDK with npm run build before running this suite");
    }
    await mkdir(scratchPath, { recursive: true });
    const root = await mkdtemp(join(scratchPath, "launch-provider-"));
    const home = join(root, "home");
    const copilotHome = join(root, "copilot-home");
    const workspace = join(root, "workspace");
    const dataDirectory = join(root, "data");
    const pluginDirectory = join(root, "installed", "revision-one");
    const extensionDirectory = join(pluginDirectory, "extensions", "marker");
    await Promise.all(
        [home, copilotHome, workspace, dataDirectory, extensionDirectory].map((path) =>
            mkdir(path, { recursive: true })
        )
    );
    await writeFile(
        join(pluginDirectory, "plugin.json"),
        JSON.stringify({ name: "launch-provider", version: "1.0.0" })
    );
    await copyFile(extensionFixture, join(extensionDirectory, "extension.mjs"));
    execFileSync("git", ["init", "--quiet"], { cwd: workspace });

    const requests = new OfflineRequests();
    const clients: CopilotClient[] = [];
    onTestFinished(async () => {
        try {
            for (const client of clients) {
                await client.stop();
            }
            await retry("reap fixture processes", async () => {
                expect(readPids(extensionDirectory).filter(isRunning)).toEqual([]);
            });
            expect(requests.requests).toBe(0);
        } finally {
            await rm(root, { recursive: true, force: true, maxRetries: 20, retryDelay: 100 });
        }
    });

    const createClient = (provider?: ExtensionLaunchProvider) => {
        const client = new CopilotClient({
            connection: RuntimeConnection.forStdio({ path: runtimePath }),
            extensionLaunchProvider: provider,
            mode: "empty",
            baseDirectory: copilotHome,
            useLoggedInUser: false,
            workingDirectory: workspace,
            requestHandler: requests,
            env: {
                PATH: process.env.PATH ?? "",
                ...(process.env.SystemRoot ? { SystemRoot: process.env.SystemRoot } : {}),
                HOME: home,
                USERPROFILE: home,
                COPILOT_HOME: copilotHome,
                GH_CONFIG_DIR: home,
                XDG_CONFIG_HOME: home,
                XDG_STATE_HOME: home,
                XDG_CACHE_HOME: home,
                APPDATA: home,
                LOCALAPPDATA: home,
                TMPDIR: root,
                TEMP: root,
                TMP: root,
                COPILOT_DISABLE_KEYTAR: "1",
                COPILOT_CLI_ENABLED_FEATURE_FLAGS: "EXTENSIONS",
                COPILOT_OTEL_ENABLED: "false",
                DO_NOT_TRACK: "1",
            },
        });
        clients.push(client);
        return client;
    };

    const sessionConfig: SessionConfig = {
        onPermissionRequest: approveAll,
        availableTools: [],
        workingDirectory: workspace,
        enableExperimentalMode: true,
        requestExtensions: true,
        pluginDirectories: [pluginDirectory],
        extensionSdkPath: sdkPath,
    };
    const approve = (request: ExtensionLaunchProviderResolveRequest) => {
        if (request.id !== extensionId || !request.defaultLaunch) {
            return { launch: null };
        }
        return {
            launch: {
                ...request.defaultLaunch,
                env: {
                    ...request.defaultLaunch.env,
                    VSCODE_CANVAS_DATA_DIR: dataDirectory,
                },
            },
        };
    };
    const waitForLaunch = async (session: CopilotSession, count: number) => {
        await retry("start the approved extension", async () => {
            const pids = readPids(extensionDirectory, "ready-pids");
            expect(pids).toHaveLength(count);
            const extensions = await session.rpc.extensions.list();
            expect(extensions.extensions).toContainEqual(
                expect.objectContaining({
                    id: extensionId,
                    status: "running",
                    pid: pids.at(-1),
                })
            );
        });
    };
    const expectDenied = async (session: CopilotSession) => {
        await retry(
            "settle denied extension startup",
            async () => {
                const extensions = await session.rpc.extensions.list();
                expect(extensions.extensions).toContainEqual(
                    expect.objectContaining({ id: extensionId, status: "failed" })
                );
            },
            200
        );
        expect(readPids(extensionDirectory)).toEqual([]);
    };

    return {
        createClient,
        sessionConfig,
        approve,
        waitForLaunch,
        expectDenied,
        extensionDirectory,
        dataDirectory,
        workspace,
        copilotHome,
        requests,
    };
}

describe.skipIf(!runtimePath)("Extension launch provider — real Node runtime", () => {
    it("approves the runtime bootstrap on create, reload, and cold resume", async () => {
        const context = await createLaunchContext();
        const calls: ExtensionLaunchProviderResolveRequest[] = [];
        const client = context.createClient((request) => {
            calls.push(request);
            return context.approve(request);
        });
        const session = await client.createSession(context.sessionConfig);
        await context.waitForLaunch(session, 1);
        expect(calls).toEqual([
            expect.objectContaining({
                id: extensionId,
                sessionId: session.sessionId,
                source: "plugin",
                modulePath: join(context.extensionDirectory, "extension.mjs"),
                defaultLaunch: expect.objectContaining({
                    executable: expect.any(String),
                    args: expect.arrayContaining([
                        expect.stringContaining("extension_bootstrap.mjs"),
                    ]),
                    env: expect.objectContaining({
                        COPILOT_SDK_PATH: sdkPath,
                        SESSION_ID: session.sessionId,
                    }),
                }),
            }),
        ]);
        await session.rpc.retain();
        await session.rpc.extensions.reload();
        await context.waitForLaunch(session, 2);
        expect(calls).toHaveLength(2);
        expect(await client.stop()).toEqual([]);
        await retry("stop pre-resume extension processes", async () => {
            expect(readPids(context.extensionDirectory).filter(isRunning)).toEqual([]);
        });

        const resumed = await client.resumeSession(session.sessionId, context.sessionConfig);
        await context.waitForLaunch(resumed, 3);
        expect(calls.map((request) => request.sessionId)).toEqual([
            session.sessionId,
            session.sessionId,
            session.sessionId,
        ]);
        expect(new Set(readPids(context.extensionDirectory)).size).toBe(3);
        expect(await resumed.getEvents()).not.toContainEqual(
            expect.objectContaining({ type: "user.message" })
        );
    });

    it.each(["omitted", "null", "sync-error", "async-error"])(
        "never launches the default process after %s",
        async (outcome) => {
            const context = await createLaunchContext();
            const calls: ExtensionLaunchProviderResolveRequest[] = [];
            const client = context.createClient((request) => {
                calls.push(request);
                if (outcome === "sync-error") {
                    throw new Error("Fixture approval lookup failed");
                }
                if (outcome === "async-error") {
                    return Promise.reject(new Error("Fixture approval lookup failed"));
                }
                return outcome === "null" ? { launch: null } : {};
            });
            const session = await client.createSession(context.sessionConfig);
            await context.expectDenied(session);
            expect(calls).toHaveLength(1);
            expect(calls[0].defaultLaunch).toBeDefined();
            await session.rpc.extensions.reload();
            await context.expectDenied(session);
            expect(calls).toHaveLength(2);
        }
    );

    it("leaves the built-in launcher unchanged when the provider option is absent", async () => {
        const context = await createLaunchContext();
        const client = context.createClient();
        const session = await client.createSession(context.sessionConfig);
        await context.waitForLaunch(session, 1);
        expect(readPids(context.extensionDirectory)).toHaveLength(1);
    });

    it("denies a timed-out resolver without launching the default profile", async () => {
        const context = await createLaunchContext();
        const calls: ExtensionLaunchProviderResolveRequest[] = [];
        const client = context.createClient((request) => {
            calls.push(request);
            return new Promise<never>(() => {});
        });
        const session = await client.createSession(context.sessionConfig);
        await context.expectDenied(session);
        expect(calls).toHaveLength(1);
        expect(calls[0].defaultLaunch).toBeDefined();
    });

    it("does not carry an outstanding callback across shutdown and process replacement", async () => {
        const context = await createLaunchContext();
        const calls: ExtensionLaunchProviderResolveRequest[] = [];
        let release!: (result: ExtensionLaunchProviderResolveResult) => void;
        const pending = new Promise<ExtensionLaunchProviderResolveResult>((resolve) => {
            release = resolve;
        });
        let block = true;
        const client = context.createClient((request) => {
            calls.push(request);
            return block ? pending : context.approve(request);
        });
        const creating = client.createSession(context.sessionConfig);
        const settled = Promise.allSettled([creating]);
        await retry("receive the blocked callback", async () => {
            expect(calls).toHaveLength(1);
        });
        await client.forceStop();
        await settled;
        expect(readPids(context.extensionDirectory)).toEqual([]);

        block = false;
        await client.start();
        release(context.approve(calls[0]));
        const replacement = await client.createSession(context.sessionConfig);
        await context.waitForLaunch(replacement, 1);
        expect(calls).toHaveLength(2);
        expect(calls[0].sessionId).not.toBe(replacement.sessionId);
        expect(calls[1].sessionId).toBe(replacement.sessionId);
        expect(readPids(context.extensionDirectory)).toHaveLength(1);
    });

    it("retains non-chat effects across stop and cold resume without inventing a turn", async () => {
        const context = await createLaunchContext();
        const client = context.createClient(context.approve);
        const session = await client.createSession(context.sessionConfig);
        await context.waitForLaunch(session, 1);
        const retention: Promise<void> = session.rpc.retain();
        await retention;
        await session.rpc.retain();
        await session.rpc.canvas.open({
            canvasId: "launch-marker",
            instanceId: "saved-open",
        });
        await expect(
            session.rpc.canvas.open({
                canvasId: "launch-marker",
                instanceId: "failed-open",
                input: { fail: true },
            })
        ).rejects.toThrow("Fixture open failed after saving data");
        await session.abort();

        const events = await session.getEvents();
        const retained = events.filter((event) => event.type === "session.retained");
        expect(retained).toEqual([expect.objectContaining({ data: {} })]);
        expect(events.filter((event) => /^(user|assistant)\./.test(event.type))).toEqual([]);
        expect(await client.stop()).toEqual([]);
        expect(readFileSync(join(context.dataDirectory, "operations"), "utf8")).toBe(
            "saved-open\nfailed-open\n"
        );

        const freshClient = context.createClient(context.approve);
        const resumed = await freshClient.resumeSession(session.sessionId, context.sessionConfig);
        await context.waitForLaunch(resumed, 2);
        await resumed.rpc.retain();
        const restoredEvents = await resumed.getEvents();
        expect(restoredEvents.filter((event) => event.type === "session.retained")).toEqual(
            retained
        );
        expect(restoredEvents.filter((event) => /^(user|assistant)\./.test(event.type))).toEqual(
            []
        );
        expect((await freshClient.listSessions()).map((entry) => entry.sessionId)).toContain(
            session.sessionId
        );
    });

    it.each([false, true])(
        "flushes retention before the first package startup (defer plugin roots: %s)",
        async (deferPluginRoots) => {
            const context = await createLaunchContext();
            const sessionId = randomUUID();
            const config: SessionConfig = { ...context.sessionConfig, sessionId };
            await writeFile(
                join(context.extensionDirectory, "retention-events-path"),
                join(context.copilotHome, "session-state", sessionId, "events.jsonl")
            );
            const calls: ExtensionLaunchProviderResolveRequest[] = [];
            let launchAdmitted = false;
            const client = context.createClient((request) => {
                calls.push(request);
                return launchAdmitted ? context.approve(request) : { launch: null };
            });

            const inert = await client.createSession({
                ...config,
                requestExtensions: false,
                pluginDirectories: deferPluginRoots ? [] : config.pluginDirectories,
            });
            await expect(inert.rpc.extensions.list()).resolves.toEqual({ extensions: [] });
            expect(calls).toEqual([]);
            expect(readPids(context.extensionDirectory)).toEqual([]);
            await inert.rpc.retain();
            const retained = (await inert.getEvents()).filter(
                (event) => event.type === "session.retained"
            );
            expect(retained).toEqual([expect.objectContaining({ data: {} })]);
            await inert.disconnect();

            launchAdmitted = true;
            const active = await client.resumeSession(sessionId, config);
            await context.waitForLaunch(active, 1);
            expect(readPids(context.extensionDirectory, "retained-startup-pids")).toEqual(
                readPids(context.extensionDirectory)
            );
            expect(calls.map((request) => request.sessionId)).toEqual([sessionId]);
            const events = await active.getEvents();
            expect(events.filter((event) => event.type === "session.retained")).toEqual(retained);
            expect(events.filter((event) => /^(user|assistant)\./.test(event.type))).toEqual([]);
        }
    );

    it.each([false, true])(
        "retains by ID inside the resolver before initial create completes (yield: %s)",
        async (yieldBeforeRetain) => {
            const context = await createLaunchContext();
            const sessionId = randomUUID();
            await writeFile(
                join(context.extensionDirectory, "retention-events-path"),
                join(context.copilotHome, "session-state", sessionId, "events.jsonl")
            );
            let operationResolved = false;
            const observedResolved: boolean[] = [];
            const client: CopilotClient = context.createClient(async (request) => {
                if (!request.sessionId || !request.defaultLaunch) {
                    return { launch: null };
                }
                observedResolved.push(operationResolved);
                if (yieldBeforeRetain) {
                    await new Promise((resolve) => setImmediate(resolve));
                }
                await client.retainSession(request.sessionId);
                return context.approve(request);
            });
            const config: SessionConfig = { ...context.sessionConfig, sessionId };
            const session = await client.createSession(config);
            operationResolved = true;
            await context.waitForLaunch(session, 1);
            expect(observedResolved).toEqual([false]);
            expect(readPids(context.extensionDirectory, "retained-startup-pids")).toEqual(
                readPids(context.extensionDirectory)
            );
            const retained = (await session.getEvents()).filter(
                (event) => event.type === "session.retained"
            );
            expect(retained).toEqual([expect.objectContaining({ data: {} })]);
            expect(await client.stop()).toEqual([]);

            operationResolved = false;
            const resumed = await client.resumeSession(sessionId, config);
            operationResolved = true;
            await context.waitForLaunch(resumed, 2);
            expect(observedResolved).toHaveLength(2);
            expect(readPids(context.extensionDirectory, "retained-startup-pids")).toEqual(
                readPids(context.extensionDirectory)
            );
            const events = await resumed.getEvents();
            expect(events.filter((event) => event.type === "session.retained")).toEqual(retained);
            expect(events.filter((event) => /^(user|assistant)\./.test(event.type))).toEqual([]);
        }
    );

    it("does not launch when the resolver's retain-by-ID call fails", async () => {
        const context = await createLaunchContext();
        const client: CopilotClient = context.createClient(async (request) => {
            await client.retainSession("missing-runtime-session");
            return context.approve(request);
        });
        const session = await client.createSession(context.sessionConfig);
        await context.expectDenied(session);
        expect(
            (await session.getEvents()).filter((event) => event.type === "session.retained")
        ).toEqual([]);
    });

    it("keeps ordinary unused sessions ephemeral", async () => {
        const context = await createLaunchContext();
        const client = context.createClient();
        const session = await client.createSession({
            ...context.sessionConfig,
            sessionId: randomUUID(),
            requestExtensions: false,
            pluginDirectories: [],
        });
        expect(await client.stop()).toEqual([]);
        const freshClient = context.createClient();
        await freshClient.start();
        expect((await freshClient.listSessions()).map((entry) => entry.sessionId)).not.toContain(
            session.sessionId
        );
        await expect(
            freshClient.resumeSession(session.sessionId, {
                ...context.sessionConfig,
                requestExtensions: false,
                pluginDirectories: [],
            })
        ).rejects.toThrow();
    });
});
