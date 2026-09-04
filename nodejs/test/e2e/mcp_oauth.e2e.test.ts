/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import { dirname, resolve } from "node:path";
import { createInterface } from "node:readline";
import { fileURLToPath } from "node:url";
import { describe, expect, it, onTestFinished } from "vitest";
import type { CopilotSession, MCPServerConfig, McpAuthRequest } from "../../src/index.js";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const TEST_MCP_OAUTH_SERVER = resolve(__dirname, "../../../test/harness/test-mcp-oauth-server.mjs");
const EXPECTED_TOKEN = "sdk-host-token";
const REFRESH_TOKEN = `${EXPECTED_TOKEN}-refresh`;
const UPSCOPE_TOKEN = `${EXPECTED_TOKEN}-upscope`;
const REAUTH_TOKEN = `${EXPECTED_TOKEN}-reauth`;
const CHILD_SHUTDOWN_TIMEOUT_MS = 1_000;

describe("MCP OAuth host auth", async () => {
    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            env: {
                COPILOT_MCP_APPS: "true",
                MCP_APPS: "true",
            },
        },
    });

    it("should satisfy MCP OAuth using host-provided token", { timeout: 120_000 }, async () => {
        const oauthServer = await startOAuthMcpServer();
        const serverName = "oauth-protected-mcp";
        let authRequest: McpAuthRequest | undefined;

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            enableMcpApps: true,
            onMcpAuthRequest: async (request) => {
                authRequest = request;
                return {
                    kind: "token",
                    accessToken: EXPECTED_TOKEN,
                    tokenType: "Bearer",
                    expiresIn: 3600,
                };
            },
            mcpServers: {
                [serverName]: {
                    type: "http",
                    url: `${oauthServer.url}/mcp`,
                    tools: ["*"],
                    oauthClientId: "sdk-e2e-client",
                    oauthPublicClient: true,
                } as unknown as MCPServerConfig,
            },
        });
        onTestFinished(() => disconnectSession(session));

        await session.rpc.mcp.reload();
        await waitForMcpServerStatus(session, serverName);

        const tools = await session.rpc.mcp.listTools({ serverName });
        expect(tools.tools.map((tool) => tool.name)).toContain("whoami");

        expect(authRequest).toMatchObject({
            requestId: expect.any(String),
            serverName,
            serverUrl: `${oauthServer.url}/mcp`,
            reason: "initial",
            wwwAuthenticateParams: {
                resourceMetadataUrl: `${oauthServer.url}/.well-known/oauth-protected-resource`,
                scope: "mcp.read",
                error: "invalid_token",
            },
            resourceMetadata: JSON.stringify({
                resource: `${oauthServer.url}/mcp`,
                authorization_servers: [oauthServer.url],
                scopes_supported: ["mcp.read"],
                bearer_methods_supported: ["header"],
            }),
        });

        const requests = await oauthServer.requests();
        expect(requests.some((request) => request.authorization === null)).toBe(true);
        expect(
            requests.some((request) => request.authorization === `Bearer ${EXPECTED_TOKEN}`)
        ).toBe(true);
    });

    it(
        "should resolve pending MCP OAuth request with direct RPC",
        { timeout: 120_000 },
        async () => {
            const oauthServer = await startOAuthMcpServer();
            const serverName = "oauth-direct-rpc-mcp";
            const authRequests = createAsyncQueue<McpAuthRequest>();
            let releaseHandler!: (value: unknown) => void;
            const handlerResult = new Promise<unknown>((resolve) => {
                releaseHandler = resolve;
            });

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                enableMcpApps: true,
                onMcpAuthRequest: async (request) => {
                    authRequests.push(request);
                    await handlerResult;
                    return { kind: "token", accessToken: EXPECTED_TOKEN };
                },
                mcpServers: {
                    [serverName]: {
                        type: "http",
                        url: `${oauthServer.url}/mcp`,
                        tools: ["*"],
                        oauthClientId: "sdk-e2e-client",
                        oauthPublicClient: true,
                    } as unknown as MCPServerConfig,
                },
            });
            onTestFinished(() => disconnectSession(session));

            const reload = session.rpc.mcp.reload();
            const connected = waitForMcpServerStatus(session, serverName);
            let request = await authRequests.next();
            while (
                !(
                    await session.rpc.mcp.oauth.handlePendingRequest({
                        requestId: request.requestId,
                        result: {
                            kind: "token",
                            accessToken: EXPECTED_TOKEN,
                            tokenType: "Bearer",
                            expiresIn: 3600,
                        },
                    })
                ).success
            ) {
                request = await authRequests.next();
            }

            expect(request).toMatchObject({
                requestId: expect.any(String),
                serverName,
                serverUrl: `${oauthServer.url}/mcp`,
                reason: "initial",
                wwwAuthenticateParams: {
                    resourceMetadataUrl: `${oauthServer.url}/.well-known/oauth-protected-resource`,
                    scope: "mcp.read",
                    error: "invalid_token",
                },
            });

            releaseHandler(undefined);
            await reload;
            await connected;
            const tools = await session.rpc.mcp.listTools({ serverName });
            expect(tools.tools.map((tool) => tool.name)).toContain("whoami");
        }
    );

    it(
        "should request host-owned replacement tokens across the MCP OAuth lifecycle",
        { timeout: 120_000 },
        async () => {
            const oauthServer = await startOAuthMcpServer();
            const serverName = "oauth-lifecycle-mcp";
            const authRequests: McpAuthRequest[] = [];
            let refreshCount = 0;

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                enableMcpApps: true,
                onMcpAuthRequest: async (request) => {
                    authRequests.push(request);
                    switch (request.reason) {
                        case "initial":
                            return { kind: "token", accessToken: EXPECTED_TOKEN };
                        case "refresh":
                            refreshCount++;
                            if (refreshCount === 1) {
                                return { kind: "token", accessToken: REFRESH_TOKEN };
                            }
                            return { kind: "cancelled" };
                        case "upscope":
                            return { kind: "token", accessToken: UPSCOPE_TOKEN };
                        case "reauth":
                            return { kind: "token", accessToken: REAUTH_TOKEN };
                    }
                },
                mcpServers: {
                    [serverName]: {
                        type: "http",
                        url: `${oauthServer.url}/mcp`,
                        tools: ["*"],
                        oauthClientId: "sdk-e2e-client",
                        oauthPublicClient: true,
                    } as unknown as MCPServerConfig,
                },
            });
            onTestFinished(() => disconnectSession(session));

            await session.rpc.mcp.reload();
            await waitForMcpServerStatus(session, serverName);
            refreshCount = 0;
            await callWhoami(session, serverName, "refresh");
            await callWhoami(session, serverName, "upscope");
            await callWhoami(session, serverName, "reauth");

            expect(
                authRequests
                    .filter((request) => request.reason !== "initial")
                    .map((request) => request.reason)
            ).toEqual(["refresh", "upscope", "refresh", "reauth"]);

            const upscopeRequest = authRequests.find((request) => request.reason === "upscope");
            expect(upscopeRequest?.wwwAuthenticateParams).toEqual({
                resourceMetadataUrl: `${oauthServer.url}/.well-known/oauth-protected-resource`,
                scope: "mcp.write",
                error: "insufficient_scope",
            });
            expect(upscopeRequest?.resourceMetadata).toBe(
                JSON.stringify({
                    resource: `${oauthServer.url}/mcp`,
                    authorization_servers: [oauthServer.url],
                    scopes_supported: ["mcp.read"],
                    bearer_methods_supported: ["header"],
                })
            );

            const requests = await oauthServer.requests();
            for (const token of [EXPECTED_TOKEN, REFRESH_TOKEN, UPSCOPE_TOKEN, REAUTH_TOKEN]) {
                expect(
                    requests.some((request) => request.authorization === `Bearer ${token}`)
                ).toBe(true);
            }
        }
    );

    it(
        "should cancel pending MCP OAuth requests when the host declines",
        { timeout: 120_000 },
        async () => {
            const oauthServer = await startOAuthMcpServer();
            const serverName = "oauth-cancelled-mcp";
            let resolveAuthRequest!: (request: McpAuthRequest) => void;
            const authRequest = new Promise<McpAuthRequest>((resolve) => {
                resolveAuthRequest = resolve;
            });

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                onMcpAuthRequest: async (request) => {
                    resolveAuthRequest(request);
                    return { kind: "cancelled" };
                },
                mcpServers: {
                    [serverName]: {
                        type: "http",
                        url: `${oauthServer.url}/mcp`,
                        tools: ["*"],
                        oauthClientId: "sdk-e2e-client",
                        oauthPublicClient: true,
                    } as unknown as MCPServerConfig,
                },
            });
            onTestFinished(() => disconnectSession(session));

            await session.rpc.mcp.reload();
            await waitForMcpServerStatus(session, serverName, "needs-auth");

            expect(await authRequest).toMatchObject({
                serverName,
                reason: "initial",
            });
        }
    );
});

async function waitForMcpServerStatus(
    session: CopilotSession,
    serverName: string,
    expectedStatus = "connected"
): Promise<void> {
    let lastStatus = "<not listed>";
    await waitForCondition(
        async () => {
            const result = await session.rpc.mcp.list();
            const server = result.servers.find((entry) => entry.name === serverName);
            lastStatus = server?.status ?? "<not listed>";
            return server?.status === expectedStatus;
        },
        {
            timeoutMs: 60_000,
            intervalMs: 200,
            timeoutMessage: `${serverName} did not reach ${expectedStatus}; last status was ${lastStatus}`,
        }
    );
}

async function callWhoami(
    session: CopilotSession,
    serverName: string,
    scenario: "refresh" | "upscope" | "reauth"
): Promise<void> {
    const result = await session.rpc.mcp.apps.callTool({
        serverName,
        originServerName: serverName,
        toolName: "whoami",
        arguments: { scenario },
    });
    expect(result.content).toEqual([{ type: "text", text: "oauth-test-user" }]);
}

async function startOAuthMcpServer(): Promise<{
    url: string;
    requests: () => Promise<Array<{ authorization: string | null }>>;
}> {
    const child = spawn(process.execPath, [TEST_MCP_OAUTH_SERVER], {
        env: { ...process.env, EXPECTED_TOKEN },
        stdio: ["ignore", "pipe", "pipe"],
    });
    onTestFinished(() => stopChild(child));

    const stderr: string[] = [];
    child.stderr.on("data", (chunk) => stderr.push(String(chunk)));

    const url = await new Promise<string>((resolvePromise, reject) => {
        const rl = createInterface({ input: child.stdout });
        const timeout = setTimeout(() => {
            rl.close();
            reject(new Error(`Timed out waiting for OAuth MCP server. ${stderr.join("")}`));
        }, 10_000);

        child.once("exit", (code, signal) => {
            clearTimeout(timeout);
            rl.close();
            reject(
                new Error(
                    `OAuth MCP server exited before listening. code=${code} signal=${signal} ${stderr.join("")}`
                )
            );
        });

        rl.on("line", (line) => {
            const match = /^Listening: (.+)$/.exec(line);
            if (!match) {
                return;
            }
            clearTimeout(timeout);
            rl.close();
            resolvePromise(match[1]);
        });
    });

    return {
        url,
        requests: async () => {
            const response = await fetch(`${url}/__requests`);
            if (!response.ok) {
                throw new Error(`Failed to fetch OAuth MCP requests: ${response.status}`);
            }
            return response.json();
        },
    };
}

async function disconnectSession(session: CopilotSession): Promise<void> {
    try {
        await session.disconnect();
    } catch {
        // Best-effort cleanup.
    }
}

async function stopChild(child: ChildProcessWithoutNullStreams): Promise<void> {
    if (hasChildExited(child)) {
        return;
    }

    child.kill("SIGTERM");
    if (await waitForChildExit(child, CHILD_SHUTDOWN_TIMEOUT_MS)) {
        return;
    }

    child.kill("SIGKILL");
    if (!(await waitForChildExit(child, CHILD_SHUTDOWN_TIMEOUT_MS))) {
        throw new Error("OAuth MCP server did not exit after SIGKILL");
    }
}

function hasChildExited(child: ChildProcessWithoutNullStreams): boolean {
    return child.exitCode !== null || child.signalCode !== null;
}

function waitForChildExit(
    child: ChildProcessWithoutNullStreams,
    timeoutMs: number
): Promise<boolean> {
    if (hasChildExited(child)) {
        return Promise.resolve(true);
    }

    return new Promise<boolean>((resolvePromise) => {
        let settled = false;
        const finish = (exited: boolean) => {
            if (settled) {
                return;
            }
            settled = true;
            clearTimeout(timeout);
            child.off("exit", onExit);
            resolvePromise(exited);
        };
        const onExit = () => finish(true);
        const timeout = setTimeout(() => finish(false), timeoutMs);

        child.once("exit", onExit);
        if (hasChildExited(child)) {
            onExit();
        }
    });
}

function createAsyncQueue<T>(): { push(value: T): void; next(): Promise<T> } {
    const values: T[] = [];
    const waiters: Array<(value: T) => void> = [];
    return {
        push(value) {
            const waiter = waiters.shift();
            if (waiter) {
                waiter(value);
            } else {
                values.push(value);
            }
        },
        next() {
            const value = values.shift();
            return value === undefined
                ? new Promise<T>((resolvePromise) => waiters.push(resolvePromise))
                : Promise.resolve(value);
        },
    };
}
