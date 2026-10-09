/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import { mkdir, mkdtemp, readFile, realpath, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, test, type TestContext } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import { startComputerUseOAuthServer } from "../../../test/harness/test-mcp-computer-use-oauth-server.mjs";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";

const TEST_MCP_SERVER = fileURLToPath(
    new URL("../../../test/harness/test-mcp-server.mjs", import.meta.url)
);
const SERVER_NAME = "unrelated-oauth";

describe("Computer Use OAuth isolation", async () => {
    const { env } = await createSdkTestContext();

    async function createFixture(context: TestContext, preference?: boolean, startup = false) {
        // The shared harness clears workDir before this fixture's onTestFinished cleanup.
        const directory = await realpath(await mkdtemp(join(tmpdir(), "computer-use-oauth-")));
        const home = join(directory, "home");
        const plugin = join(directory, "computer-use");
        let client: CopilotClient | undefined;
        const startingOAuthServer = startComputerUseOAuthServer({
            deferRegistration: !startup,
            deferInitialChallenge: startup,
            deferAuthorizationMetadata: startup,
            preconfiguredClientId: startup ? "app-client" : "",
        });
        context.onTestFinished(async () => {
            const errors: unknown[] = [];
            const [serverStartup] = await Promise.allSettled([startingOAuthServer]);
            if (serverStartup.status === "fulfilled") {
                serverStartup.value.releaseRegistration();
                serverStartup.value.releaseInitialChallenge();
                serverStartup.value.releaseAuthorizationMetadata();
            }
            if (client) {
                try {
                    await readFile(join(plugin, ".plugin", "plugin.json"), "utf8");
                } catch (error) {
                    errors.push(
                        new Error(
                            "Computer Use OAuth fixture was removed before its client stopped",
                            {
                                cause: error,
                            }
                        )
                    );
                }
                try {
                    errors.push(...(await client.stop()));
                } catch (error) {
                    errors.push(error);
                }
            }
            if (serverStartup.status === "fulfilled") {
                try {
                    await serverStartup.value.close();
                } catch (error) {
                    errors.push(error);
                }
            }
            try {
                await rm(directory, {
                    recursive: true,
                    force: true,
                    maxRetries: 10,
                    retryDelay: 100,
                });
            } catch (error) {
                errors.push(error);
            }
            if (errors.length)
                throw new AggregateError(errors, "Computer Use OAuth cleanup failed");
        });
        const oauthServer = await startingOAuthServer;
        context.signal.throwIfAborted();
        await mkdir(home);
        await mkdir(join(plugin, ".plugin"), { recursive: true });
        execFileSync("git", ["init", "--quiet", directory], { windowsHide: true });
        if (preference !== undefined) {
            await writeFile(
                join(home, "settings.json"),
                JSON.stringify({
                    enabledPlugins: { "computer-use": preference },
                })
            );
        }
        await writeFile(
            join(plugin, ".plugin", "plugin.json"),
            JSON.stringify({
                name: "computer-use",
                version: "1.0.0",
                mcpServers: "./.mcp.json",
            })
        );
        await writeFile(
            join(plugin, ".mcp.json"),
            JSON.stringify({
                mcpServers: {
                    "computer-use": {
                        command: process.execPath,
                        args: [TEST_MCP_SERVER, "--server-name", "computer-use"],
                        cwd: dirname(TEST_MCP_SERVER),
                        tools: ["*"],
                    },
                },
            })
        );
        client = new CopilotClient({
            workingDirectory: directory,
            env: {
                ...env,
                COPILOT_HOME: home,
                GH_CONFIG_DIR: home,
                XDG_CONFIG_HOME: home,
                XDG_STATE_HOME: home,
                COPILOT_DISABLE_KEYTAR: "1",
            },
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
        });
        await client.start();
        context.signal.throwIfAborted();
        await client.rpc.plugins.builtin.set({ paths: [plugin] });
        const session = await client.createSession({
            workingDirectory: directory,
            onPermissionRequest: approveAll,
            onMcpAuthRequest: async () => ({ kind: "cancelled" }),
            mcpServers: {
                [SERVER_NAME]: {
                    type: "http",
                    url: `${oauthServer.url}/mcp`,
                    tools: ["*"],
                    ...(startup ? { oauthClientId: "app-client", oauthPublicClient: true } : {}),
                },
            },
        });
        return { client, session, oauthServer };
    }

    test.for([
        { preference: "unset", hosted: false },
        { preference: "disabled", hosted: false },
        { preference: "unset", hosted: true },
        { preference: "disabled", hosted: true },
    ] as const)(
        "returns the initial OAuth URL across Computer Use toggles ($preference, hosted=$hosted)",
        { timeout: 90_000 },
        async ({ preference, hosted }, context) => {
            const { expect } = context;
            const { client, session, oauthServer } = await createFixture(
                context,
                preference === "disabled" ? false : undefined
            );
            const [initial] = await Promise.all([
                session.rpc.mcp.list(),
                session.rpc.plugins.list(),
            ]);
            expect(initial.servers.map((server) => server.name)).not.toContain("computer-use");
            const login = session.rpc.mcp.oauth
                .login({
                    serverName: SERVER_NAME,
                    forceReauth: true,
                    ...(hosted ? { redirectUri: "https://agent.example.test/oauth/callback" } : {}),
                })
                .then(
                    (result) => ({ result }),
                    (error: unknown) => ({ error })
                );
            // Registration is after lifetime capture but before the browser URL is returned.
            await expect
                .poll(() => oauthServer.requests.map((request) => request.path), {
                    timeout: 60_000,
                })
                .toContain("/register");
            for (const enabled of [true, false, true]) {
                if (enabled) await client.rpc.plugins.enable({ names: ["computer-use"] });
                else await client.rpc.plugins.disable({ names: ["computer-use"] });
                await expect
                    .poll(
                        async () =>
                            (await session.rpc.mcp.list()).servers.find(
                                (candidate) => candidate.name === "computer-use"
                            )?.status,
                        { timeout: 60_000 }
                    )
                    .toBe(enabled ? "connected" : undefined);
            }
            oauthServer.releaseRegistration();
            const outcome = await login;
            if ("error" in outcome) throw outcome.error;
            const result = outcome.result;
            expect(result.authorizationUrl).toBeDefined();
            const authorization = new URL(result.authorizationUrl!);
            expect(authorization.origin).toBe(oauthServer.url);
            expect(authorization.searchParams.get("client_id")).toBe("registered-client");

            await client.rpc.plugins.disable({ names: ["computer-use"] });
            await expect
                .poll(
                    async () => (await session.rpc.mcp.list()).servers.map((server) => server.name),
                    { timeout: 60_000 }
                )
                .not.toContain("computer-use");
            const callback = new URL(authorization.searchParams.get("redirect_uri")!);
            callback.searchParams.set("code", "accepted-code");
            callback.searchParams.set("state", authorization.searchParams.get("state")!);
            if (hosted) {
                expect(result.authorizationId).toBe(authorization.searchParams.get("state"));
                expect(callback.origin).toBe("https://agent.example.test");
                await session.rpc.mcp.oauth.complete({
                    authorizationId: result.authorizationId!,
                    callbackUrl: callback.toString(),
                });
            } else {
                const response = await fetch(callback);
                expect(response.ok).toBe(true);
            }
            await expect
                .poll(
                    async () =>
                        (await session.rpc.mcp.list()).servers.find(
                            (server) => server.name === SERVER_NAME
                        )?.status,
                    { timeout: 60_000 }
                )
                .toBe("connected");
            expect(
                (await session.rpc.mcp.listTools({ serverName: SERVER_NAME })).tools.map(
                    (tool) => tool.name
                )
            ).toContain("whoami");
            const tokens = oauthServer.requests.filter((request) => request.path === "/token");
            expect(tokens).toHaveLength(1);
            expect(new URLSearchParams(tokens[0].body).get("client_id")).toBe("registered-client");
        }
    );

    test.for(["unset", "disabled"] as const)(
        "preserves preconfigured-client OAuth overlapping initial discovery (%s preference)",
        { timeout: 90_000 },
        async (preference, context) => {
            const { expect } = context;
            const { client, session, oauthServer } = await createFixture(
                context,
                preference === "disabled" ? false : undefined,
                true
            );
            const discovery = session.rpc.mcp.list().then(
                (result) => ({ result }),
                (error: unknown) => ({ error })
            );
            const login = session.rpc.mcp.oauth.login({ serverName: SERVER_NAME }).then(
                (result) => ({ result }),
                (error: unknown) => ({ error })
            );
            await expect
                .poll(() => oauthServer.requests.filter((request) => request.path === "/mcp"), {
                    timeout: 60_000,
                })
                .not.toHaveLength(0);
            await session.rpc.plugins.list();
            oauthServer.releaseInitialChallenge();
            await expect
                .poll(() => oauthServer.requests.map((request) => request.path), {
                    timeout: 60_000,
                })
                .toContain("/.well-known/oauth-authorization-server");
            const discovered = await discovery;
            if ("error" in discovered) throw discovered.error;
            expect(discovered.result.servers.map((server) => server.name)).not.toContain(
                "computer-use"
            );
            await client.rpc.plugins.enable({ names: ["computer-use"] });
            await expect
                .poll(
                    async () =>
                        (await session.rpc.mcp.list()).servers.find(
                            (server) => server.name === "computer-use"
                        )?.status,
                    { timeout: 60_000 }
                )
                .toBe("connected");
            oauthServer.releaseAuthorizationMetadata();
            const outcome = await login;
            if ("error" in outcome) throw outcome.error;
            const authorization = new URL(outcome.result.authorizationUrl!);
            expect(authorization.searchParams.get("client_id")).toBe("app-client");
            const callback = new URL(authorization.searchParams.get("redirect_uri")!);
            callback.searchParams.set("code", "accepted-code");
            callback.searchParams.set("state", authorization.searchParams.get("state")!);
            expect((await fetch(callback)).ok).toBe(true);
            await expect
                .poll(
                    async () =>
                        (await session.rpc.mcp.list()).servers.find(
                            (server) => server.name === SERVER_NAME
                        )?.status,
                    { timeout: 60_000 }
                )
                .toBe("connected");
            expect(
                (await session.rpc.mcp.listTools({ serverName: SERVER_NAME })).tools.map(
                    (tool) => tool.name
                )
            ).toContain("whoami");
            expect(
                oauthServer.requests.filter((request) => request.path === "/register")
            ).toHaveLength(0);
            const tokens = oauthServer.requests.filter((request) => request.path === "/token");
            expect(tokens).toHaveLength(1);
            expect(new URLSearchParams(tokens[0].body).get("client_id")).toBe("app-client");
        }
    );

    test("still cancels initial OAuth when its own server is reloaded", async (context) => {
        const { expect } = context;
        const { session, oauthServer } = await createFixture(context);
        await session.rpc.mcp.list();
        const login = session.rpc.mcp.oauth
            .login({ serverName: SERVER_NAME, forceReauth: true })
            .then(
                (result) => ({ result }),
                (error: unknown) => ({ error })
            );
        await expect
            .poll(() => oauthServer.requests.map((request) => request.path), { timeout: 60_000 })
            .toContain("/register");
        await session.rpc.mcp.reload();
        oauthServer.releaseRegistration();
        expect(await login).toEqual({
            error: expect.objectContaining({
                message: expect.stringContaining(
                    "MCP OAuth was cancelled because its original requester or configuration changed."
                ),
            }),
        });
        expect(oauthServer.requests.filter((request) => request.path === "/token")).toHaveLength(0);
    }, 90_000);
});
