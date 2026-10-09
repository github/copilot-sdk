/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdir, readFile, readdir, writeFile } from "node:fs/promises";
import { randomUUID } from "node:crypto";
import { join } from "node:path";
import { describe, expect, it, onTestFinished } from "vitest";
import { approveAll, RuntimeConnection, CopilotClient } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";
import { ConnectProxy } from "../../../test/harness/connectProxy.js";
import { createE2eRequestHandler } from "../../../test/harness/mockHandlers.js";

describe("account-derived WorkIQ", async () => {
    const { env, workDir, openAiEndpoint } = await createSdkTestContext();

    it("discovers WorkIQ only on supported platforms without requesting authentication or persisting a server", async () => {
        const profile = join(workDir, "workiq-profile");
        await mkdir(profile, { recursive: true });
        await writeFile(
            join(profile, "config.json"),
            JSON.stringify({
                loggedInUsers: [
                    {
                        host: "https://substrate.office.com",
                        login: "workiq-test-account",
                        kind: "loki",
                        derivedFrom: "entra:workiq-account-a",
                    },
                ],
            })
        );
        const client = new CopilotClient({
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            workingDirectory: workDir,
            env: { ...env, COPILOT_HOME: profile, COPILOT_DISABLE_KEYTAR: "1" },
        });
        onTestFinished(() => client.stop());
        let authenticationRequests = 0;
        const session = await client.createSession({
            configDirectory: profile,
            onPermissionRequest: approveAll,
            onMcpAuthRequest: async () => {
                authenticationRequests++;
                return { kind: "cancelled" };
            },
        });
        const accounts = await session.rpc.accounts.enumerate({ query: { kind: "accounts" } });
        expect(accounts.items).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ kind: "entra", selectionId: "entra:workiq-account-a" }),
            ])
        );
        const discovered = await session.rpc.mcp.list();
        const expected =
            process.platform === "linux"
                ? []
                : [
                      expect.objectContaining({
                          name: "WorkIQ",
                          source: "account",
                          status: "disabled",
                          url: "https://workiq.svc.cloud.microsoft/mcp",
                      }),
                  ];
        expect(discovered.servers.filter((server) => server.name === "WorkIQ")).toEqual(expected);
        expect(discovered).not.toHaveProperty("accountServerConfigs");
        // Gating off WorkIQ can leave this fixture with no MCP configuration to reload.
        if (discovered.servers.length === 0) {
            await expect(session.rpc.mcp.reload()).rejects.toThrow(
                "MCP config reload not available"
            );
        } else {
            await session.rpc.mcp.reload();
        }
        const reloaded = await session.rpc.mcp.list();
        expect(reloaded.servers.filter((server) => server.name === "WorkIQ")).toEqual(expected);
        expect(authenticationRequests).toBe(0);
        await expect(readFile(join(profile, "mcp-config.json"))).rejects.toMatchObject({
            code: "ENOENT",
        });
    });

    it("preserves an explicitly configured WorkIQ alias instead of adding another server", async () => {
        const profile = join(workDir, "workiq-override-profile");
        await mkdir(profile, { recursive: true });
        await writeFile(
            join(profile, "config.json"),
            JSON.stringify({
                loggedInUsers: [
                    {
                        host: "https://substrate.office.com",
                        login: "workiq-test-account",
                        kind: "loki",
                        derivedFrom: "entra:workiq-account-a",
                    },
                ],
            })
        );
        const client = new CopilotClient({
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            workingDirectory: workDir,
            env: { ...env, COPILOT_HOME: profile, COPILOT_DISABLE_KEYTAR: "1" },
        });
        onTestFinished(() => client.stop());
        const session = await client.createSession({
            configDirectory: profile,
            onPermissionRequest: approveAll,
            mcpServers: {
                "my-workiq": {
                    type: "http",
                    url: "https://workiq.svc.cloud.microsoft/mcp",
                    tools: ["chosen-tool"],
                },
                "my-sse": {
                    type: "sse",
                    url: "https://example.test/events",
                    tools: ["*"],
                },
                "my-local": {
                    type: "stdio",
                    command: "unused-while-disabled",
                    tools: ["*"],
                },
            },
            disabledMcpServers: ["my-workiq", "my-sse", "my-local"],
        });
        const result = await session.rpc.mcp.list();
        expect(result.servers.some((server) => server.name === "WorkIQ")).toBe(false);
        expect(result.servers).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    name: "my-workiq",
                    status: "disabled",
                    url: "https://workiq.svc.cloud.microsoft/mcp",
                }),
                expect.objectContaining({
                    name: "my-sse",
                    status: "disabled",
                    url: "https://example.test/events",
                }),
                expect.objectContaining({ name: "my-local", status: "disabled" }),
            ])
        );
        expect(result.servers.find((server) => server.name === "my-local")).not.toHaveProperty(
            "url"
        );
        expect(result).not.toHaveProperty("accountServerConfigs");
    });

    it.skipIf(process.platform === "linux").each(["silent", "browser"] as const)(
        "authenticates through %s, persists renewal, and disconnects on parent logout",
        async (authentication) => {
            const profile = join(workDir, `workiq-${authentication}-profile`);
            await mkdir(profile, { recursive: true });
            const objectId = "workiq-object";
            const tenantId = randomUUID();
            const parent = `entra:${objectId}@${tenantId}`;
            await writeFile(
                join(profile, "config.json"),
                JSON.stringify({
                    loggedInUsers: [
                        {
                            host: "https://substrate.office.com",
                            login: "workiq-test-account",
                            kind: "loki",
                            derivedFrom: parent,
                        },
                    ],
                    authTokens: {
                        [`entra:${parent}:entra`]: {
                            token: JSON.stringify({ web: { refresh_token: "parent-refresh" } }),
                        },
                    },
                })
            );
            const resource = "fdcc1f02-fc51-4226-8753-f668596af7f7";
            const scope = `${resource}/access`;
            const requests: Array<{ host: string; path: string }> = [];
            const grants: URLSearchParams[] = [];
            let browserScopes: string[] = [];
            let initializeRequests = 0;
            const fallback = createE2eRequestHandler({ capiProxyUrl: openAiEndpoint.url });
            const proxy = new ConnectProxy(
                async (req, res, host) => {
                    if (
                        host !== "workiq.svc.cloud.microsoft" &&
                        host !== "login.microsoftonline.com"
                    ) {
                        return fallback(req, res, host);
                    }
                    const path = req.url ?? "/";
                    requests.push({ host, path });
                    const reply = (status: number, value: unknown) => {
                        res.writeHead(status, { "content-type": "application/json" });
                        res.end(JSON.stringify(value));
                        return true;
                    };
                    if (path.includes(".well-known/oauth-protected-resource")) {
                        return reply(200, {
                            resource: "https://workiq.svc.cloud.microsoft/mcp",
                            authorization_servers: [
                                "https://login.microsoftonline.com/organizations/v2.0",
                            ],
                            scopes_supported: [scope],
                        });
                    }
                    if (host === "login.microsoftonline.com" && path.includes(".well-known")) {
                        return reply(200, {
                            issuer: "https://login.microsoftonline.com/organizations/v2.0",
                            authorization_endpoint:
                                "https://login.microsoftonline.com/organizations/oauth2/v2.0/authorize",
                            token_endpoint:
                                "https://login.microsoftonline.com/organizations/oauth2/v2.0/token",
                            response_types_supported: ["code"],
                            code_challenge_methods_supported: ["S256"],
                        });
                    }
                    let body = "";
                    for await (const chunk of req) body += chunk.toString();
                    if (host === "login.microsoftonline.com" && path.endsWith("/token")) {
                        const grant = new URLSearchParams(body);
                        grants.push(grant);
                        if (
                            authentication === "browser" &&
                            grant.get("refresh_token") === "parent-refresh"
                        ) {
                            return reply(400, {
                                error: "invalid_grant",
                                error_description: "Interaction required",
                            });
                        }
                        // Entra emits oid only when the browser request includes profile.
                        const claims = {
                            sub: "workiq-client-subject",
                            tid: tenantId,
                            ...(browserScopes.includes("profile") ? { oid: objectId } : {}),
                        };
                        return reply(200, {
                            access_token: "workiq-service-token",
                            token_type: "Bearer",
                            expires_in: 3600,
                            scope,
                            ...(grant.get("grant_type") === "authorization_code"
                                ? {
                                      id_token: `e30.${Buffer.from(JSON.stringify(claims)).toString("base64url")}.fixture`,
                                      ...(browserScopes.includes("offline_access")
                                          ? { refresh_token: "workiq-browser-refresh" }
                                          : {}),
                                  }
                                : {}),
                        });
                    }
                    if (host === "workiq.svc.cloud.microsoft") {
                        if (req.headers.authorization !== "Bearer workiq-service-token") {
                            res.setHeader(
                                "www-authenticate",
                                `Bearer resource_metadata="https://workiq.svc.cloud.microsoft/.well-known/oauth-protected-resource", scope="${scope}"`
                            );
                            return reply(401, { error: "invalid_token" });
                        }
                        if (req.method !== "POST") return reply(405, {});
                        const message = JSON.parse(body);
                        if (message.method === "initialize") initializeRequests++;
                        if (message.id === undefined) {
                            res.writeHead(202);
                            res.end();
                            return true;
                        }
                        const result =
                            message.method === "initialize"
                                ? {
                                      protocolVersion: "2024-11-05",
                                      capabilities: { tools: {} },
                                      serverInfo: { name: "workiq-fixture", version: "1" },
                                  }
                                : message.method === "tools/list"
                                  ? {
                                        tools: [
                                            {
                                                name: "whoami",
                                                description: "Return the linked fixture identity",
                                                inputSchema: { type: "object", properties: {} },
                                            },
                                        ],
                                    }
                                  : message.method === "tools/call"
                                    ? { content: [{ type: "text", text: parent }] }
                                    : {};
                        return reply(200, { jsonrpc: "2.0", id: message.id, result });
                    }
                    return reply(404, {});
                },
                {
                    interceptDomains: [
                        "workiq.svc.cloud.microsoft",
                        "login.microsoftonline.com",
                        "api.githubcopilot.com",
                        "api.github.com",
                        "github.com",
                        "api.mcp.github.com",
                    ],
                }
            );
            let client: CopilotClient | undefined;
            const starting = proxy.start();
            onTestFinished(async () => {
                try {
                    await client?.stop();
                } finally {
                    await starting;
                    await proxy.stop();
                }
            });
            await starting;
            const clientOptions = {
                connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
                workingDirectory: workDir,
                env: {
                    ...env,
                    COPILOT_HOME: profile,
                    COPILOT_DISABLE_KEYTAR: "1",
                    HTTPS_PROXY: proxy.proxyUrl,
                    HTTP_PROXY: proxy.proxyUrl,
                    https_proxy: proxy.proxyUrl,
                    http_proxy: proxy.proxyUrl,
                    NODE_EXTRA_CA_CERTS: proxy.caFilePath,
                    SSL_CERT_FILE: proxy.caFilePath,
                },
            };
            client = new CopilotClient(clientOptions);
            let session = await client.createSession({
                configDirectory: profile,
                mcpOAuthTokenStorage: "persistent",
                onPermissionRequest: approveAll,
            });
            expect(
                (await session.rpc.mcp.list()).servers.find((server) => server.name === "WorkIQ")
                    ?.status
            ).toBe("disabled");
            expect(requests).toHaveLength(0);
            await session.rpc.mcp.enable({ serverName: "WorkIQ" });
            if (authentication === "browser") {
                await waitForCondition(
                    async () =>
                        (await session.rpc.mcp.list()).servers.some(
                            (server) => server.name === "WorkIQ" && server.status === "needs-auth"
                        ),
                    { timeoutMessage: "WorkIQ did not request browser authentication" }
                );
                const redirectUri = "https://example.test/workiq-callback";
                const login = await session.rpc.mcp.oauth.login({
                    serverName: "WorkIQ",
                    forceReauth: true,
                    redirectUri,
                });
                expect(login.authorizationUrl).toBeDefined();
                expect(login.authorizationId).toBeDefined();
                const authorizationUrl = new URL(login.authorizationUrl!);
                browserScopes = authorizationUrl.searchParams.get("scope")?.split(" ") ?? [];
                const callbackUrl = new URL(redirectUri);
                callbackUrl.searchParams.set("code", "accepted-code");
                callbackUrl.searchParams.set("state", login.authorizationId!);
                await session.rpc.mcp.oauth.complete({
                    authorizationId: login.authorizationId!,
                    callbackUrl: callbackUrl.toString(),
                });
                expect(browserScopes).toEqual(
                    expect.arrayContaining([scope, "openid", "profile", "offline_access"])
                );
            }
            await waitForCondition(
                async () =>
                    (await session.rpc.mcp.list()).servers.some(
                        (server) => server.name === "WorkIQ" && server.status === "connected"
                    ),
                { timeoutMessage: "WorkIQ did not connect after enablement" }
            );
            const initialGrantCount = authentication === "browser" ? 2 : 1;
            expect(grants).toHaveLength(initialGrantCount);
            expect(grants[0].get("client_id")).toBe("ba081686-5d24-4bc6-a0d6-d034ecffed87");
            expect(grants[0].get("refresh_token")).toBe("parent-refresh");
            expect(grants[0].get("scope")).toContain(scope);
            expect(
                (await session.rpc.mcp.listTools({ serverName: "WorkIQ" })).tools.map(
                    (tool) => tool.name
                )
            ).toContain("whoami");

            const connectedInitializeRequests = initializeRequests;
            expect(connectedInitializeRequests).toBeGreaterThan(0);
            for (let attempt = 0; attempt < 2; attempt++) {
                await session.rpc.mcp.enable({ serverName: "WorkIQ" });
                expect(initializeRequests).toBe(connectedInitializeRequests);
                expect(
                    (await session.rpc.mcp.list()).servers.find(
                        (server) => server.name === "WorkIQ"
                    )?.status
                ).toBe("connected");
            }
            const preferences = JSON.parse(await readFile(join(profile, "config.json"), "utf8"));
            expect(preferences.accountMcpEnablement[JSON.stringify([parent, "WorkIQ"])]).toBe(true);
            expect(grants).toHaveLength(initialGrantCount);

            await session.rpc.mcp.oauth.authenticationStateChanged({
                serverName: "WorkIQ",
                refreshSessionToken: true,
            });
            expect(grants).toHaveLength(initialGrantCount + 1);
            expect(grants.at(-1)?.get("refresh_token")).toBe(
                authentication === "browser" ? "workiq-browser-refresh" : "parent-refresh"
            );
            const tokenFiles = (await readdir(join(profile, "mcp-oauth-config"))).filter((name) =>
                name.endsWith(".tokens.json")
            );
            const saved = await Promise.all(
                tokenFiles.map(async (name) =>
                    JSON.parse(await readFile(join(profile, "mcp-oauth-config", name), "utf8"))
                )
            );
            expect(saved.length).toBeGreaterThan(0);
            expect(saved.every((record) => record.accountId === parent)).toBe(true);
            expect(saved.every((record) => record.parentAccountId === undefined)).toBe(true);
            expect(saved.every((record) => record.parentAccount === undefined)).toBe(true);
            expect(saved.every((record) => record.brokerAccountId === undefined)).toBe(true);
            await client.stop();
            client = new CopilotClient(clientOptions);
            session = await client.createSession({
                configDirectory: profile,
                mcpOAuthTokenStorage: "persistent",
                onPermissionRequest: approveAll,
            });
            await session.rpc.mcp.reload();
            await waitForCondition(
                async () =>
                    (await session.rpc.mcp.list()).servers.some(
                        (server) => server.name === "WorkIQ" && server.status === "connected"
                    ),
                { timeoutMessage: "Persisted WorkIQ authentication did not reconnect" }
            );
            expect(grants).toHaveLength(initialGrantCount + 1);
            await session.rpc.accounts.set({ command: { kind: "logout", selectionId: parent } });
            expect(
                (await session.rpc.mcp.list()).servers.some((server) => server.name === "WorkIQ")
            ).toBe(false);
            await expect(session.rpc.mcp.listTools({ serverName: "WorkIQ" })).rejects.toThrow();
        }
    );
});
