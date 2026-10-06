/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { mkdirSync, rmSync } from "node:fs";
import { createServer, type Server } from "node:http";
import type { AddressInfo } from "node:net";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const CATALOG_PATH = "/copilot-connectors/api/v1/plugins";

interface CatalogRequest {
    token: string;
    ifNoneMatch?: string;
    statusCode: number;
}

/**
 * Serves the GitHub API routes client-level Connector discovery needs: the
 * Copilot-user lookup that resolves the SDK credential's login, and a Connector
 * catalog that honors `If-None-Match` so conditional revalidation is observable.
 */
async function startGitHubApi(login: string, capiUrl: string, etag: string) {
    const catalogRequests: CatalogRequest[] = [];
    const server: Server = createServer((req, res) => {
        const url = req.url ?? "/";
        if (req.method === "GET" && url.startsWith("/copilot_internal/user")) {
            res.writeHead(200, { "content-type": "application/json" });
            res.end(
                JSON.stringify({
                    login,
                    copilot_plan: "individual_pro",
                    endpoints: { api: capiUrl, telemetry: "https://localhost:1/telemetry" },
                    analytics_tracking_id: "connector-discovery-tracking-id",
                })
            );
            return;
        }
        if (req.method === "GET" && url.startsWith(CATALOG_PATH)) {
            const token = String(req.headers.authorization ?? "").replace(
                /^(?:Bearer|token)\s+/i,
                ""
            );
            const ifNoneMatch =
                typeof req.headers["if-none-match"] === "string"
                    ? req.headers["if-none-match"]
                    : undefined;
            const statusCode = ifNoneMatch === etag ? 304 : 200;
            catalogRequests.push({
                token,
                ...(ifNoneMatch === undefined ? {} : { ifNoneMatch }),
                statusCode,
            });
            if (statusCode === 304) {
                res.writeHead(304, { etag });
                res.end();
                return;
            }
            res.writeHead(200, { "content-type": "application/json", etag });
            res.end(
                JSON.stringify({
                    plugins: [
                        {
                            name: "mail",
                            connection: { status: "not_connected" },
                            metadata: { displayName: "Mail Connector" },
                            mcpServers: {
                                mcpServers: {
                                    mail: { type: "http", url: "https://connector.invalid/mcp" },
                                },
                            },
                        },
                    ],
                })
            );
            return;
        }
        res.writeHead(404, { "content-type": "application/json" });
        res.end(JSON.stringify({ message: "Not Found" }));
    });
    await new Promise<void>((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", () => resolve());
    });
    const { port } = server.address() as AddressInfo;
    return {
        url: `http://127.0.0.1:${port}`,
        catalogRequests,
        close: () =>
            new Promise<void>((resolve) => {
                server.closeAllConnections();
                server.close(() => resolve());
            }),
    };
}

describe("Client-level Connector discovery", async () => {
    const { env, workDir } = await createSdkTestContext();

    it(
        "should discover and revalidate the Connector catalog without a session",
        { timeout: 120_000 },
        async () => {
            const login = `connector-discovery-${randomUUID().replaceAll("-", "")}`;
            const token = `connector-discovery-token-${randomUUID().replaceAll("-", "")}`;
            const etag = 'W/"connector-discovery-v1"';
            const gitHubApi = await startGitHubApi(login, env.COPILOT_API_URL, etag);
            const home = join(workDir, `copilot-e2e-connectors-home-${randomUUID()}`);
            mkdirSync(home, { recursive: true });
            const client = new CopilotClient({
                workingDirectory: workDir,
                env: {
                    ...env,
                    COPILOT_HOME: home,
                    GH_CONFIG_DIR: home,
                    XDG_CONFIG_HOME: home,
                    XDG_STATE_HOME: home,
                    GH_TOKEN: "",
                    GITHUB_TOKEN: "",
                    COPILOT_DEBUG_GITHUB_API_URL: gitHubApi.url,
                    COPILOT_EXP_COPILOT_CLI_MANAGED_MCP_SERVERS: "true",
                },
                logLevel: "error",
                connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
                gitHubToken: token,
            });
            try {
                await client.start();
                expect(await client.listSessions()).toEqual([]);

                const capabilities = await client.rpc.connectors.getCapabilities();
                expect(capabilities).toEqual({
                    apiVersion: 1,
                    availability: "enabled",
                    conditionalCache: true,
                    opaqueAccountSelection: true,
                });

                const accounts = await client.rpc.connectors.getAccounts();
                expect(accounts.availability).toBe("enabled");
                const account = accounts.accounts.find(
                    (candidate) => candidate.authInfo.login === login
                );
                expect(account).toBeDefined();
                expect(account!.authInfo.host).toBe("https://github.com");
                expect(Object.keys(account!).sort()).toEqual(["accountId", "authInfo"]);
                expect(Object.keys(account!.authInfo).sort()).toEqual(["host", "login", "type"]);
                expect(JSON.stringify(accounts)).not.toContain(token);
                const accountId = account!.accountId;

                await expect(
                    client.rpc.connectors.list({ accountId: "unknown-discovery-account" })
                ).rejects.toThrow(/account selection/i);

                const catalog = await client.rpc.connectors.refresh({ accountId });
                expect(catalog.accountId).toBe(accountId);
                expect(catalog.revision).toBeGreaterThan(0);
                expect(catalog.connectors).toEqual([
                    { name: "mail", displayName: "Mail Connector", status: "not_connected" },
                ]);
                expect(JSON.stringify(catalog)).not.toContain("connector.invalid");

                const revalidated = await client.rpc.connectors.refresh({ accountId });
                expect(revalidated).toEqual(catalog);
                expect(gitHubApi.catalogRequests).toEqual([
                    { token, statusCode: 200 },
                    { token, ifNoneMatch: etag, statusCode: 304 },
                ]);

                const cached = await client.rpc.connectors.list({ accountId });
                expect(cached).toEqual(catalog);
                expect(gitHubApi.catalogRequests).toHaveLength(2);
                expect(await client.listSessions()).toEqual([]);
            } finally {
                try {
                    await client.stop();
                } catch {
                    // Best-effort cleanup.
                }
                await gitHubApi.close();
                rmSync(home, { recursive: true, force: true });
            }
        }
    );
});
