/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { SessionCompactionCompleteEvent } from "@github/copilot/sdk";
import { MemoryProvider, VirtualProvider } from "@platformatic/vfs";
import { createHash } from "crypto";
import { existsSync, mkdtempSync, realpathSync } from "fs";
import { mkdir, readFile, readdir, rm, writeFile } from "fs/promises";
import { createServer } from "http";
import { tmpdir } from "os";
import { join } from "path";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import { CopilotClient } from "../../src/client.js";
import { getRuntimePlatform } from "../../src/runtimeArtifacts.js";
import { createSessionFsAdapter, RuntimeConnection } from "../../src/index.js";
import type { SessionFsReaddirWithTypesEntry } from "../../src/generated/rpc.js";
import {
    approveAll,
    CopilotSession,
    defineTool,
    SessionEvent,
    type SessionFsConfig,
    type SessionFsProvider,
    type SessionFsFileInfo,
    type PermissionRequestResult,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const sessionStatePath =
    process.platform === "win32"
        ? "/session-state"
        : join(
              realpathSync(mkdtempSync(join(tmpdir(), "copilot-sessionfs-state-"))),
              "session-state"
          ).replace(/\\/g, "/");

describe("Session Fs", async () => {
    // Single provider for the describe block — session IDs are unique per test,
    // so no cross-contamination between tests.
    const provider = new MemoryProvider();
    const createSessionFsProvider = (session: CopilotSession) =>
        createTestSessionFsHandler(session, provider);

    // Helpers to build session-namespaced paths for direct provider assertions
    const p = (sessionId: string, path: string) =>
        `/${sessionId}${path.startsWith("/") ? path : "/" + path}`;

    const {
        copilotClient: client,
        createClient,
        env,
        openAiEndpoint,
    } = await createSdkTestContext({
        copilotClientOptions: { sessionFs: sessionFsConfig },
    });

    it(
        "should route file operations through the session fs provider",
        { timeout: 60000 },
        async () => {
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                createSessionFsProvider,
            });

            const errors: SessionEvent[] = [];
            session.on((event) => {
                if (event.type === "session.error") {
                    errors.push(event);
                }
            });

            const msg = await session.sendAndWait({ prompt: "What is 100 + 200?" });
            expect(msg?.data.content).toContain("300");
            await session.disconnect();

            const buf = await provider.readFile(
                p(session.sessionId, `${sessionStatePath}/events.jsonl`)
            );
            const content = buf.toString("utf8");
            expect(content).toContain("300");

            // No sqlite capabilities declared — verify no errors from missing sqlite
            expect(errors).toHaveLength(0);
        }
    );

    it("should view an image that exists only in the binary session fs provider", async () => {
        const imagePath = "/sdk-provider-image.png";
        const imageBytes = Buffer.from(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==",
            "base64"
        );
        const binaryClient = createClient({
            sessionFs: { ...sessionFsConfig, capabilities: { binary: true } },
        });
        try {
            let requestedPath: string | undefined;
            const session = await binaryClient.createSession({
                onPermissionRequest: approveAll,
                modelCapabilities: { supports: { vision: true } },
                createSessionFsProvider: (session) => ({
                    ...createSessionFsProvider(session),
                    async readFileBytes(path) {
                        requestedPath = path;
                        return provider.readFile(p(session.sessionId, path));
                    },
                    async writeFileBytes(path, content) {
                        await provider.writeFile(p(session.sessionId, path), content);
                    },
                }),
            });
            await provider.mkdir(`/${session.sessionId}`, { recursive: true });
            await provider.writeFile(p(session.sessionId, imagePath), imageBytes);
            expect(existsSync(imagePath)).toBe(false);

            const msg = await session.sendAndWait({
                prompt: "Use the view tool to view /sdk-provider-image.png, then reply with exactly SDK_PROVIDER_IMAGE_DONE.",
            });
            expect(msg?.data.content).toContain("SDK_PROVIDER_IMAGE_DONE");
            expect(requestedPath).toBe(imagePath);
            const events = await session.getEvents();
            const assetId = `sha256:${createHash("sha256").update(imageBytes).digest("hex")}`;
            expect(
                events.some(
                    (event) =>
                        event.type === "session.binary_asset" &&
                        event.data.assetId === assetId &&
                        event.data.mimeType === "image/png" &&
                        event.data.data === imageBytes.toString("base64")
                )
            ).toBe(true);
            expect(
                events.some(
                    (event) =>
                        event.type === "tool.execution_complete" &&
                        event.data.success &&
                        event.data.result?.binaryResultsForLlm?.some(
                            (result) => "assetId" in result && result.assetId === assetId
                        )
                )
            ).toBe(true);
            expect(
                (await openAiEndpoint.getRequests())
                    .filter((request) =>
                        ["/chat/completions", "/responses", "/v1/messages"].includes(request.url)
                    )
                    .some((request) => request.body.includes(imageBytes.toString("base64")))
            ).toBe(true);
            await session.disconnect();
        } finally {
            await binaryClient.stop();
        }
    });

    it("should load session data from fs provider on resume", async () => {
        const session1 = await client.createSession({
            onPermissionRequest: approveAll,
            createSessionFsProvider,
        });
        const sessionId = session1.sessionId;

        const msg = await session1.sendAndWait({ prompt: "What is 50 + 50?" });
        expect(msg?.data.content).toContain("100");
        await session1.disconnect();

        // The events file should exist before resume
        expect(await provider.exists(p(sessionId, `${sessionStatePath}/events.jsonl`))).toBe(true);

        const session2 = await client.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            createSessionFsProvider,
        });

        // Send another message to verify the session is functional after resume
        const msg2 = await session2.sendAndWait({ prompt: "What is that times 3?" });
        await session2.disconnect();
        expect(msg2?.data.content).toContain("300");
    });

    it("should reject setProvider when sessions already exist", async () => {
        const tcpConnectionToken = "session-fs-test-token";
        const client = new CopilotClient({
            // Use TCP so we can connect from a second client
            connection: RuntimeConnection.forTcp({ connectionToken: tcpConnectionToken }),
            env,
        });
        onTestFinished(() => client.stop());
        await client.createSession({ onPermissionRequest: approveAll, createSessionFsProvider });

        const { runtimePort: port } = client as unknown as { runtimePort: number };

        // Second client tries to connect with a session fs — should fail
        // because sessions already exist on the runtime.
        const client2 = new CopilotClient({
            env,
            logLevel: "error",
            connection: RuntimeConnection.forUri(`localhost:${port}`, {
                connectionToken: tcpConnectionToken,
            }),
            sessionFs: sessionFsConfig,
        });
        onTestFinished(() => client2.stop());

        await expect(client2.start()).rejects.toThrow();
    });

    it("should map large output handling into sessionFs", async () => {
        const suppliedFileContent = "x".repeat(100_000);
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            createSessionFsProvider,
            tools: [
                defineTool("get_big_string", {
                    description: "Returns a large string",
                    handler: async () => suppliedFileContent,
                }),
            ],
        });

        await session.sendAndWait({
            prompt: "Call the get_big_string tool and reply with the word DONE only.",
        });

        // The tool result should reference a temp file under the session state path.
        // The CLI joins the temp path using the host separator, so normalize before
        // matching to keep the assertion valid on Windows.
        const messages = await session.getEvents();
        const toolResult = findToolCallResult(messages, "get_big_string")?.replaceAll("\\", "/");
        expect(toolResult).toContain(`${sessionStatePath}/temp/`);
        const filename = toolResult?.match(
            new RegExp(`(${escapeRegExp(sessionStatePath)}/temp/[^\\s]+)`)
        )?.[1];
        expect(filename).toBeDefined();

        // Verify the file was written with the correct content via the provider
        const fileContent = await provider.readFile(p(session.sessionId, filename!), "utf8");
        expect(fileContent).toBe(suppliedFileContent);
        await session.disconnect();
    });

    it("should write workspace metadata via sessionFs", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            createSessionFsProvider,
        });

        const msg = await session.sendAndWait({ prompt: "What is 7 * 8?" });
        expect(msg?.data.content).toContain("56");

        // WorkspaceManager should have created workspace.yaml via sessionFs
        const workspaceYamlPath = p(session.sessionId, `${sessionStatePath}/workspace.yaml`);
        await expect.poll(() => provider.exists(workspaceYamlPath)).toBe(true);
        const yaml = await provider.readFile(workspaceYamlPath, "utf8");
        expect(yaml).toContain("id:");

        // Checkpoint index should also exist
        const indexPath = p(session.sessionId, `${sessionStatePath}/checkpoints/index.md`);
        await expect.poll(() => provider.exists(indexPath)).toBe(true);

        await session.disconnect();
    });

    it("should persist plan.md via sessionFs", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            createSessionFsProvider,
        });

        // Write a plan via the session RPC
        await session.sendAndWait({ prompt: "What is 2 + 3?" });
        await session.rpc.plan.update({ content: "# Test Plan\n\nThis is a test." });

        const planPath = p(session.sessionId, `${sessionStatePath}/plan.md`);
        await expect.poll(() => provider.exists(planPath)).toBe(true);
        const content = await provider.readFile(planPath, "utf8");
        expect(content).toContain("# Test Plan");

        await session.disconnect();
    });

    it("should succeed with compaction while using sessionFs", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            createSessionFsProvider,
        });

        let compactionEvent: SessionCompactionCompleteEvent | undefined;
        session.on("session.compaction_complete", (evt) => (compactionEvent = evt));

        await session.sendAndWait({ prompt: "What is 2+2?" });

        const eventsPath = p(session.sessionId, `${sessionStatePath}/events.jsonl`);
        await expect.poll(() => provider.exists(eventsPath)).toBe(true);
        const contentBefore = await provider.readFile(eventsPath, "utf8");
        expect(contentBefore).not.toContain("checkpointNumber");

        await session.rpc.history.compact();
        await expect.poll(() => compactionEvent, { timeout: 30_000 }).toBeDefined();
        expect(compactionEvent!.data.success).toBe(true);

        // Verify the events file was rewritten with a checkpoint via sessionFs
        await expect
            .poll(() => provider.readFile(eventsPath, "utf8"), { timeout: 30_000 })
            .toContain("checkpointNumber");
    });
});

describe("Session Fs native plan edits", async () => {
    const { createClient, workDir } = await createSdkTestContext();

    it.each(["interactive", "plan"] as const)(
        "keeps native edits and write failures in the provider in %s mode",
        async (mode) => {
            const statePath = join(workDir, `provider-state-${mode}`);
            const planPath = join(statePath, "plan.md");
            const provider = new MemoryProvider();
            const writes: string[] = [];
            let rejectWrites = false;
            const client = createClient({
                mode: "empty",
                sessionFs: {
                    initialCwd: workDir,
                    sessionStatePath: statePath,
                    conventions: process.platform === "win32" ? "windows" : "posix",
                },
            });
            onTestFinished(() => client.stop());
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                availableTools: ["edit"],
                createSessionFsProvider: (session) => {
                    const handler = createTestSessionFsHandler(session, provider);
                    return {
                        ...handler,
                        async writeFile(path, content) {
                            if (path === planPath) {
                                writes.push(content);
                                if (rejectWrites) {
                                    throw new Error("provider write denied");
                                }
                            }
                            return handler.writeFile(path, content);
                        },
                    };
                },
            });
            await session.rpc.mode.set({ mode });
            await session.rpc.plan.update({ content: "# Provider plan\n" });
            expect(await session.rpc.plan.read()).toMatchObject({
                path: planPath,
                content: "# Provider plan\n",
            });

            // A writable host parent makes an accidental host write succeed.
            await mkdir(statePath, { recursive: true });
            expect(await readdir(statePath)).toEqual([]);
            const edit = (old_str: string, new_str: string) =>
                session.rpc.tools.execute({
                    name: "edit",
                    arguments: { path: planPath, old_str, new_str },
                });
            await expect(edit("Provider plan", "Native update")).resolves.toMatchObject({
                resultType: "success",
            });
            expect((await session.rpc.plan.read()).content).toBe("# Native update\n");
            expect(writes).toEqual(["# Provider plan\n", "# Native update\n"]);
            expect(await readdir(statePath)).toEqual([]);

            await writeFile(planPath, "host-only content\n");
            await expect(edit("Native update", "Second update")).resolves.toMatchObject({
                resultType: "success",
            });
            expect((await session.rpc.plan.read()).content).toBe("# Second update\n");
            expect(await readFile(planPath, "utf8")).toBe("host-only content\n");

            rejectWrites = true;
            const failed = await edit("Second update", "Rejected update");
            expect(failed.resultType).toBe("failure");
            expect(failed.textResultForLlm).toContain("provider write denied");
            expect((await session.rpc.plan.read()).content).toBe("# Second update\n");
            expect(writes).toEqual([
                "# Provider plan\n",
                "# Native update\n",
                "# Second update\n",
                "# Rejected update\n",
            ]);
            expect(await readFile(planPath, "utf8")).toBe("host-only content\n");
            expect(await readdir(statePath)).toEqual(["plan.md"]);
        }
    );
});

describe("Session Fs remembered approvals", async () => {
    const workingDirectory = "/workspace";
    const otherDirectory = "/provider-dir";
    const permissionsPath = `${sessionStatePath}/permissions.json`;
    const {
        copilotClient: client,
        env,
        openAiEndpoint,
    } = await createSdkTestContext({
        copilotClientOptions: {
            mode: "empty",
            sessionFs: { ...sessionFsConfig, initialCwd: workingDirectory },
        },
    });

    function createApprovalProvider() {
        const provider = new MemoryProvider();
        const reads: string[] = [];
        const writes: string[] = [];
        return {
            provider,
            reads,
            writes,
            createSessionFsProvider(session: CopilotSession) {
                const handler = createTestSessionFsHandler(session, provider, "approvals");
                return {
                    ...handler,
                    async readFile(path: string) {
                        reads.push(path);
                        return handler.readFile(path);
                    },
                    async writeFile(path: string, content: string) {
                        writes.push(path);
                        return handler.writeFile(path, content);
                    },
                    async rename(src: string, dest: string) {
                        writes.push(dest);
                        return handler.rename(src, dest);
                    },
                };
            },
            async readPermissions(): Promise<string> {
                return (await provider.readFile(`/approvals${permissionsPath}`, "utf8")) as string;
            },
        };
    }

    async function expectCommands(session: CopilotSession, directory: string, commands: string[]) {
        const location = await session.rpc.permissions.locations.resolve({
            workingDirectory: directory,
        });
        const applied = await session.rpc.permissions.locations.apply({
            workingDirectory: directory,
        });
        expect(applied.locationKey).toBe(location.locationKey);
        expect(applied.locationType).toBe(location.locationType);
        expect(applied.appliedRules).toEqual(
            commands.map((argument) => ({ kind: "shell", argument }))
        );
        return applied;
    }

    async function addCommand(session: CopilotSession, directory: string, command: string) {
        const location = await session.rpc.permissions.locations.resolve({
            workingDirectory: directory,
        });
        const result = await session.rpc.permissions.locations.addToolApproval({
            locationKey: location.locationKey,
            approval: { kind: "commands", commandIdentifiers: [command] },
        });
        expect(result.success).toBe(true);
        return location.locationKey;
    }

    async function seedHostPermissions() {
        const path = join(env.COPILOT_HOME, "permissions-config.json");
        const content = Buffer.from(
            JSON.stringify(
                {
                    locations: Object.fromEntries(
                        [workingDirectory, otherDirectory].map((locationKey) => [
                            locationKey,
                            {
                                tool_approvals: [
                                    { kind: "commands", commandIdentifiers: ["host-only"] },
                                ],
                            },
                        ])
                    ),
                },
                null,
                2
            ) + "\n"
        );
        await writeFile(path, content);
        onTestFinished(() => rm(path, { force: true }));
        return async () => expect(await readFile(path)).toEqual(content);
    }

    it("isolates location approvals by provider namespace and shares grants in the same file", async () => {
        const expectHostUnchanged = await seedHostPermissions();
        const a = createApprovalProvider();
        const b = createApprovalProvider();
        const sessionA = await client.createSession({
            availableTools: [],
            createSessionFsProvider: a.createSessionFsProvider,
        });
        const sessionB = await client.createSession({
            availableTools: [],
            createSessionFsProvider: b.createSessionFsProvider,
        });

        for (const session of [sessionA, sessionB]) {
            await expectCommands(session, workingDirectory, []);
            await expectCommands(session, otherDirectory, []);
        }
        const workingLocation = await addCommand(sessionA, workingDirectory, "session-a");
        const otherLocation = await addCommand(sessionA, otherDirectory, "session-a-other");
        expect([workingLocation, otherLocation]).toEqual([workingDirectory, otherDirectory]);
        await expectCommands(sessionA, workingDirectory, ["session-a"]);
        await expectCommands(sessionA, otherDirectory, ["session-a-other"]);
        await expectCommands(sessionB, workingDirectory, []);

        await addCommand(sessionB, workingDirectory, "session-b");
        await expectCommands(sessionB, workingDirectory, ["session-b"]);
        const persistedA = await a.readPermissions();
        expect(JSON.parse(persistedA)).toEqual({
            locations: {
                [workingLocation]: {
                    tool_approvals: [{ kind: "commands", commandIdentifiers: ["session-a"] }],
                },
                [otherLocation]: {
                    tool_approvals: [{ kind: "commands", commandIdentifiers: ["session-a-other"] }],
                },
            },
        });
        const persistedB = await b.readPermissions();
        expect(persistedB).not.toContain("session-a");
        expect(
            (await sessionB.rpc.permissions.resetSessionApprovals({ includeLocation: true }))
                .success
        ).toBe(true);
        expect(await b.readPermissions()).toBe(persistedB);
        const reloaded = await expectCommands(sessionB, workingDirectory, ["session-b"]);
        expect(reloaded.appliedRuleCount).toBe(1);
        await sessionB.rpc.commands.invoke({ name: "reset-allowed-tools" });
        await expectCommands(sessionB, workingDirectory, []);
        expect(await a.readPermissions()).toBe(persistedA);
        await expectCommands(sessionA, workingDirectory, ["session-a"]);
        await expectHostUnchanged();

        // No-turn sessions have no transcript writer. Save the actual runtime events
        // through the fixture so approval resume can be tested without model inference.
        const events = await sessionA.getEvents();
        expect(events.some((event) => event.type === "session.start")).toBe(true);
        await a.provider.writeFile(
            `/approvals${sessionStatePath}/events.jsonl`,
            events.map((event) => JSON.stringify(event)).join("\n") + "\n"
        );
        await sessionA.disconnect();
        a.reads.length = 0;
        const resumed = await client.resumeSession(sessionA.sessionId, {
            availableTools: [],
            createSessionFsProvider: a.createSessionFsProvider,
        });
        await expectCommands(resumed, workingDirectory, ["session-a"]);
        await expectCommands(resumed, otherDirectory, ["session-a-other"]);
        for (const operations of [a.reads, a.writes, b.reads, b.writes]) {
            expect(operations).toContainEqual(expect.stringMatching(/\/permissions\.json$/));
        }

        await resumed.disconnect();
        const fresh = await client.createSession({
            availableTools: [],
            createSessionFsProvider: a.createSessionFsProvider,
        });
        expect(fresh.sessionId).not.toBe(sessionA.sessionId);
        await expectCommands(fresh, workingDirectory, ["session-a"]);
        await expectCommands(fresh, otherDirectory, ["session-a-other"]);
        await sessionB.disconnect();
        await fresh.disconnect();
        await expectHostUnchanged();
        expect(
            (await openAiEndpoint.getRequests()).filter((request) =>
                /\/(chat\/completions|responses|messages)$/.test(request.url)
            )
        ).toEqual([]);
    });

    it("does not serialize approval writes across separate session provider endpoints", async () => {
        const a = createApprovalProvider();
        const b = createApprovalProvider();
        let handlerA: SessionFsProvider;
        const sessionA = await client.createSession({
            availableTools: [],
            createSessionFsProvider(session) {
                handlerA = a.createSessionFsProvider(session);
                return handlerA;
            },
        });
        const sessionB = await client.createSession({
            availableTools: [],
            createSessionFsProvider: b.createSessionFsProvider,
        });
        const location = await sessionA.rpc.permissions.locations.resolve({ workingDirectory });
        let readStarted!: () => void;
        const readingA = new Promise<void>((resolve) => {
            readStarted = resolve;
        });
        let releaseA!: () => void;
        const waitForRelease = new Promise<void>((resolve) => {
            releaseA = resolve;
        });
        onTestFinished(() => releaseA());
        let aPaused = false;
        const readA = handlerA!.readFile.bind(handlerA!);
        vi.spyOn(handlerA!, "readFile").mockImplementation(async (path) => {
            if (path === permissionsPath) {
                aPaused = true;
                readStarted();
                await waitForRelease;
                aPaused = false;
            }
            return readA(path);
        });
        const pendingA = sessionA.rpc.permissions.locations.addToolApproval({
            locationKey: location.locationKey,
            approval: { kind: "commands", commandIdentifiers: ["session-a"] },
        });
        try {
            await Promise.race([
                readingA,
                pendingA.then(() => {
                    throw new Error("Session A completed without reaching its paused read");
                }),
            ]);
            expect(await addCommand(sessionB, workingDirectory, "session-b")).toBe(
                location.locationKey
            );
            expect(aPaused).toBe(true);
        } finally {
            releaseA();
        }
        expect((await pendingA).success).toBe(true);
        for (const [storage, command] of [
            [a, "session-a"],
            [b, "session-b"],
        ] as const) {
            expect(storage.reads).toContain(permissionsPath);
            expect(JSON.parse(await storage.readPermissions())).toEqual({
                locations: {
                    [location.locationKey]: {
                        tool_approvals: [{ kind: "commands", commandIdentifiers: [command] }],
                    },
                },
            });
        }
        await expectCommands(sessionA, workingDirectory, ["session-a"]);
        await expectCommands(sessionB, workingDirectory, ["session-b"]);
        await sessionA.disconnect();
        await sessionB.disconnect();
    });

    it.each(["read", "write"] as const)(
        "does not fall back to host approvals when the provider fails to %s",
        async (operation) => {
            const expectHostUnchanged = await seedHostPermissions();
            const storage = createApprovalProvider();
            let handler: SessionFsProvider;
            const session = await client.createSession({
                availableTools: [],
                createSessionFsProvider(created) {
                    handler = storage.createSessionFsProvider(created);
                    return handler;
                },
            });
            await expectCommands(session, workingDirectory, []);
            const error = Object.assign(new Error(`permission ${operation} denied`), {
                code: "EACCES",
            });
            if (operation === "read") {
                const original = handler!.readFile.bind(handler!);
                vi.spyOn(handler!, "readFile").mockImplementation((path) => {
                    if (path.endsWith("/permissions.json")) {
                        throw error;
                    }
                    return original(path);
                });
                await expect(
                    session.rpc.permissions.locations.apply({
                        workingDirectory: otherDirectory,
                    })
                ).rejects.toThrow(/permission read denied/);
            } else {
                const location = await session.rpc.permissions.locations.resolve({
                    workingDirectory,
                });
                const original = handler!.writeFile.bind(handler!);
                vi.spyOn(handler!, "writeFile").mockImplementation((path, content) => {
                    if (/\/permissions\.json(?:\.tmp\..*)?$/.test(path)) {
                        throw error;
                    }
                    return original(path, content);
                });
                await expect(
                    session.rpc.permissions.locations.addToolApproval({
                        locationKey: location.locationKey,
                        approval: { kind: "commands", commandIdentifiers: ["not-persisted"] },
                    })
                ).rejects.toThrow(/permission write denied/);
                await expectCommands(session, workingDirectory, []);
            }
            await expectHostUnchanged();
            await session.disconnect();
        }
    );
});

describe.each(["empty", "copilot-cli"] as const)(
    "Session Fs additional approvals in %s mode",
    async (mode) => {
        const { createClient, env, openAiEndpoint } = await createSdkTestContext({
            copilotClientOptions: { env: { COPILOT_WEB_FETCH_ALLOW_LOCALHOST: "1" } },
        });
        const statePath = (name: string) => `/approvals${sessionStatePath}/${name}`;

        function setup() {
            const client = createClient({
                mode,
                sessionFs: { ...sessionFsConfig, initialCwd: "/workspace" },
            });
            onTestFinished(() => client.stop());
            function createNamespace() {
                const provider = new MemoryProvider();
                const handlers = new Map<string, SessionFsProvider>();
                const config = {
                    availableTools: ["builtin:web_fetch"],
                    createSessionFsProvider(session: CopilotSession) {
                        const handler = createTestSessionFsHandler(session, provider, "approvals");
                        handlers.set(session.sessionId, handler);
                        return handler;
                    },
                };
                return {
                    client,
                    provider,
                    handlers,
                    config,
                    createNamespace,
                    async readState(name: string) {
                        return JSON.parse(
                            (await provider.readFile(statePath(name), "utf8")) as string
                        );
                    },
                    async resume(session: CopilotSession) {
                        // Direct RPCs do not start the transcript writer. Preserve only
                        // actual runtime events, not fabricated permission decisions.
                        const events = await session.getEvents();
                        await provider.writeFile(
                            statePath("events.jsonl"),
                            events.map((event) => JSON.stringify(event)).join("\n") + "\n"
                        );
                        await session.disconnect();
                        return client.resumeSession(session.sessionId, config);
                    },
                };
            }
            return createNamespace();
        }

        async function seedHostState(allowedUrl = "http://127.0.0.1") {
            const files = {
                "settings.json": {
                    allowedUrls: [allowedUrl],
                    sandbox: { enabled: false },
                    approval_test_sentinel: "settings-unchanged",
                },
                "config.json": {
                    trustedFolders: ["/workspace", "/host-only"],
                    approval_test_sentinel: "config-unchanged",
                },
            };
            const contents = Object.entries(files).map(([name, value]) => ({
                path: join(env.COPILOT_HOME, name),
                content: JSON.stringify(value, null, 2) + "\n",
            }));
            for (const { path, content } of contents) {
                await writeFile(path, content);
            }
            return async () => {
                for (const { path, content } of contents) {
                    expect(await readFile(path, "utf8")).toBe(content);
                }
            };
        }

        it.each([false, true])(
            "loads existing-format provider grants while ignoring legacy config URLs=%s on create and resume",
            async (legacyConfigUrls) => {
                const endpoint = await createPermissionEndpoint();
                const hostUnchanged = await seedHostState();
                const storage = setup();
                const files = {
                    "permissions.json": {
                        locations: {
                            "/workspace": {
                                tool_approvals: [
                                    { kind: "commands", commandIdentifiers: ["imported-command"] },
                                ],
                                allowed_directories: ["/imported-directory"],
                            },
                        },
                    },
                    "settings.json": {
                        allowedUrls: [endpoint.origin],
                        sandbox: { enabled: false },
                    },
                    "config.json": {
                        trustedFolders: ["/imported-directory"],
                        ...(legacyConfigUrls ? { allowedUrls: ["https://legacy.example"] } : {}),
                    },
                };
                await storage.provider.mkdir(statePath(""), { recursive: true });
                await storage.provider.mkdir("/approvals/imported-directory", { recursive: true });
                for (const [name, value] of Object.entries(files)) {
                    await storage.provider.writeFile(statePath(name), JSON.stringify(value) + "\n");
                }
                const session = await storage.client.createSession(storage.config);
                async function expectImportedGrants(session: CopilotSession) {
                    const applied = await session.rpc.permissions.locations.apply({
                        workingDirectory: "/workspace",
                    });
                    expect(applied.appliedRules).toEqual([
                        { kind: "shell", argument: "imported-command" },
                    ]);
                    await expect(
                        session.rpc.permissions.paths.isPathWithinAllowedDirectories({
                            path: "/imported-directory/child",
                        })
                    ).resolves.toEqual({ allowed: true });
                    await expect(fetchUrl(session, endpoint.url)).resolves.toMatchObject({
                        resultType: "success",
                    });
                    await expect(
                        session.rpc.permissions.folderTrust.isTrusted({
                            path: "/imported-directory/child",
                        })
                    ).resolves.toEqual({ trusted: true });
                    await expect(
                        session.rpc.commands.invoke({ name: "sandbox", input: "status" })
                    ).resolves.toMatchObject({
                        kind: "text",
                        text: expect.stringMatching(/disabled/i),
                    });
                    await session.rpc.options.update({ sandboxConfig: { enabled: true } });
                    await expect(
                        session.rpc.commands.invoke({ name: "sandbox", input: "status" })
                    ).resolves.toMatchObject({
                        kind: "text",
                        text: expect.stringContaining("Current sandbox status: enabled"),
                    });
                }
                await expectImportedGrants(session);
                const resumed = await storage.resume(session);
                await expectImportedGrants(resumed);
                expect(endpoint.requests).toHaveLength(2);
                for (const [name, value] of Object.entries(files)) {
                    expect(await storage.provider.readFile(statePath(name), "utf8")).toBe(
                        JSON.stringify(value) + "\n"
                    );
                }
                await hostUnchanged();
                expect(
                    (await openAiEndpoint.getRequests()).filter((request) =>
                        /\/(chat\/completions|responses|messages)$/.test(request.url)
                    )
                ).toEqual([]);
            }
        );

        it("routes permanent URL consent through provider namespaces across live and resumed sessions", async () => {
            const endpoint = await createPermissionEndpoint();
            const hostUnchanged = await seedHostState(endpoint.origin);
            const storage = setup();
            const isolated = storage.createNamespace();
            const config = {
                allowedUrls: ["https://legacy.example"],
                trustedFolders: ["/workspace/project"],
            };
            await storage.provider.mkdir(statePath(""), { recursive: true });
            await storage.provider.writeFile(statePath("config.json"), JSON.stringify(config));
            const a = await storage.client.createSession(storage.config);
            const b = await storage.client.createSession(isolated.config);
            for (const session of [a, b]) {
                await session.rpc.permissions.configure({
                    approveAllReadPermissionRequests: false,
                    urls: { unrestricted: false, initialAllowed: [] },
                });
            }

            await decideUrl(a, endpoint.url, {
                kind: "approve-permanently",
                domain: endpoint.origin,
            });
            expect(endpoint.requests).toHaveLength(1);
            expect(await storage.readState("settings.json")).toEqual({
                allowedUrls: [endpoint.origin],
            });
            expect(await storage.readState("config.json")).toEqual(config);
            await expect(fetchUrl(a, endpoint.url)).resolves.toMatchObject({
                resultType: "success",
            });
            await decideUrl(b, endpoint.url, { kind: "reject" });
            expect(endpoint.requests).toHaveLength(2);

            await a.rpc.permissions.configure({
                rules: { approved: [], denied: [{ kind: "url", argument: endpoint.origin }] },
            });
            await expect(fetchUrl(a, endpoint.url)).resolves.toMatchObject({
                resultType: "denied",
            });
            expect(endpoint.requests).toHaveLength(2);
            await a.rpc.permissions.configure({ rules: { approved: [], denied: [] } });

            const resumed = await storage.resume(a);
            await expect(fetchUrl(resumed, endpoint.url)).resolves.toMatchObject({
                resultType: "success",
            });
            await resumed.disconnect();
            const fresh = await storage.client.createSession(storage.config);
            await expect(fetchUrl(fresh, endpoint.url)).resolves.toMatchObject({
                resultType: "success",
            });
            expect(endpoint.requests).toHaveLength(4);
            await decideUrl(b, endpoint.url, {
                kind: "approve-for-session",
                domain: endpoint.origin,
            });
            await expect(fetchUrl(b, endpoint.url)).resolves.toMatchObject({
                resultType: "success",
            });
            expect(endpoint.requests).toHaveLength(6);
            const bEvents = await b.getEvents();
            expect(bEvents.some((event) => event.type === "permission.completed")).toBe(true);
            expect(await isolated.provider.exists(statePath("settings.json"))).toBe(false);
            await hostUnchanged();
            expect(
                (await openAiEndpoint.getRequests()).filter((request) =>
                    /\/(chat\/completions|responses|messages)$/.test(request.url)
                )
            ).toEqual([]);
        });

        it("isolates lexical folder trust by provider namespace and restores it on resume", async () => {
            const hostUnchanged = await seedHostState();
            const storage = setup();
            const isolated = storage.createNamespace();
            const a = await storage.client.createSession(storage.config);
            const b = await storage.client.createSession(isolated.config);
            const trusted = (session: CopilotSession, path: string) =>
                session.rpc.permissions.folderTrust.isTrusted({ path });
            for (const session of [a, b]) {
                await expect(trusted(session, "/workspace")).resolves.toEqual({ trusted: false });
                await expect(trusted(session, "/host-only")).resolves.toEqual({ trusted: false });
            }
            expect(
                (await a.rpc.permissions.folderTrust.addTrusted({ path: "/workspace/./project" }))
                    .success
            ).toBe(true);
            expect(await storage.readState("config.json")).toEqual({
                trustedFolders: ["/workspace/project"],
            });
            await expect(trusted(a, "/workspace/project/nested")).resolves.toEqual({
                trusted: true,
            });
            for (const path of ["/workspace/project-other", "/workspace/project/../outside"]) {
                await expect(trusted(a, path)).resolves.toEqual({ trusted: false });
            }
            await expect(trusted(b, "/workspace/project/nested")).resolves.toEqual({
                trusted: false,
            });
            const resumed = await storage.resume(a);
            await expect(trusted(resumed, "/workspace/project/nested")).resolves.toEqual({
                trusted: true,
            });
            await resumed.disconnect();
            const fresh = await storage.client.createSession(storage.config);
            await expect(trusted(fresh, "/workspace/project")).resolves.toEqual({
                trusted: true,
            });
            await hostUnchanged();
        });

        it("stores sandbox command overrides in provider settings and restores them on resume", async () => {
            const hostUnchanged = await seedHostState();
            const storage = setup();
            const isolated = storage.createNamespace();
            // No process is launched: configuring enabled state works even on
            // hosts without a sandbox backend, unlike the enable command.
            const sandboxConfig = { enabled: true };
            const a = await storage.client.createSession(storage.config);
            const b = await storage.client.createSession(isolated.config);
            await a.rpc.options.update({ sandboxConfig });
            await b.rpc.options.update({ sandboxConfig });
            await expect(
                a.rpc.commands.invoke({ name: "sandbox", input: "status" })
            ).resolves.toMatchObject({ kind: "text", text: expect.stringMatching(/enabled/i) });
            await a.rpc.commands.invoke({ name: "sandbox", input: "disable" });
            expect(await storage.readState("settings.json")).toEqual({
                sandbox: { enabled: false },
            });
            await a.rpc.options.update({ sandboxConfig });
            await expect(
                a.rpc.commands.invoke({ name: "sandbox", input: "status" })
            ).resolves.toMatchObject({
                kind: "text",
                text: expect.stringContaining("Current sandbox status: enabled"),
            });
            expect(await storage.readState("settings.json")).toEqual({
                sandbox: { enabled: false },
            });
            expect(await isolated.provider.exists(statePath("settings.json"))).toBe(false);
            await expect(
                b.rpc.commands.invoke({ name: "sandbox", input: "status" })
            ).resolves.toMatchObject({ kind: "text", text: expect.stringMatching(/enabled/i) });
            const resumed = await storage.resume(a);
            expect((await storage.readState("settings.json")).sandbox).toEqual({
                enabled: false,
            });
            await expect(
                resumed.rpc.commands.invoke({ name: "sandbox", input: "status" })
            ).resolves.toMatchObject({ kind: "text", text: expect.stringMatching(/disabled/i) });
            if ((await storage.client.rpc.sandbox.getHostSupport()).supported) {
                const baseDirectory = join(env.COPILOT_HOME, "sandbox-host-defaults");
                await mkdir(baseDirectory);
                await writeFile(
                    join(baseDirectory, "settings.json"),
                    JSON.stringify({ sandbox: { enabled: false } })
                );
                const hostClient = createClient({ mode, baseDirectory });
                onTestFinished(() => hostClient.stop());
                const hostSession = await hostClient.createSession({ availableTools: [] });
                await hostSession.rpc.commands.invoke({ name: "sandbox", input: "enable" });
                const hostSettings = JSON.parse(
                    await readFile(join(baseDirectory, "settings.json"), "utf8")
                );
                expect(hostSettings.sandbox.enabled).toBe(true);
                await resumed.rpc.commands.invoke({ name: "sandbox", input: "enable" });
                expect((await storage.readState("settings.json")).sandbox).toEqual(
                    hostSettings.sandbox
                );
                const enabledResume = await storage.resume(resumed);
                await expect(
                    enabledResume.rpc.commands.invoke({ name: "sandbox", input: "status" })
                ).resolves.toMatchObject({
                    kind: "text",
                    text: expect.stringMatching(/enabled/i),
                });
                await enabledResume.disconnect();
                await expect(
                    b.rpc.commands.invoke({ name: "sandbox", input: "status" })
                ).resolves.toMatchObject({
                    kind: "text",
                    text: expect.stringMatching(/enabled/i),
                });
            }
            await hostUnchanged();
        });

        for (const enabled of [false, true]) {
            // Empty-mode initialization constructs shell descriptors, which
            // reject an enabled sandbox on musl before the session can open.
            it.skipIf(enabled && mode === "empty" && getRuntimePlatform().startsWith("linuxmusl-"))(
                `honors explicit sandbox updates without changing the saved enabled=${enabled} preference`,
                async () => {
                    const hostUnchanged = await seedHostState();
                    const storage = setup();
                    const settings = JSON.stringify({ sandbox: { enabled } });
                    await storage.provider.mkdir(statePath(""), { recursive: true });
                    await storage.provider.writeFile(statePath("settings.json"), settings);
                    const session = await storage.client.createSession(storage.config);
                    const expectStatus = (session: CopilotSession, enabled: boolean) =>
                        expect(
                            session.rpc.commands.invoke({ name: "sandbox", input: "status" })
                        ).resolves.toMatchObject({
                            kind: "text",
                            text: expect.stringContaining(
                                `Current sandbox status: ${enabled ? "enabled" : "disabled"}`
                            ),
                        });

                    await expectStatus(session, enabled);
                    await session.rpc.options.update({ sandboxConfig: { enabled: !enabled } });
                    await expectStatus(session, !enabled);
                    expect(
                        await storage.provider.readFile(statePath("settings.json"), "utf8")
                    ).toBe(settings);

                    const resident = await storage.client.resumeSession(
                        session.sessionId,
                        storage.config
                    );
                    await expectStatus(resident, enabled);
                    await resident.rpc.options.update({ sandboxConfig: { enabled: !enabled } });
                    await expectStatus(resident, !enabled);

                    const resumed = await storage.resume(resident);
                    await expectStatus(resumed, enabled);
                    await resumed.rpc.options.update({ sandboxConfig: { enabled: !enabled } });
                    await expectStatus(resumed, !enabled);
                    expect(
                        await storage.provider.readFile(statePath("settings.json"), "utf8")
                    ).toBe(settings);
                    await hostUnchanged();
                }
            );
        }

        it.each(["read", "write"] as const)(
            "does not fall back to host folder or sandbox state after a provider %s failure",
            async (operation) => {
                const hostUnchanged = await seedHostState();
                const storage = setup();
                const session = await storage.client.createSession(storage.config);
                const handler = storage.handlers.get(session.sessionId)!;
                const error = Object.assign(new Error(`approval ${operation} denied`), {
                    code: "EACCES",
                });
                if (operation === "read") {
                    const original = handler.readFile.bind(handler);
                    vi.spyOn(handler, "readFile").mockImplementation((path) => {
                        if (/\/(?:config|settings)\.json$/.test(path)) {
                            throw error;
                        }
                        return original(path);
                    });
                    await expect(
                        session.rpc.permissions.folderTrust.isTrusted({ path: "/host-only" })
                    ).rejects.toThrow(/approval read denied/);
                } else {
                    const original = handler.writeFile.bind(handler);
                    vi.spyOn(handler, "writeFile").mockImplementation((path, content) => {
                        if (/\/(?:config|settings)\.json(?:\.tmp\..*)?$/.test(path)) {
                            throw error;
                        }
                        return original(path, content);
                    });
                }
                await expect(
                    session.rpc.permissions.folderTrust.addTrusted({ path: "/workspace/project" })
                ).rejects.toThrow(`approval ${operation} denied`);
                await expect(
                    session.rpc.commands.invoke({ name: "sandbox", input: "disable" })
                ).rejects.toThrow(`approval ${operation} denied`);
                await hostUnchanged();
            }
        );

        it.each(["read", "write"] as const)(
            "does not fall back to host URL consent after a provider %s failure",
            async (operation) => {
                const endpoint = await createPermissionEndpoint();
                const hostUnchanged = await seedHostState(endpoint.origin);
                const storage = setup();
                const session = await storage.client.createSession(storage.config);
                await session.rpc.permissions.configure({
                    approveAllReadPermissionRequests: false,
                    urls: { unrestricted: false, initialAllowed: [] },
                });
                await session.rpc.permissions.setRequired({ required: true });
                const handler = storage.handlers.get(session.sessionId)!;
                const error = Object.assign(new Error(`URL ${operation} denied`), {
                    code: "EACCES",
                });
                if (operation === "read") {
                    const original = handler.readFile.bind(handler);
                    vi.spyOn(handler, "readFile").mockImplementation((path) => {
                        if (path.endsWith("/settings.json")) {
                            throw error;
                        }
                        return original(path);
                    });
                    await expect(fetchUrl(session, endpoint.url)).resolves.toMatchObject({
                        resultType: "failure",
                        textResultForLlm: expect.stringContaining("URL read denied"),
                    });
                } else {
                    const original = handler.writeFile.bind(handler);
                    const failure = vi
                        .spyOn(handler, "writeFile")
                        .mockImplementation((path, content) => {
                            if (/\/settings\.json(?:\.tmp\..*)?$/.test(path)) {
                                throw error;
                            }
                            return original(path, content);
                        });
                    const execution = fetchUrl(session, endpoint.url);
                    void execution.catch(() => {});
                    await expect
                        .poll(async () => (await session.rpc.permissions.pendingRequests()).items)
                        .toHaveLength(1);
                    const [pending] = (await session.rpc.permissions.pendingRequests()).items;
                    expect(
                        (
                            await session.rpc.permissions.handlePendingPermissionRequest({
                                requestId: pending.requestId,
                                result: { kind: "approve-permanently", domain: endpoint.origin },
                            })
                        ).success
                    ).toBe(true);
                    await expect(execution).resolves.toMatchObject({
                        resultType: "failure",
                        textResultForLlm: expect.stringContaining("URL write denied"),
                    });
                    failure.mockRestore();
                    await decideUrl(session, endpoint.url, { kind: "reject" });
                }
                expect(endpoint.requests).toEqual([]);
                await hostUnchanged();
            }
        );
    }
);

describe("SDK remembered approvals without Session Fs", async () => {
    const { createClient, workDir, env } = await createSdkTestContext({
        copilotClientOptions: { env: { COPILOT_WEB_FETCH_ALLOW_LOCALHOST: "1" } },
    });

    it.each(["empty", "copilot-cli"] as const)(
        "preserves host location approvals across new SDK sessions in %s mode",
        async (mode) => {
            const workingDirectory = join(workDir, mode);
            await mkdir(workingDirectory);
            const baseDirectory = join(env.COPILOT_HOME, mode);
            await mkdir(baseDirectory);
            const firstClient = createClient({ mode, baseDirectory });
            onTestFinished(() => firstClient.stop());
            const first = await firstClient.createSession({
                availableTools: [],
                workingDirectory,
                onPermissionRequest: approveAll,
            });
            const location = await first.rpc.permissions.locations.resolve({ workingDirectory });
            expect(
                (await first.rpc.permissions.locations.apply({ workingDirectory })).appliedRules
            ).toEqual([]);
            const approval = {
                kind: "commands" as const,
                commandIdentifiers: ["host-sdk-persisted"],
            };
            expect(
                (
                    await first.rpc.permissions.locations.addToolApproval({
                        locationKey: location.locationKey,
                        approval,
                    })
                ).success
            ).toBe(true);
            const hostPermissions = JSON.parse(
                await readFile(join(baseDirectory, "permissions-config.json"), "utf8")
            );
            expect(hostPermissions.locations[location.locationKey].tool_approvals).toContainEqual(
                approval
            );
            await first.disconnect();
            await firstClient.stop();

            const secondClient = createClient({ mode, baseDirectory });
            onTestFinished(() => secondClient.stop());
            const second = await secondClient.createSession({
                availableTools: [],
                workingDirectory,
                onPermissionRequest: approveAll,
            });
            expect(second.sessionId).not.toBe(first.sessionId);
            const applied = await second.rpc.permissions.locations.apply({ workingDirectory });
            expect(applied.locationKey).toBe(location.locationKey);
            expect(applied.appliedRules).toEqual([
                { kind: "shell", argument: "host-sdk-persisted" },
            ]);
            await second.disconnect();
        }
    );

    it.each(["empty", "copilot-cli"] as const)(
        "preserves host URL consent, folder trust, and sandbox settings in %s mode",
        async (mode) => {
            const baseDirectory = join(env.COPILOT_HOME, `additional-${mode}`);
            await mkdir(baseDirectory);
            const folder = join(workDir, `trusted-${mode}`);
            await mkdir(folder);
            const endpoint = await createPermissionEndpoint();
            const firstClient = createClient({ mode, baseDirectory });
            onTestFinished(() => firstClient.stop());
            const first = await firstClient.createSession({
                availableTools: ["builtin:web_fetch"],
            });
            await decideUrl(first, endpoint.url, {
                kind: "approve-permanently",
                domain: endpoint.origin,
            });
            await expect(
                first.rpc.permissions.folderTrust.isTrusted({ path: folder })
            ).resolves.toEqual({ trusted: false });
            expect(
                (await first.rpc.permissions.folderTrust.addTrusted({ path: folder })).success
            ).toBe(true);
            await first.rpc.commands.invoke({ name: "sandbox", input: "disable" });
            const settings = JSON.parse(
                await readFile(join(baseDirectory, "settings.json"), "utf8")
            );
            expect(settings.allowedUrls).toContain(endpoint.origin);
            expect(settings.sandbox.enabled).toBe(false);
            const savedSettings = await readFile(join(baseDirectory, "settings.json"), "utf8");
            // Host config is JSONC and canonicalizes aliases such as Windows 8.3
            // names; the public query below also verifies trust after restart.
            const config = await readFile(join(baseDirectory, "config.json"), "utf8");
            expect(config).toContain(JSON.stringify(realpathSync.native(folder)));
            await first.disconnect();
            await firstClient.stop();
            const secondClient = createClient({ mode, baseDirectory });
            onTestFinished(() => secondClient.stop());
            const second = await secondClient.createSession({
                availableTools: ["builtin:web_fetch"],
            });
            // SDK sessions do not automatically import the host URL allowlist.
            // Permanent consent saves host settings and grants the live manager only.
            await decideUrl(second, endpoint.url, { kind: "approve-once" });
            expect(endpoint.requests).toHaveLength(2);
            expect(await readFile(join(baseDirectory, "settings.json"), "utf8")).toBe(
                savedSettings
            );
            await expect(
                second.rpc.permissions.folderTrust.isTrusted({ path: folder })
            ).resolves.toEqual({ trusted: true });
            await expect(
                second.rpc.commands.invoke({ name: "sandbox", input: "status" })
            ).resolves.toMatchObject({ kind: "text", text: expect.stringMatching(/disabled/i) });
        }
    );
});

async function createPermissionEndpoint() {
    const requests: string[] = [];
    const server = createServer((request, response) => {
        requests.push(request.url ?? "");
        response.writeHead(200, { "Content-Type": "text/html" });
        response.end("<html><body>SESSIONFS_URL_CONSENT_CONFIRMED</body></html>");
    });
    onTestFinished(
        () =>
            new Promise<void>((resolve, reject) => {
                server.close((error) => (error ? reject(error) : resolve()));
                server.closeAllConnections();
            })
    );
    await new Promise<void>((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", resolve);
    });
    const address = server.address();
    if (!address || typeof address === "string") {
        throw new Error("URL approval fixture did not acquire a TCP port");
    }
    const origin = `http://127.0.0.1:${address.port}`;
    return { origin, url: `${origin}/consent`, requests };
}

async function fetchUrl(session: CopilotSession, url: string) {
    await session.rpc.permissions.setRequired({ required: true });
    await session.rpc.tools.initializeAndValidate();
    return session.rpc.tools.execute({ name: "web_fetch", arguments: { url, raw: true } });
}

async function decideUrl(
    session: CopilotSession,
    url: string,
    decision: Exclude<PermissionRequestResult, { kind: "no-result" }>
) {
    await session.rpc.permissions.setRequired({ required: true });
    const execution = fetchUrl(session, url);
    // Observe errors immediately while waiting for the permission event.
    void execution.catch(() => {});
    await expect
        .poll(async () => (await session.rpc.permissions.pendingRequests()).items)
        .toHaveLength(1);
    const [pending] = (await session.rpc.permissions.pendingRequests()).items;
    expect(
        (
            await session.rpc.permissions.handlePendingPermissionRequest({
                requestId: pending.requestId,
                result: decision,
            })
        ).success
    ).toBe(true);
    const result = await execution;
    expect(result).toMatchObject({
        resultType: decision.kind === "reject" ? "rejected" : "success",
    });
}

describe("Session Fs Adapter", () => {
    it("should map all sessionFs handler operations", async () => {
        const provider = new MemoryProvider();
        const userProvider: SessionFsProvider = {
            async readFile(path: string): Promise<string> {
                return (await provider.readFile(path, "utf8")) as string;
            },
            async writeFile(path: string, content: string): Promise<void> {
                await provider.writeFile(path, content);
            },
            async appendFile(path: string, content: string): Promise<void> {
                await provider.appendFile(path, content);
            },
            async exists(path: string): Promise<boolean> {
                return provider.exists(path);
            },
            async stat(path: string): Promise<SessionFsFileInfo> {
                const st = await provider.stat(path);
                return {
                    isFile: st.isFile(),
                    isDirectory: st.isDirectory(),
                    size: st.size,
                    mtime: new Date(st.mtimeMs).toISOString(),
                    birthtime: new Date(st.birthtimeMs).toISOString(),
                };
            },
            async mkdir(path: string, recursive: boolean, mode?: number): Promise<void> {
                await provider.mkdir(path, { recursive, mode });
            },
            async readdir(path: string): Promise<string[]> {
                return (await provider.readdir(path)) as string[];
            },
            async readdirWithTypes(path: string): Promise<SessionFsReaddirWithTypesEntry[]> {
                const names = (await provider.readdir(path)) as string[];
                return Promise.all(
                    names.map(async (name) => {
                        const st = await provider.stat(`${path}/${name}`);
                        return {
                            name,
                            type: st.isDirectory() ? ("directory" as const) : ("file" as const),
                        };
                    })
                );
            },
            async rm(path: string, _recursive: boolean, force: boolean): Promise<void> {
                try {
                    await provider.unlink(path);
                } catch (err) {
                    if (force && (err as NodeJS.ErrnoException).code === "ENOENT") {
                        return;
                    }
                    throw err;
                }
            },
            async rename(src: string, dest: string): Promise<void> {
                await provider.rename(src, dest);
            },
            sqlite: {
                async query(queryType, query, params) {
                    return {
                        columns: ["sessionId", "query", "queryType", "answer"],
                        rows: [
                            {
                                sessionId: "handler-session",
                                query,
                                queryType,
                                answer: params?.answer,
                            },
                        ],
                        rowsAffected: 0,
                    };
                },
                async transaction(statements) {
                    return statements.map((statement) => ({
                        columns: ["sessionId", "query", "queryType", "answer"],
                        rows: [
                            {
                                sessionId: "handler-session",
                                query: statement.query,
                                queryType: statement.queryType,
                                answer: statement.params?.answer,
                            },
                        ],
                        rowsAffected: 0,
                    }));
                },
                async exists() {
                    return true;
                },
            },
        };
        const handler = createSessionFsAdapter(userProvider);

        const sessionId = "handler-session";
        const params = (extra: Record<string, unknown> = {}) => ({ sessionId, ...extra });

        expect(
            await handler.mkdir(params({ path: "/workspace/nested", recursive: true }))
        ).toBeUndefined();

        expect(
            await handler.writeFile(
                params({ path: "/workspace/nested/file.txt", content: "hello" })
            )
        ).toBeUndefined();

        expect(
            await handler.appendFile(
                params({ path: "/workspace/nested/file.txt", content: " world" })
            )
        ).toBeUndefined();

        const exists = await handler.exists(params({ path: "/workspace/nested/file.txt" }));
        expect(exists.exists).toBe(true);

        const stat = await handler.stat(params({ path: "/workspace/nested/file.txt" }));
        expect(stat.isFile).toBe(true);
        expect(stat.isDirectory).toBe(false);
        expect(stat.size).toBe("hello world".length);
        expect(stat.error).toBeUndefined();

        const content = await handler.readFile(params({ path: "/workspace/nested/file.txt" }));
        expect(content.content).toBe("hello world");
        expect(content.error).toBeUndefined();

        const entries = await handler.readdir(params({ path: "/workspace/nested" }));
        expect(entries.entries).toContain("file.txt");
        expect(entries.error).toBeUndefined();

        const typedEntries = await handler.readdirWithTypes(params({ path: "/workspace/nested" }));
        expect(typedEntries.entries).toContainEqual({ name: "file.txt", type: "file" });
        expect(typedEntries.error).toBeUndefined();

        expect(
            await handler.rename(
                params({
                    src: "/workspace/nested/file.txt",
                    dest: "/workspace/nested/renamed.txt",
                })
            )
        ).toBeUndefined();

        const oldPath = await handler.exists(params({ path: "/workspace/nested/file.txt" }));
        expect(oldPath.exists).toBe(false);

        const renamed = await handler.readFile(params({ path: "/workspace/nested/renamed.txt" }));
        expect(renamed.content).toBe("hello world");

        expect(await handler.rm(params({ path: "/workspace/nested/renamed.txt" }))).toBeUndefined();

        const removed = await handler.exists(params({ path: "/workspace/nested/renamed.txt" }));
        expect(removed.exists).toBe(false);

        // Forced removal of a missing file should not error.
        expect(
            await handler.rm(params({ path: "/workspace/nested/missing.txt", force: true }))
        ).toBeUndefined();

        const missing = await handler.stat(params({ path: "/workspace/nested/missing.txt" }));
        expect(missing.error?.code).toBe("ENOENT");

        const sqliteQuery = await handler.sqliteQuery({
            sessionId,
            query: "select :answer as answer",
            queryType: "query",
            params: { answer: 42 },
        });
        expect(sqliteQuery.columns).toContain("answer");
        expect(sqliteQuery.rows[0]).toMatchObject({
            sessionId,
            query: "select :answer as answer",
            queryType: "query",
            answer: 42,
        });
        expect(sqliteQuery.rowsAffected).toBe(0);
        expect(sqliteQuery.error).toBeUndefined();

        const sqliteExists = await handler.sqliteExists({ sessionId });
        expect(sqliteExists.exists).toBe(true);
    });

    it("converts provider exceptions to RPC errors", async () => {
        const enoent: NodeJS.ErrnoException = Object.assign(new Error("missing"), {
            code: "ENOENT",
        });
        const throwing: SessionFsProvider = {
            readFile: async () => {
                throw enoent;
            },
            writeFile: async () => {
                throw enoent;
            },
            appendFile: async () => {
                throw enoent;
            },
            exists: async () => {
                throw enoent;
            },
            stat: async () => {
                throw enoent;
            },
            mkdir: async () => {
                throw enoent;
            },
            readdir: async () => {
                throw enoent;
            },
            readdirWithTypes: async () => {
                throw enoent;
            },
            rm: async () => {
                throw enoent;
            },
            rename: async () => {
                throw enoent;
            },
            sqlite: {
                query: async () => {
                    throw enoent;
                },
                transaction: async () => {
                    throw enoent;
                },
                exists: async () => {
                    throw enoent;
                },
            },
        };

        const handler = createSessionFsAdapter(throwing);

        const assertEnoent = (error: { code: string; message: string } | undefined) => {
            expect(error).toBeDefined();
            expect(error!.code).toBe("ENOENT");
            expect(error!.message.toLowerCase()).toContain("missing");
        };

        assertEnoent((await handler.readFile({ path: "missing.txt" } as never)).error);
        assertEnoent(
            await handler.writeFile({
                path: "missing.txt",
                content: "content",
            } as never)
        );
        assertEnoent(
            await handler.appendFile({
                path: "missing.txt",
                content: "content",
            } as never)
        );

        // exists swallows errors and returns { exists: false }
        const existsResult = await handler.exists({ path: "missing.txt" } as never);
        expect(existsResult.exists).toBe(false);

        assertEnoent((await handler.stat({ path: "missing.txt" } as never)).error);
        assertEnoent(await handler.mkdir({ path: "missing-dir" } as never));
        assertEnoent((await handler.readdir({ path: "missing-dir" } as never)).error);
        assertEnoent((await handler.readdirWithTypes({ path: "missing-dir" } as never)).error);
        assertEnoent(await handler.rm({ path: "missing.txt" } as never));
        assertEnoent(await handler.rename({ src: "missing.txt", dest: "dest.txt" } as never));

        // sqlite methods let errors propagate (no try/catch wrapping)
        await expect(
            handler.sqliteQuery({
                sessionId: "throw-session",
                query: "select 1",
                queryType: "query",
            })
        ).rejects.toThrow("missing");
        await expect(handler.sqliteExists({ sessionId: "throw-session" })).rejects.toThrow(
            "missing"
        );

        // Non-ENOENT errors map to UNKNOWN.
        const unknown: SessionFsProvider = {
            ...throwing,
            writeFile: async () => {
                throw new Error("bad path");
            },
        };
        const unknownHandler = createSessionFsAdapter(unknown);
        const unknownError = await unknownHandler.writeFile({
            path: "bad.txt",
            content: "content",
        } as never);
        expect(unknownError?.code).toBe("UNKNOWN");
    });
});

function findToolCallResult(messages: SessionEvent[], toolName: string): string | undefined {
    for (const m of messages) {
        if (m.type === "tool.execution_complete") {
            if (findToolName(messages, m.data.toolCallId) === toolName) {
                return m.data.result?.content;
            }
        }
    }
}

function findToolName(messages: SessionEvent[], toolCallId: string): string | undefined {
    for (const m of messages) {
        if (m.type === "tool.execution_start" && m.data.toolCallId === toolCallId) {
            return m.data.toolName;
        }
    }
}

const sessionFsConfig: SessionFsConfig = {
    initialCwd: "/",
    sessionStatePath,
    conventions: "posix",
};

function escapeRegExp(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function createTestSessionFsHandler(
    session: CopilotSession,
    provider: VirtualProvider,
    namespace = session.sessionId
): SessionFsProvider {
    const sp = (path: string) => `/${namespace}${path.startsWith("/") ? path : "/" + path}`;

    return {
        async readFile(path: string): Promise<string> {
            return (await provider.readFile(sp(path), "utf8")) as string;
        },
        async writeFile(path: string, content: string): Promise<void> {
            await provider.writeFile(sp(path), content);
        },
        async appendFile(path: string, content: string): Promise<void> {
            await provider.appendFile(sp(path), content);
        },
        async exists(path: string): Promise<boolean> {
            return provider.exists(sp(path));
        },
        async stat(path: string): Promise<SessionFsFileInfo> {
            const st = await provider.stat(sp(path));
            return {
                isFile: st.isFile(),
                isDirectory: st.isDirectory(),
                size: st.size,
                mtime: new Date(st.mtimeMs).toISOString(),
                birthtime: new Date(st.birthtimeMs).toISOString(),
            };
        },
        async mkdir(path: string, recursive: boolean, mode?: number): Promise<void> {
            await provider.mkdir(sp(path), { recursive, mode });
        },
        async readdir(path: string): Promise<string[]> {
            return (await provider.readdir(sp(path))) as string[];
        },
        async readdirWithTypes(path: string): Promise<SessionFsReaddirWithTypesEntry[]> {
            const names = (await provider.readdir(sp(path))) as string[];
            return Promise.all(
                names.map(async (name) => {
                    const st = await provider.stat(sp(`${path}/${name}`));
                    return {
                        name,
                        type: st.isDirectory() ? ("directory" as const) : ("file" as const),
                    };
                })
            );
        },
        async rm(path: string): Promise<void> {
            await provider.unlink(sp(path));
        },
        async rename(src: string, dest: string): Promise<void> {
            await provider.rename(sp(src), sp(dest));
        },
        sqlite: {
            async query() {
                return {
                    columns: [],
                    rows: [],
                    rowsAffected: 0,
                };
            },
            async transaction(statements) {
                return statements.map(() => ({
                    columns: [],
                    rows: [],
                    rowsAffected: 0,
                }));
            },
            async exists() {
                return true;
            },
        },
    };
}
