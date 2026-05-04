/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFinished } from "vitest";
import { z } from "zod";
import { approveAll, CopilotClient, defineTool } from "../../src/index.js";
import type {
    CopilotSession,
    ExternalToolRequestedEvent,
    PermissionRequest,
    PermissionRequestedEvent,
    PermissionRequestResult,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { getFinalAssistantMessage } from "./harness/sdkTestHelper.js";

const PENDING_WORK_TIMEOUT_MS = 60_000;
const TEST_TIMEOUT_MS = 180_000;

function deferred<T>(): {
    promise: Promise<T>;
    resolve: (value: T) => void;
    reject: (reason: unknown) => void;
    settled: () => boolean;
} {
    let resolveFn!: (value: T) => void;
    let rejectFn!: (reason: unknown) => void;
    let isSettled = false;
    const promise = new Promise<T>((resolve, reject) => {
        resolveFn = (value: T) => {
            isSettled = true;
            resolve(value);
        };
        rejectFn = (reason: unknown) => {
            isSettled = true;
            reject(reason);
        };
    });
    return { promise, resolve: resolveFn, reject: rejectFn, settled: () => isSettled };
}

async function waitWithTimeout<T>(
    promise: Promise<T>,
    timeoutMs: number,
    label: string
): Promise<T> {
    let timer: NodeJS.Timeout | undefined;
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

function waitForExternalToolRequests(
    session: CopilotSession,
    toolNames: string[]
): Promise<Record<string, ExternalToolRequestedEvent>> {
    const expected = new Set(toolNames);
    const seen: Record<string, ExternalToolRequestedEvent> = {};
    const d = deferred<Record<string, ExternalToolRequestedEvent>>();
    let timer: NodeJS.Timeout | undefined;

    const unsubscribe = session.on((event) => {
        if (event.type === "external_tool.requested") {
            const evt = event as ExternalToolRequestedEvent;
            if (expected.has(evt.data.toolName)) {
                seen[evt.data.toolName] = evt;
                if (Object.keys(seen).length === expected.size) {
                    if (timer) clearTimeout(timer);
                    unsubscribe();
                    d.resolve({ ...seen });
                }
            }
        } else if (event.type === "session.error") {
            if (timer) clearTimeout(timer);
            unsubscribe();
            d.reject(new Error(event.data.message ?? "session error"));
        }
    });

    timer = setTimeout(() => {
        unsubscribe();
        d.reject(
            new Error(
                `Timeout waiting for external tool request(s): ${Array.from(expected).join(", ")}`
            )
        );
    }, PENDING_WORK_TIMEOUT_MS);

    return d.promise;
}

function waitForPermissionRequest(session: CopilotSession): Promise<PermissionRequestedEvent> {
    const d = deferred<PermissionRequestedEvent>();
    let timer: NodeJS.Timeout | undefined;

    const unsubscribe = session.on((event) => {
        if (event.type === "permission.requested") {
            if (timer) clearTimeout(timer);
            unsubscribe();
            d.resolve(event as PermissionRequestedEvent);
        } else if (event.type === "session.error") {
            if (timer) clearTimeout(timer);
            unsubscribe();
            d.reject(new Error(event.data.message ?? "session error"));
        }
    });

    timer = setTimeout(() => {
        unsubscribe();
        d.reject(new Error("Timeout waiting for permission.requested"));
    }, PENDING_WORK_TIMEOUT_MS);

    return d.promise;
}

describe("Pending work resume", async () => {
    const { env, workDir } = await createSdkTestContext();
    const SHARED_TOKEN = "pending-work-resume-shared-test-token";

    function createTcpServer(): CopilotClient {
        const server = new CopilotClient({
            cwd: workDir,
            env,
            cliPath: process.env.COPILOT_CLI_PATH,
            useStdio: false,
            tcpConnectionToken: SHARED_TOKEN,
        });
        onTestFinished(async () => {
            try {
                await server.forceStop();
            } catch {
                // Ignore cleanup errors
            }
        });
        return server;
    }

    function createConnectingClient(cliUrl: string): CopilotClient {
        const client = new CopilotClient({ cliUrl, tcpConnectionToken: SHARED_TOKEN });
        onTestFinished(async () => {
            try {
                await client.forceStop();
            } catch {
                // Ignore cleanup errors
            }
        });
        return client;
    }

    function getCliUrl(server: CopilotClient): string {
        const port = (server as unknown as { actualPort: number | null }).actualPort;
        if (!port) {
            throw new Error("Expected the test server to be listening on a TCP port.");
        }
        return `localhost:${port}`;
    }

    it(
        "should continue pending permission request after resume",
        { timeout: TEST_TIMEOUT_MS },
        async () => {
            const originalPermissionRequest = deferred<PermissionRequest>();
            const releaseOriginalPermission = deferred<PermissionRequestResult>();
            let resumedToolInvoked = false;

            const server = createTcpServer();
            await server.start();
            const cliUrl = getCliUrl(server);

            const suspendedClient = createConnectingClient(cliUrl);
            const session1 = await suspendedClient.createSession({
                tools: [
                    defineTool("resume_permission_tool", {
                        description: "Transforms a value after permission is granted",
                        parameters: z.object({ value: z.string() }),
                        handler: ({ value }) => `ORIGINAL_SHOULD_NOT_RUN_${value}`,
                    }),
                ],
                onPermissionRequest: (request) => {
                    originalPermissionRequest.resolve(request);
                    return releaseOriginalPermission.promise;
                },
            });
            const sessionId = session1.sessionId;

            try {
                const permissionRequestedP = waitForPermissionRequest(session1);

                await session1.send({
                    prompt: "Use resume_permission_tool with value 'alpha', then reply with the result.",
                });

                const initialRequest = await waitWithTimeout(
                    originalPermissionRequest.promise,
                    PENDING_WORK_TIMEOUT_MS,
                    "originalPermissionRequest"
                );
                const permissionEvent = await permissionRequestedP;
                expect(initialRequest.kind).toBe("custom-tool");

                await suspendedClient.forceStop();

                const resumedTcpClient = createConnectingClient(cliUrl);
                const session2 = await resumedTcpClient.resumeSession(sessionId, {
                    continuePendingWork: true,
                    onPermissionRequest: () => ({ kind: "no-result" }),
                    tools: [
                        defineTool("resume_permission_tool", {
                            description: "Transforms a value after permission is granted",
                            parameters: z.object({ value: z.string() }),
                            handler: ({ value }) => {
                                resumedToolInvoked = true;
                                return `PERMISSION_RESUMED_${value.toUpperCase()}`;
                            },
                        }),
                    ],
                });

                const permissionResult =
                    await session2.rpc.permissions.handlePendingPermissionRequest({
                        requestId: permissionEvent.data.requestId,
                        result: { kind: "approve-once" },
                    });
                expect(permissionResult.success).toBe(true);

                const answer = await waitWithTimeout(
                    getFinalAssistantMessage(session2),
                    PENDING_WORK_TIMEOUT_MS,
                    "final assistant message"
                );

                expect(resumedToolInvoked).toBe(true);
                expect(answer.data.content ?? "").toContain("PERMISSION_RESUMED_ALPHA");

                await session2.disconnect();
            } finally {
                if (!releaseOriginalPermission.settled()) {
                    releaseOriginalPermission.resolve({ kind: "no-result" });
                }
            }
        }
    );

    it(
        "should continue pending external tool request after resume",
        { timeout: TEST_TIMEOUT_MS },
        async () => {
            const originalToolStarted = deferred<string>();
            const releaseOriginalTool = deferred<string>();

            const server = createTcpServer();
            await server.start();
            const cliUrl = getCliUrl(server);

            const suspendedClient = createConnectingClient(cliUrl);
            const session1 = await suspendedClient.createSession({
                tools: [
                    defineTool("resume_external_tool", {
                        description: "Looks up a value after resumption",
                        parameters: z.object({ value: z.string() }),
                        handler: async ({ value }) => {
                            originalToolStarted.resolve(value);
                            return await releaseOriginalTool.promise;
                        },
                    }),
                ],
                onPermissionRequest: approveAll,
            });
            const sessionId = session1.sessionId;

            try {
                const toolRequestsP = waitForExternalToolRequests(session1, [
                    "resume_external_tool",
                ]);

                await session1.send({
                    prompt: "Use resume_external_tool with value 'beta', then reply with the result.",
                });

                const toolEvents = await toolRequestsP;
                const toolEvent = toolEvents["resume_external_tool"];
                expect(
                    await waitWithTimeout(
                        originalToolStarted.promise,
                        PENDING_WORK_TIMEOUT_MS,
                        "originalToolStarted"
                    )
                ).toBe("beta");

                await suspendedClient.forceStop();

                const resumedClient = createConnectingClient(cliUrl);
                const session2 = await resumedClient.resumeSession(sessionId, {
                    continuePendingWork: true,
                    onPermissionRequest: approveAll,
                });

                const toolResult = await session2.rpc.tools.handlePendingToolCall({
                    requestId: toolEvent.data.requestId,
                    result: "EXTERNAL_RESUMED_BETA",
                });
                expect(toolResult.success).toBe(true);

                const answer = await waitWithTimeout(
                    getFinalAssistantMessage(session2),
                    PENDING_WORK_TIMEOUT_MS,
                    "final assistant message"
                );
                expect(answer.data.content ?? "").toContain("EXTERNAL_RESUMED_BETA");

                await session2.disconnect();
            } finally {
                if (!releaseOriginalTool.settled()) {
                    releaseOriginalTool.resolve("ORIGINAL_SHOULD_NOT_WIN");
                }
            }
        }
    );

    it(
        "should continue parallel pending external tool requests after resume",
        { timeout: TEST_TIMEOUT_MS },
        async () => {
            const originalToolAStarted = deferred<string>();
            const originalToolBStarted = deferred<string>();
            const releaseOriginalToolA = deferred<string>();
            const releaseOriginalToolB = deferred<string>();

            const server = createTcpServer();
            await server.start();
            const cliUrl = getCliUrl(server);

            const suspendedClient = createConnectingClient(cliUrl);
            const session1 = await suspendedClient.createSession({
                tools: [
                    defineTool("pending_lookup_a", {
                        description: "Looks up the first value after resumption",
                        parameters: z.object({ value: z.string() }),
                        handler: async ({ value }) => {
                            originalToolAStarted.resolve(value);
                            return await releaseOriginalToolA.promise;
                        },
                    }),
                    defineTool("pending_lookup_b", {
                        description: "Looks up the second value after resumption",
                        parameters: z.object({ value: z.string() }),
                        handler: async ({ value }) => {
                            originalToolBStarted.resolve(value);
                            return await releaseOriginalToolB.promise;
                        },
                    }),
                ],
                onPermissionRequest: approveAll,
            });
            const sessionId = session1.sessionId;

            try {
                const toolRequestsP = waitForExternalToolRequests(session1, [
                    "pending_lookup_a",
                    "pending_lookup_b",
                ]);

                await session1.send({
                    prompt: "Call pending_lookup_a with value 'alpha' and pending_lookup_b with value 'beta', then reply with both results.",
                });

                const toolEvents = await toolRequestsP;
                await waitWithTimeout(
                    Promise.all([originalToolAStarted.promise, originalToolBStarted.promise]),
                    PENDING_WORK_TIMEOUT_MS,
                    "originalToolAStarted/B"
                );
                expect(await originalToolAStarted.promise).toBe("alpha");
                expect(await originalToolBStarted.promise).toBe("beta");

                await suspendedClient.forceStop();

                const resumedClient = createConnectingClient(cliUrl);
                const session2 = await resumedClient.resumeSession(sessionId, {
                    continuePendingWork: true,
                    onPermissionRequest: approveAll,
                });

                const toolA = toolEvents["pending_lookup_a"];
                const toolB = toolEvents["pending_lookup_b"];
                const resultB = await session2.rpc.tools.handlePendingToolCall({
                    requestId: toolB.data.requestId,
                    result: "PARALLEL_B_BETA",
                });
                expect(resultB.success).toBe(true);
                const resultA = await session2.rpc.tools.handlePendingToolCall({
                    requestId: toolA.data.requestId,
                    result: "PARALLEL_A_ALPHA",
                });
                expect(resultA.success).toBe(true);

                const answer = await waitWithTimeout(
                    getFinalAssistantMessage(session2),
                    PENDING_WORK_TIMEOUT_MS,
                    "final assistant message"
                );

                const content = answer.data.content ?? "";
                expect(content).toContain("PARALLEL_A_ALPHA");
                expect(content).toContain("PARALLEL_B_BETA");

                await session2.disconnect();
            } finally {
                if (!releaseOriginalToolA.settled()) {
                    releaseOriginalToolA.resolve("ORIGINAL_A_SHOULD_NOT_WIN");
                }
                if (!releaseOriginalToolB.settled()) {
                    releaseOriginalToolB.resolve("ORIGINAL_B_SHOULD_NOT_WIN");
                }
            }
        }
    );

    it(
        "should resume successfully when no pending work exists",
        { timeout: TEST_TIMEOUT_MS },
        async () => {
            const server = createTcpServer();
            await server.start();
            const cliUrl = getCliUrl(server);

            let sessionId: string;
            {
                const firstClient = createConnectingClient(cliUrl);
                const firstSession = await firstClient.createSession({
                    onPermissionRequest: approveAll,
                });
                sessionId = firstSession.sessionId;

                const firstAnswer = await firstSession.sendAndWait({
                    prompt: "Reply with exactly: NO_PENDING_TURN_ONE",
                });
                expect(firstAnswer?.data.content ?? "").toContain("NO_PENDING_TURN_ONE");

                await firstSession.disconnect();
                await firstClient.forceStop();
            }

            const resumedClient = createConnectingClient(cliUrl);
            const resumedSession = await resumedClient.resumeSession(sessionId, {
                continuePendingWork: true,
                onPermissionRequest: approveAll,
            });

            const followUp = await resumedSession.sendAndWait({
                prompt: "Reply with exactly: NO_PENDING_TURN_TWO",
            });

            expect(followUp?.data.content ?? "").toContain("NO_PENDING_TURN_TWO");

            await resumedSession.disconnect();
        }
    );
});
