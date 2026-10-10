/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawn } from "node:child_process";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { createInterface } from "node:readline";
import { fileURLToPath } from "node:url";
import { describe, expect, it, onTestFinished } from "vitest";
import type { CopilotSession, MCPServerConfig, SessionEvent } from "../../src/index.js";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { stopChildProcess, waitForCondition } from "./harness/sdkTestHelper.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const TEST_MCP_OAUTH_SERVER = resolve(__dirname, "../../../test/harness/test-mcp-oauth-server.mjs");
const TEST_MCP_SERVER = resolve(__dirname, "../../../test/harness/test-mcp-server.mjs");
const INITIAL_TOKEN = "sdk-scoped-sign-in-token";
const ROTATED_TOKEN = `${INITIAL_TOKEN}-reauth`;
const REDIRECT_URI = "https://agent.example.test/oauth/callback";
const STEP_TIMEOUT_MS = 60_000;

/**
 * A sign-in for one MCP server, completed on a separate session that shares the token store
 * (as the app's settings sign-in does), must reconnect only that server in a live session.
 */
describe("MCP sign-in for one server", async () => {
    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: { env: { COPILOT_DISABLE_KEYTAR: "1" } },
    });

    it(
        "connects the signed-in server without restarting peers or cancelling another sign-in",
        { timeout: 180_000 },
        async () => {
            const live = await startLiveSession();

            await signInOnSeparateSession(live.bravoServer, "accepted-code", INITIAL_TOKEN);
            await authenticationStateChanged(live.session, "bravo");
            await waitForMcpServerStatus(live.session, "bravo", "connected");

            expect(await live.alphaStarts()).toBe(1);
            expect(live.statusChangesFor("alpha")).toEqual([]);
            await completeHostedLogin(live.session, live.deltaAuthorizationId, "accepted-code");
            await waitForMcpServerStatus(live.session, "delta", "connected");
        }
    );

    it(
        "reconnects a connected server onto credentials rotated by another sign-in",
        { timeout: 180_000 },
        async () => {
            const live = await startLiveSession();
            await signInOnSeparateSession(live.bravoServer, "accepted-code", INITIAL_TOKEN);
            await authenticationStateChanged(live.session, "bravo");
            await waitForMcpServerStatus(live.session, "bravo", "connected");

            await signInOnSeparateSession(live.bravoServer, "accepted-code-reauth", ROTATED_TOKEN);
            const rotationStart = (await live.bravoServer.requests()).length;
            await authenticationStateChanged(live.session, "bravo");

            await waitForInitializeWith(
                live.bravoServer,
                rotationStart,
                ROTATED_TOKEN,
                "bravo did not reinitialize with the rotated token"
            );
            await waitForMcpServerStatus(live.session, "bravo", "connected");
            expect(await live.alphaStarts()).toBe(1);
            expect(live.statusChangesFor("alpha")).toEqual([]);
            await completeHostedLogin(live.session, live.deltaAuthorizationId, "accepted-code");
            await waitForMcpServerStatus(live.session, "delta", "connected");
        }
    );

    it(
        "still restarts every server and cancels pending sign-ins on an explicit reload",
        { timeout: 180_000 },
        async () => {
            const live = await startLiveSession();

            await live.session.rpc.mcp.reload();
            await waitForMcpServerStatus(live.session, "alpha", "connected");

            expect(await live.alphaStarts()).toBe(2);
            await expect(
                completeHostedLogin(live.session, live.deltaAuthorizationId, "accepted-code")
            ).rejects.toThrow();
        }
    );

    /**
     * Starts a live session with a plain stdio server (alpha), an OAuth server awaiting sign-in
     * (bravo), and a second OAuth server (delta) with an in-progress hosted sign-in.
     */
    async function startLiveSession() {
        const directory = await mkdtemp(join(tmpdir(), "mcp-scoped-sign-in-"));
        onTestFinished(() => rm(directory, { recursive: true, force: true }));
        const alphaMarker = join(directory, "alpha-starts.txt");
        const bravoServer = await startOAuthMcpServer();
        const deltaServer = await startOAuthMcpServer();
        const events: SessionEvent[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            onEvent: (event) => events.push(event),
            mcpServers: {
                alpha: {
                    type: "local",
                    command: process.execPath,
                    args: [
                        TEST_MCP_SERVER,
                        "--server-name",
                        "alpha",
                        "--startup-marker",
                        alphaMarker,
                    ],
                    tools: ["*"],
                } as MCPServerConfig,
                bravo: oauthServerConfig(bravoServer.url),
                delta: oauthServerConfig(deltaServer.url),
            },
        });
        onTestFinished(() => disconnectSession(session));
        await waitForMcpServerStatus(session, "alpha", "connected");
        await waitForMcpServerStatus(session, "bravo", "needs-auth");
        await waitForMcpServerStatus(session, "delta", "needs-auth");
        const deltaLogin = await session.rpc.mcp.oauth.login({
            serverName: "delta",
            redirectUri: REDIRECT_URI,
        });
        expect(deltaLogin.authorizationId).toBeDefined();
        // Live status can precede its asynchronous event, so observe startup before measuring peer changes.
        await waitForCondition(
            () =>
                events.some(
                    (event) =>
                        event.type === "session.mcp_server_status_changed" &&
                        event.data.serverName === "alpha" &&
                        event.data.status === "connected"
                ),
            {
                timeoutMs: STEP_TIMEOUT_MS,
                timeoutMessage: "alpha's initial connected event did not arrive",
            }
        );
        const baseline = events.length;

        return {
            session,
            bravoServer,
            deltaAuthorizationId: deltaLogin.authorizationId!,
            alphaStarts: async () =>
                (await readFile(alphaMarker, "utf8")).split("\n").filter(Boolean).length,
            statusChangesFor: (serverName: string) =>
                events
                    .slice(baseline)
                    .flatMap((event) =>
                        event.type === "session.mcp_server_status_changed" &&
                        event.data.serverName === serverName
                            ? [event.data.status]
                            : []
                    ),
        };
    }

    /**
     * Completes a hosted sign-in for bravo on another session sharing the token store, then waits
     * for that session to initialize with the issued token. Completing a login only delivers the
     * callback, and the separate session may already be connected with an older shared token, so
     * the initialize is what shows the issued token has been stored.
     */
    async function signInOnSeparateSession(
        server: OAuthMcpServer,
        code: string,
        issuedToken: string
    ): Promise<void> {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers: { bravo: oauthServerConfig(server.url) },
        });
        try {
            const login = await session.rpc.mcp.oauth.login({
                serverName: "bravo",
                redirectUri: REDIRECT_URI,
                forceReauth: true,
            });
            expect(login.authorizationId).toBeDefined();
            const loginStart = (await server.requests()).length;
            await completeHostedLogin(session, login.authorizationId!, code);
            await waitForInitializeWith(
                server,
                loginStart,
                issuedToken,
                "the separate session did not initialize bravo with the issued token"
            );
            await waitForMcpServerStatus(session, "bravo", "connected");
        } finally {
            await disconnectSession(session);
        }
    }
});

/**
 * Reports new credentials for one server. A server still awaiting sign-in hands off to
 * background recovery and may report that it still requires authentication; callers then
 * wait for the server's status.
 */
async function authenticationStateChanged(
    session: CopilotSession,
    serverName: string
): Promise<void> {
    try {
        await session.rpc.mcp.oauth.authenticationStateChanged({ serverName });
    } catch (error) {
        expect(String(error)).toContain(`MCP server "${serverName}" still requires authentication`);
    }
}

/**
 * The configured client ID keys the shared token entry, so the rotation test also proves that
 * the live session reads back the entry its token came from rather than the unkeyed one.
 */
function oauthServerConfig(url: string): MCPServerConfig {
    return {
        type: "http",
        url: `${url}/mcp`,
        tools: ["*"],
        oauthClientId: "sdk-scoped-sign-in-client",
        oauthPublicClient: true,
    } as unknown as MCPServerConfig;
}

async function completeHostedLogin(
    session: CopilotSession,
    authorizationId: string,
    code: string
): Promise<void> {
    const callbackUrl = new URL(REDIRECT_URI);
    callbackUrl.searchParams.set("code", code);
    callbackUrl.searchParams.set("state", authorizationId);
    await session.rpc.mcp.oauth.complete({
        authorizationId,
        callbackUrl: callbackUrl.toString(),
    });
}

async function waitForMcpServerStatus(
    session: CopilotSession,
    serverName: string,
    expectedStatus: string
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
            timeoutMs: STEP_TIMEOUT_MS,
            intervalMs: 200,
            timeoutMessage: `${serverName} did not reach ${expectedStatus}; last status was ${lastStatus}`,
        }
    );
}

interface OAuthMcpServer {
    url: string;
    requests: () => Promise<
        Array<{ authorization: string | null; body: string | null; path: string }>
    >;
}

/** Waits for an MCP initialize request carrying `token` after the first `start` requests. */
async function waitForInitializeWith(
    server: OAuthMcpServer,
    start: number,
    token: string,
    timeoutMessage: string
): Promise<void> {
    await waitForCondition(
        async () =>
            (await server.requests())
                .slice(start)
                .some(
                    (request) =>
                        request.path === "/mcp" &&
                        request.authorization === `Bearer ${token}` &&
                        request.body?.includes('"initialize"')
                ),
        { timeoutMs: STEP_TIMEOUT_MS, intervalMs: 200, timeoutMessage }
    );
}

async function startOAuthMcpServer(): Promise<OAuthMcpServer> {
    const child = spawn(process.execPath, [TEST_MCP_OAUTH_SERVER], {
        env: { ...process.env, EXPECTED_TOKEN: INITIAL_TOKEN },
        stdio: ["ignore", "pipe", "pipe"],
    });
    onTestFinished(() => stopChildProcess(child));

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
