/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { SessionCompactionCompleteEvent } from "@github/copilot/sdk";
import { MemoryProvider, VirtualProvider } from "@platformatic/vfs";
import { mkdtempSync, realpathSync } from "fs";
import { tmpdir } from "os";
import { join } from "path";
import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient } from "../../src/client.js";
import { createSessionFsAdapter } from "../../src/index.js";
import type { SessionFsReaddirWithTypesEntry } from "../../src/generated/rpc.js";
import {
    approveAll,
    CopilotSession,
    defineTool,
    SessionEvent,
    type SessionFsConfig,
    type SessionFsProvider,
    type SessionFsFileInfo,
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
    const createSessionFsHandler = (session: CopilotSession) =>
        createTestSessionFsHandler(session, provider);

    // Helpers to build session-namespaced paths for direct provider assertions
    const p = (sessionId: string, path: string) =>
        `/${sessionId}${path.startsWith("/") ? path : "/" + path}`;

    const { copilotClient: client, env } = await createSdkTestContext({
        copilotClientOptions: { sessionFs: sessionFsConfig },
    });

    it("should route file operations through the session fs provider", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            createSessionFsHandler,
        });

        const msg = await session.sendAndWait({ prompt: "What is 100 + 200?" });
        expect(msg?.data.content).toContain("300");
        await session.disconnect();

        const buf = await provider.readFile(
            p(session.sessionId, `${sessionStatePath}/events.jsonl`)
        );
        const content = buf.toString("utf8");
        expect(content).toContain("300");
    });

    it("should load session data from fs provider on resume", async () => {
        const session1 = await client.createSession({
            onPermissionRequest: approveAll,
            createSessionFsHandler,
        });
        const sessionId = session1.sessionId;

        const msg = await session1.sendAndWait({ prompt: "What is 50 + 50?" });
        expect(msg?.data.content).toContain("100");
        await session1.disconnect();

        // The events file should exist before resume
        expect(await provider.exists(p(sessionId, `${sessionStatePath}/events.jsonl`))).toBe(true);

        const session2 = await client.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            createSessionFsHandler,
        });

        // Send another message to verify the session is functional after resume
        const msg2 = await session2.sendAndWait({ prompt: "What is that times 3?" });
        await session2.disconnect();
        expect(msg2?.data.content).toContain("300");
    });

    it("should reject setProvider when sessions already exist", async () => {
        const tcpConnectionToken = "session-fs-test-token";
        const client = new CopilotClient({
            useStdio: false, // Use TCP so we can connect from a second client
            tcpConnectionToken,
            env,
        });
        onTestFinished(() => client.forceStop());
        await client.createSession({ onPermissionRequest: approveAll, createSessionFsHandler });

        const { actualPort: port } = client as unknown as { actualPort: number };

        // Second client tries to connect with a session fs — should fail
        // because sessions already exist on the runtime.
        const client2 = new CopilotClient({
            env,
            logLevel: "error",
            cliUrl: `localhost:${port}`,
            tcpConnectionToken,
            sessionFs: sessionFsConfig,
        });
        onTestFinished(() => client2.forceStop());

        await expect(client2.start()).rejects.toThrow();
    });

    it("should map large output handling into sessionFs", async () => {
        const suppliedFileContent = "x".repeat(100_000);
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            createSessionFsHandler,
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

        // The tool result should reference a temp file under the session state path
        const messages = await session.getMessages();
        const toolResult = findToolCallResult(messages, "get_big_string");
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
            createSessionFsHandler,
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
            createSessionFsHandler,
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
            createSessionFsHandler,
        });

        let compactionEvent: SessionCompactionCompleteEvent | undefined;
        session.on("session.compaction_complete", (evt) => (compactionEvent = evt));

        await session.sendAndWait({ prompt: "What is 2+2?" });

        const eventsPath = p(session.sessionId, `${sessionStatePath}/events.jsonl`);
        await expect.poll(() => provider.exists(eventsPath)).toBe(true);
        const contentBefore = await provider.readFile(eventsPath, "utf8");
        expect(contentBefore).not.toContain("checkpointNumber");

        await session.rpc.history.compact();
        await expect.poll(() => compactionEvent).toBeDefined();
        expect(compactionEvent!.data.success).toBe(true);

        // Verify the events file was rewritten with a checkpoint via sessionFs
        await expect
            .poll(() => provider.readFile(eventsPath, "utf8"))
            .toContain("checkpointNumber");
    });
});

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
    provider: VirtualProvider
): SessionFsProvider {
    const sp = (path: string) => `/${session.sessionId}${path.startsWith("/") ? path : "/" + path}`;

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
    };
}
