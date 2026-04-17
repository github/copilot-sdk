/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { SessionCompactionCompleteEvent } from "@github/copilot/sdk";
import { MemoryProvider, VirtualProvider } from "@platformatic/vfs";
import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient } from "../../src/client.js";
import { SessionFsHandler } from "../../src/generated/rpc.js";
import {
    approveAll,
    CopilotSession,
    defineTool,
    SessionEvent,
    type SessionFsConfig,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

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

        const buf = await provider.readFile(p(session.sessionId, "/session-state/events.jsonl"));
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
        expect(await provider.exists(p(sessionId, "/session-state/events.jsonl"))).toBe(true);

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
        const client = new CopilotClient({
            useStdio: false, // Use TCP so we can connect from a second client
            env,
        });
        await client.createSession({ onPermissionRequest: approveAll, createSessionFsHandler });

        // Get the port the first client's runtime is listening on
        const port = (client as unknown as { actualPort: number }).actualPort;

        // Second client tries to connect with a session fs — should fail
        // because sessions already exist on the runtime.
        const client2 = new CopilotClient({
            env,
            logLevel: "error",
            cliUrl: `localhost:${port}`,
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
        expect(toolResult).toContain("/session-state/temp/");
        const filename = toolResult?.match(/(\/session-state\/temp\/[^\s]+)/)?.[1];
        expect(filename).toBeDefined();

        // Verify the file was written with the correct content via the provider
        const fileContent = await provider.readFile(p(session.sessionId, filename!), "utf8");
        expect(fileContent).toBe(suppliedFileContent);
    });

    it("should write workspace metadata via sessionFs", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            createSessionFsHandler,
        });

        const msg = await session.sendAndWait({ prompt: "What is 7 * 8?" });
        expect(msg?.data.content).toContain("56");

        // WorkspaceManager should have created workspace.yaml via sessionFs
        const workspaceYamlPath = p(session.sessionId, "/session-state/workspace.yaml");
        await expect.poll(() => provider.exists(workspaceYamlPath)).toBe(true);
        const yaml = await provider.readFile(workspaceYamlPath, "utf8");
        expect(yaml).toContain("id:");

        // Checkpoint index should also exist
        const indexPath = p(session.sessionId, "/session-state/checkpoints/index.md");
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

        const planPath = p(session.sessionId, "/session-state/plan.md");
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

        const eventsPath = p(session.sessionId, "/session-state/events.jsonl");
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
    sessionStatePath: "/session-state",
    conventions: "posix",
};

function createTestSessionFsHandler(
    session: CopilotSession,
    provider: VirtualProvider
): SessionFsHandler {
    const sp = (sessionId: string, path: string) =>
        `/${sessionId}${path.startsWith("/") ? path : "/" + path}`;

    function mapError(err: unknown): { code: "ENOENT" | "UNKNOWN"; message?: string } {
        const e = err as NodeJS.ErrnoException;
        if (e.code === "ENOENT") return { code: "ENOENT", message: e.message };
        return { code: "UNKNOWN", message: e.message ?? String(err) };
    }

    return {
        readFile: async ({ path }) => {
            try {
                const content = await provider.readFile(sp(session.sessionId, path), "utf8");
                return { content: content as string };
            } catch (err) {
                return { content: "", error: mapError(err) };
            }
        },
        writeFile: async ({ path, content }) => {
            try {
                await provider.writeFile(sp(session.sessionId, path), content);
                return {};
            } catch (err) {
                return { error: mapError(err) };
            }
        },
        appendFile: async ({ path, content }) => {
            try {
                await provider.appendFile(sp(session.sessionId, path), content);
                return {};
            } catch (err) {
                return { error: mapError(err) };
            }
        },
        exists: async ({ path }) => {
            return { exists: await provider.exists(sp(session.sessionId, path)) };
        },
        stat: async ({ path }) => {
            try {
                const st = await provider.stat(sp(session.sessionId, path));
                return {
                    isFile: st.isFile(),
                    isDirectory: st.isDirectory(),
                    size: st.size,
                    mtime: new Date(st.mtimeMs).toISOString(),
                    birthtime: new Date(st.birthtimeMs).toISOString(),
                };
            } catch (err) {
                return { isFile: false, isDirectory: false, size: 0, mtime: new Date().toISOString(), birthtime: new Date().toISOString(), error: mapError(err) };
            }
        },
        mkdir: async ({ path, recursive, mode }) => {
            try {
                await provider.mkdir(sp(session.sessionId, path), {
                    recursive: recursive ?? false,
                    mode,
                });
                return {};
            } catch (err) {
                return { error: mapError(err) };
            }
        },
        readdir: async ({ path }) => {
            try {
                const entries = await provider.readdir(sp(session.sessionId, path));
                return { entries: entries as string[] };
            } catch (err) {
                return { entries: [], error: mapError(err) };
            }
        },
        readdirWithTypes: async ({ path }) => {
            try {
                const names = (await provider.readdir(sp(session.sessionId, path))) as string[];
                const entries = await Promise.all(
                    names.map(async (name) => {
                        const st = await provider.stat(sp(session.sessionId, `${path}/${name}`));
                        return {
                            name,
                            type: st.isDirectory() ? ("directory" as const) : ("file" as const),
                        };
                    })
                );
                return { entries };
            } catch (err) {
                return { entries: [], error: mapError(err) };
            }
        },
        rm: async ({ path }) => {
            try {
                await provider.unlink(sp(session.sessionId, path));
                return {};
            } catch (err) {
                return { error: mapError(err) };
            }
        },
        rename: async ({ src, dest }) => {
            try {
                await provider.rename(sp(session.sessionId, src), sp(session.sessionId, dest));
                return {};
            } catch (err) {
                return { error: mapError(err) };
            }
        },
    };
}
