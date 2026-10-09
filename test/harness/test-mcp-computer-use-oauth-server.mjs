/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "node:http";

export async function startComputerUseOAuthServer({
    deferRegistration = false,
    deferInitialChallenge = false,
    deferAuthorizationMetadata = false,
    preconfiguredClientId = "",
} = {}) {
    let clientId = preconfiguredClientId;
    let releaseRegistration = () => {};
    const registration = deferRegistration
        ? new Promise((resolve) => {
              releaseRegistration = resolve;
          })
        : Promise.resolve();
    let releaseInitialChallenge = () => {};
    const initialChallenge = deferInitialChallenge
        ? new Promise((resolve) => {
              releaseInitialChallenge = resolve;
          })
        : Promise.resolve();
    let releaseAuthorizationMetadata = () => {};
    const authorizationMetadata = deferAuthorizationMetadata
        ? new Promise((resolve) => {
              releaseAuthorizationMetadata = resolve;
          })
        : Promise.resolve();
    const requests = [];
    const token = "computer-use-isolation-token";
    const server = http.createServer(async (request, response) => {
        const url = new URL(request.url ?? "/", `http://${request.headers.host}`);
        const chunks = [];
        for await (const chunk of request) chunks.push(chunk);
        const body = Buffer.concat(chunks).toString("utf8");
        requests.push({
            method: request.method,
            path: url.pathname,
            authorization: request.headers.authorization ?? null,
            body,
        });
        const respond = (status, value, headers = {}) => {
            response.writeHead(status, { "content-type": "application/json", ...headers });
            response.end(JSON.stringify(value));
        };
        if (request.method === "GET" && url.pathname === "/.well-known/oauth-protected-resource") {
            respond(200, {
                resource: `${url.origin}/mcp`,
                authorization_servers: [url.origin],
                scopes_supported: ["mcp.read"],
                bearer_methods_supported: ["header"],
            });
            return;
        }
        if (request.method === "GET" && url.pathname === "/.well-known/oauth-authorization-server") {
            await authorizationMetadata;
            respond(200, {
                issuer: url.origin,
                authorization_endpoint: `${url.origin}/authorize`,
                token_endpoint: `${url.origin}/token`,
                registration_endpoint: `${url.origin}/register`,
                response_types_supported: ["code"],
                grant_types_supported: ["authorization_code"],
            });
            return;
        }
        if (request.method === "POST" && url.pathname === "/register") {
            await registration;
            clientId = "registered-client";
            respond(201, { client_id: clientId, client_id_issued_at: Math.floor(Date.now() / 1000) });
            return;
        }
        if (request.method === "POST" && url.pathname === "/token") {
            const form = new URLSearchParams(body);
            if (!clientId || form.get("client_id") !== clientId) {
                respond(400, { error: "invalid_client" });
                return;
            }
            if (
                form.get("grant_type") !== "authorization_code" ||
                form.get("code") !== "accepted-code" ||
                !form.get("code_verifier")
            ) {
                respond(400, { error: "invalid_grant" });
                return;
            }
            respond(200, { access_token: token, token_type: "Bearer", expires_in: 3600 });
            return;
        }
        if (url.pathname !== "/mcp") {
            respond(404, { error: "not_found" });
            return;
        }
        if (request.headers.authorization !== `Bearer ${token}`) {
            await initialChallenge;
            respond(
                401,
                { error: "unauthorized" },
                {
                    "www-authenticate": `Bearer resource_metadata="${url.origin}/.well-known/oauth-protected-resource", scope="mcp.read"`,
                },
            );
            return;
        }
        if (request.method !== "POST") {
            respond(405, { error: "method_not_allowed" });
            return;
        }
        let message;
        try {
            message = JSON.parse(body);
        } catch {
            respond(400, { error: "invalid_json" });
            return;
        }
        if (message.id === undefined) {
            response.writeHead(202);
            response.end();
            return;
        }
        let result;
        if (message.method === "initialize") {
            result = {
                protocolVersion: "2025-03-26",
                capabilities: { tools: {} },
                serverInfo: { name: "computer-use-oauth-isolation", version: "1.0.0" },
            };
        } else if (message.method === "tools/list") {
            result = {
                tools: [
                    {
                        name: "whoami",
                        description: "Reports an authenticated connection",
                        inputSchema: { type: "object", properties: {}, additionalProperties: false },
                    },
                ],
            };
        } else if (message.method === "ping") {
            result = {};
        } else {
            respond(200, { jsonrpc: "2.0", id: message.id, error: { code: -32601, message: "Method not found" } });
            return;
        }
        respond(200, { jsonrpc: "2.0", id: message.id, result }, { "mcp-session-id": "computer-use-oauth-isolation" });
    });
    await new Promise((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", () => {
            server.off("error", reject);
            resolve();
        });
    });
    const address = server.address();
    if (!address || typeof address === "string") throw new Error("Expected TCP server address");
    return {
        url: `http://127.0.0.1:${address.port}`,
        requests,
        releaseRegistration,
        releaseInitialChallenge,
        releaseAuthorizationMetadata,
        close: () =>
            new Promise((resolve, reject) => {
                releaseRegistration();
                releaseInitialChallenge();
                releaseAuthorizationMetadata();
                server.close((error) => (error ? reject(error) : resolve()));
                server.closeAllConnections();
            }),
    };
}
