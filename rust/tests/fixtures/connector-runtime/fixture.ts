/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { appendFile, mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";

import { CASE_TIMEOUT_MS, cases, transports } from "./budget.ts";

const runtimeRoot = process.env.CONNECTOR_FIXTURE_RUNTIME_ROOT;
const binary = process.env.CONNECTOR_LOCAL_BINARY;
assert(runtimeRoot && binary, "Supply the runtime fixture root and compiled SDK test binary");
const { TestMcpHttpServer } = await import(
    pathToFileURL(join(runtimeRoot, "test/cli/e2e/harness/testMcpHttpServer.ts")).href
);
const { getSharedCA } = await import(pathToFileURL(join(runtimeRoot, "test/cli/e2e/harness/certUtils.ts")).href);

const tokens = {
    first: "ghu_connector_session_first",
    rotated: "ghu_connector_session_rotated",
    other: "ghu_connector_session_other_user",
    repository: "ghu_connector_repository_identity",
};

async function runCase(transport: string, testCase: string) {
    const caPem = getSharedCA().certPem;
    const directory = await mkdtemp(join(tmpdir(), "sdk-connector-local-"));
    const home = join(directory, "home");
    await mkdir(home);
    const ca = join(directory, "ca.pem");
    await writeFile(ca, caPem);
    const mail = new TestMcpHttpServer(true, tokens.first);
    const calendar = new TestMcpHttpServer(true, tokens.first);
    const ordinary = new TestMcpHttpServer(true);
    let mailUrl = "";
    let calendarUrl = "";
    let ordinaryUrl = "";
    let reads = 0;
    let writes = 0;
    const generations: string[] = [];
    const userGenerations: string[] = [];
    let expandedScope = false;
    let apiUrl: string;
    const mcpStatus = (server: InstanceType<typeof TestMcpHttpServer>) => ({
        initializations: server.connectionInitializationCount,
        lists: server.toolsListRequestCount,
        unauthorized: server.unauthorizedRequestCount,
        active: server.activeConnectionCount,
    });
    const status = () => ({
        reads,
        writes,
        generations,
        userGenerations,
        mail: mcpStatus(mail),
        calendar: mcpStatus(calendar),
        ordinary: mcpStatus(ordinary),
    });
    const plugin = (name: string, url: string) => ({
        name,
        logo: "https://images.example/connector.svg",
        metadata: { displayName: name, tier: "standard", releaseTag: "preview" },
        connection: { status: "connected" },
        mcpServers: { mcpServers: { [name]: { type: "http", url } } },
    });
    const api = createServer(async (request, response) => {
        try {
            response.setHeader("content-type", "application/json");
            if (request.url === "/local-test-control") {
                if (request.method === "POST") {
                    let body = "";
                    for await (const chunk of request) body += chunk;
                    const update = JSON.parse(body);
                    if (update.expandedScope !== undefined) expandedScope = update.expandedScope;
                    if (update.credential !== undefined) {
                        const credential = tokens[update.credential as keyof typeof tokens];
                        assert(credential);
                        mail.setRequiredBearerToken(credential);
                        calendar.setRequiredBearerToken(credential);
                    }
                }
                response.end(JSON.stringify(status()));
                return;
            }
            const header = request.headers.authorization ?? "";
            const token = header.slice(header.indexOf(" ") + 1);
            const generation = Object.entries(tokens).find(([, value]) => value === token)?.[0] ?? "unknown";
            if (request.url?.startsWith("/copilot_internal/user")) {
                userGenerations.push(generation);
                if (generation === "unknown") {
                    response.writeHead(401).end(JSON.stringify({ message: "Bad credentials" }));
                } else {
                    response.end(
                        JSON.stringify({
                            login:
                                generation === "repository"
                                    ? "repository-user"
                                    : generation === "other"
                                      ? "hubot"
                                      : "octocat",
                            copilot_plan: "individual_pro",
                            is_mcp_enabled: true,
                            endpoints: { api: apiUrl },
                        }),
                    );
                }
                return;
            }
            if (request.url === "/copilot-connectors/api/v1/plugins") {
                reads++;
                generations.push(generation);
                if (expandedScope && generation === "first") {
                    response
                        .writeHead(403, {
                            "x-github-request-id": "LOCAL-CONNECTOR-SCOPE-FIXTURE",
                            "x-accepted-oauth-scopes": "write:plugin_gateway_connections",
                            "x-oauth-scopes": "read:user",
                        })
                        .end(JSON.stringify({ message: "insufficient OAuth scope" }));
                } else {
                    response.end(
                        JSON.stringify({ plugins: [plugin("mail", mailUrl), plugin("calendar", calendarUrl)] }),
                    );
                }
                return;
            }
            if (
                request.url?.startsWith("/copilot-connectors/api/v1/connectors/managed/") &&
                ["PUT", "DELETE"].includes(request.method ?? "")
            ) {
                writes++;
                response.end("{}");
                return;
            }
            response.writeHead(404).end("{}");
        } catch (error) {
            console.error("Local fixture request failed", error);
            if (!response.headersSent) response.writeHead(500);
            response.end(JSON.stringify({ message: "Local fixture failure" }));
        }
    });
    try {
        // Started inside the try so a partial startup failure still reaches the cleanup below,
        // and one at a time so no sibling can begin listening after that cleanup has run.
        mailUrl = await mail.start();
        calendarUrl = await calendar.start();
        ordinaryUrl = await ordinary.start();
        await new Promise<void>((resolve, reject) => {
            api.once("error", reject);
            api.listen(0, "127.0.0.1", resolve);
        });
        const address = api.address();
        assert(address && typeof address !== "string");
        apiUrl = `http://127.0.0.1:${address.port}`;
        await new Promise<void>((resolve, reject) => {
            const child = spawn(binary!, [], {
                cwd: directory,
                stdio: "inherit",
                env: {
                    ...process.env,
                    HOME: home,
                    USERPROFILE: home,
                    COPILOT_HOME: join(directory, "copilot-home"),
                    COPILOT_DEBUG_GITHUB_API_URL: apiUrl,
                    COPILOT_API_URL: apiUrl,
                    COPILOT_GITHUB_TOKEN: tokens.repository,
                    GH_TOKEN: tokens.repository,
                    GITHUB_TOKEN: tokens.repository,
                    COPILOT_SDK_AUTH_TOKEN: "",
                    NODE_EXTRA_CA_CERTS: ca,
                    SSL_CERT_FILE: ca,
                    CURL_CA_BUNDLE: ca,
                    NO_PROXY: "localhost,127.0.0.1,::1",
                    no_proxy: "localhost,127.0.0.1,::1",
                    CONNECTOR_LOCAL_API: apiUrl,
                    CONNECTOR_LOCAL_DIRECTORY: directory,
                    CONNECTOR_LOCAL_ORDINARY: ordinaryUrl,
                    CONNECTOR_LOCAL_CASE: testCase,
                    CONNECTOR_LOCAL_TRANSPORT: transport,
                },
            });
            // Escalate to SIGKILL and only settle once the child is gone, so cleanup never
            // removes the working directory or servers an orphan is still using.
            let timedOut = false;
            let kill: ReturnType<typeof setTimeout> | undefined;
            const timeout = setTimeout(() => {
                timedOut = true;
                child.kill("SIGTERM");
                kill = setTimeout(() => child.kill("SIGKILL"), 5_000);
            }, CASE_TIMEOUT_MS);
            const settle = () => {
                clearTimeout(timeout);
                if (kill !== undefined) clearTimeout(kill);
            };
            child.once("error", (error) => {
                settle();
                reject(error);
            });
            child.once("exit", (code, signal) => {
                settle();
                if (timedOut) {
                    reject(new Error(`Local integration timeout: ${transport}/${testCase}`));
                    return;
                }
                if (code === 0) resolve();
                else
                    reject(
                        new Error(`Local integration failed: ${transport}/${testCase}, exit ${code}, signal ${signal}`),
                    );
            });
        });
        const report = JSON.stringify({ transport, testCase, ...status() });
        console.log(report);
        if (process.env.CONNECTOR_LOCAL_REPORT) {
            await appendFile(process.env.CONNECTOR_LOCAL_REPORT, `${report}\n`);
        }
    } catch (error) {
        if (process.env.CONNECTOR_LOCAL_REPORT) {
            await appendFile(
                process.env.CONNECTOR_LOCAL_REPORT,
                `${JSON.stringify({ transport, testCase, passed: false, ...status() })}\n`,
            );
        }
        throw error;
    } finally {
        api.closeAllConnections();
        await Promise.all([
            mail.stop(),
            calendar.stop(),
            ordinary.stop(),
            // Closing a server that never listened reports ERR_SERVER_NOT_RUNNING, which would
            // mask the startup failure that skipped `api.listen` in the first place.
            api.listening
                ? new Promise<void>((resolve, reject) => api.close((error) => (error ? reject(error) : resolve())))
                : Promise.resolve(),
        ]);
        await rm(directory, { recursive: true, force: true });
    }
}

for (const transport of transports) {
    for (const testCase of cases) await runCase(transport, testCase);
}
