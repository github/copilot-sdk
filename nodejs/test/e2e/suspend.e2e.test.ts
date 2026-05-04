/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFinished } from "vitest";
import { z } from "zod";
import { approveAll, CopilotClient, defineTool } from "../../src/index.js";
import type { PermissionRequest, PermissionRequestResult, SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const SUSPEND_TIMEOUT_MS = 60_000;
const TEST_TIMEOUT_MS = 180_000;

type Deferred<T> = {
    promise: Promise<T>;
    resolve: (value: T) => void;
    settled: () => boolean;
};

function deferred<T>(): Deferred<T> {
    let resolveFn!: (value: T) => void;
    let isSettled = false;
    const promise = new Promise<T>((resolve) => {
        resolveFn = (value: T) => {
            isSettled = true;
            resolve(value);
        };
    });
    return { promise, resolve: resolveFn, settled: () => isSettled };
}

async function waitWithTimeout<T>(
    promise: Promise<T>,
    timeoutMs: number,
    label: string
): Promise<T> {
    let timer: ReturnType<typeof setTimeout> | undefined;
    try {
        return await Promise.race([
            promise,
            new Promise<T>((_, reject) => {
                timer = setTimeout(() => reject(new Error(`Timeout: ${label}`)), timeoutMs);
            }),
        ]);
    } finally {
        if (timer) clearTimeout(timer);
    }
}

function onTestFinishedForceStop(client: CopilotClient): void {
    onTestFinished(async () => {
        try {
            await client.forceStop();
        } catch {
            // Ignore cleanup errors
        }
    });
}

describe("Suspend RPC", async () => {
    const { copilotClient: client, env, workDir } = await createSdkTestContext();
    const SHARED_TOKEN = "suspend-shared-test-token";

    function createTcpServer(): CopilotClient {
        const server = new CopilotClient({
            cwd: workDir,
            env,
            cliPath: process.env.COPILOT_CLI_PATH,
            useStdio: false,
            tcpConnectionToken: SHARED_TOKEN,
        });
        onTestFinishedForceStop(server);
        return server;
    }

    function createConnectingClient(cliUrl: string): CopilotClient {
        const connectedClient = new CopilotClient({ cliUrl, tcpConnectionToken: SHARED_TOKEN });
        onTestFinishedForceStop(connectedClient);
        return connectedClient;
    }

    function getCliUrl(server: CopilotClient): string {
        const port = (server as unknown as { actualPort: number | null }).actualPort;
        if (!port) {
            throw new Error("Expected the test server to be listening on a TCP port.");
        }
        return `localhost:${port}`;
    }

    it("should suspend idle session without throwing", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await session.sendAndWait({ prompt: "Reply with: SUSPEND_IDLE_OK" });

        await waitWithTimeout(session.rpc.suspend(), SUSPEND_TIMEOUT_MS, "session.rpc.suspend");

        await session.disconnect();
    });

    it(
        "should allow resume and continue conversation after suspend",
        { timeout: TEST_TIMEOUT_MS },
        async () => {
            const server = createTcpServer();
            await server.start();
            const cliUrl = getCliUrl(server);

            let sessionId: string;
            {
                const client1 = createConnectingClient(cliUrl);
                const session1 = await client1.createSession({ onPermissionRequest: approveAll });
                sessionId = session1.sessionId;

                await session1.sendAndWait({
                    prompt: "Remember the magic word: SUSPENSE. Reply with: SUSPEND_TURN_ONE",
                });

                await waitWithTimeout(
                    session1.rpc.suspend(),
                    SUSPEND_TIMEOUT_MS,
                    "session1.rpc.suspend"
                );
                await session1.disconnect();
            }

            const client2 = createConnectingClient(cliUrl);
            const session2 = await client2.resumeSession(sessionId, {
                onPermissionRequest: approveAll,
            });

            const followUp = await session2.sendAndWait({
                prompt: "What was the magic word I asked you to remember? Reply with just the word.",
            });
            expect(followUp?.data.content ?? "").toMatch(/SUSPENSE/i);

            await session2.disconnect();
        }
    );

    it("should cancel pending permission request when suspending", async () => {
        const permissionHandlerEntered = deferred<PermissionRequest>();
        const releasePermissionHandler = deferred<PermissionRequestResult>();
        let toolInvoked = false;

        const session = await client.createSession({
            tools: [
                defineTool("suspend_cancel_permission_tool", {
                    description:
                        "Transforms a value (should not run when suspend cancels permission)",
                    parameters: z.object({
                        value: z.string().describe("Value to transform"),
                    }),
                    handler: ({ value }) => {
                        toolInvoked = true;
                        return `SHOULD_NOT_RUN_${value}`;
                    },
                }),
            ],
            onPermissionRequest: (request) => {
                permissionHandlerEntered.resolve(request);
                return releasePermissionHandler.promise;
            },
        });

        try {
            await session.send({
                prompt: "Use suspend_cancel_permission_tool with value 'omega', then reply with the result.",
            });

            const requestObserved = await waitWithTimeout(
                permissionHandlerEntered.promise,
                SUSPEND_TIMEOUT_MS,
                "pending permission request"
            );
            expect(requestObserved.kind).toBe("custom-tool");
            expect((requestObserved as PermissionRequest & { toolName?: string }).toolName).toBe(
                "suspend_cancel_permission_tool"
            );

            await waitWithTimeout(session.rpc.suspend(), SUSPEND_TIMEOUT_MS, "session.rpc.suspend");

            expect(toolInvoked).toBe(false);
        } finally {
            if (!releasePermissionHandler.settled()) {
                releasePermissionHandler.resolve({ kind: "user-not-available" });
            }
            await session.disconnect();
        }
    });

    it("should reject pending external tool when suspending", async () => {
        const toolStarted = deferred<string>();
        const releaseTool = deferred<string>();
        const externalToolRequested = deferred<void>();

        const session = await client.createSession({
            tools: [
                defineTool("suspend_reject_external_tool", {
                    description: "Looks up a value externally",
                    parameters: z.object({
                        value: z.string().describe("Value to look up"),
                    }),
                    handler: async ({ value }) => {
                        toolStarted.resolve(value);
                        return await releaseTool.promise;
                    },
                }),
            ],
            onPermissionRequest: approveAll,
        });

        const unsubscribe = session.on((event: SessionEvent) => {
            if (
                event.type === "external_tool.requested" &&
                event.data.toolName === "suspend_reject_external_tool"
            ) {
                externalToolRequested.resolve();
            }
        });

        try {
            await session.send({
                prompt: "Use suspend_reject_external_tool with value 'sigma', then reply with the result.",
            });

            const [value] = await waitWithTimeout(
                Promise.all([toolStarted.promise, externalToolRequested.promise]),
                SUSPEND_TIMEOUT_MS,
                "pending external tool request"
            );
            expect(value).toBe("sigma");

            await waitWithTimeout(session.rpc.suspend(), SUSPEND_TIMEOUT_MS, "session.rpc.suspend");
        } finally {
            unsubscribe();
            if (!releaseTool.settled()) {
                releaseTool.resolve("RELEASED_AFTER_SUSPEND");
            }
            await session.disconnect();
        }
    });
});
