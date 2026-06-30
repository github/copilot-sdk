#!/usr/bin/env node
/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Minimal OAuth-protected Streamable HTTP MCP server for SDK E2E tests.
 *
 * The `/mcp` endpoint returns a WWW-Authenticate challenge until requests include
 * an accepted test token, then serves enough JSON-RPC MCP methods for the runtime
 * to initialize and list/call one tool. Specific tool-call scenarios trigger
 * replacement-token challenges so SDK E2E tests can cover refresh, upscope, and
 * reauth flows without relying on a real OAuth server.
 */

import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";

const DEFAULT_EXPECTED_TOKEN = "sdk-host-token";
const PROTOCOL_VERSION = "2025-03-26";
const PROTECTED_RESOURCE_PATH = "/.well-known/oauth-protected-resource";

export async function startOAuthMcpServer({
  expectedToken = DEFAULT_EXPECTED_TOKEN,
  host = "127.0.0.1",
  port = 0,
} = {}) {
  const requests = [];
  const tokens = {
    initial: expectedToken,
    refresh: `${expectedToken}-refresh`,
    upscope: `${expectedToken}-upscope`,
    reauth: `${expectedToken}-reauth`,
    rejected: `${expectedToken}-rejected`,
  };
  const acceptedTokens = new Set([
    tokens.initial,
    tokens.refresh,
    tokens.upscope,
    tokens.reauth,
  ]);

  const server = http.createServer(async (req, res) => {
    const url = new URL(
      req.url ?? "/",
      `http://${req.headers.host ?? `${host}:${port}`}`,
    );
    const baseUrl = url.origin;

    if (req.method === "GET" && url.pathname === "/__requests") {
      respondJson(res, 200, requests);
      return;
    }

    if (
      req.method === "GET" &&
      url.pathname === PROTECTED_RESOURCE_PATH
    ) {
      respondJson(res, 200, {
        resource: `${baseUrl}/mcp`,
        authorization_servers: [baseUrl],
        scopes_supported: ["mcp.read"],
        bearer_methods_supported: ["header"],
      });
      return;
    }

    if (
      req.method === "GET" &&
      url.pathname === "/.well-known/oauth-authorization-server"
    ) {
      respondJson(res, 200, {
        issuer: baseUrl,
        authorization_endpoint: `${baseUrl}/authorize`,
        token_endpoint: `${baseUrl}/token`,
        response_types_supported: ["code"],
        grant_types_supported: ["authorization_code"],
      });
      return;
    }

    if (url.pathname !== "/mcp") {
      respondJson(res, 404, { error: "not_found" });
      return;
    }

    const body = await readBody(req);
    requests.push({
      method: req.method,
      path: url.pathname,
      authorization: req.headers.authorization ?? null,
      body: body || null,
    });

    const token = parseBearerToken(req.headers.authorization);
    if (!token || !acceptedTokens.has(token)) {
      challengeInitial(res, baseUrl);
      return;
    }

    if (req.method !== "POST") {
      respondJson(res, 405, { error: "method_not_allowed" });
      return;
    }

    const parsedBody = parseJsonBody(body);
    if (!parsedBody.ok) {
      respondJson(res, 400, { error: "invalid_json" });
      return;
    }

    const message = parsedBody.value;
    const replacementChallenge = getReplacementChallenge(
      message,
      token,
      tokens,
      baseUrl,
    );
    if (replacementChallenge) {
      res.writeHead(replacementChallenge.statusCode, {
        "www-authenticate": replacementChallenge.wwwAuthenticate,
        "content-type": "application/json",
      });
      res.end(JSON.stringify({ error: replacementChallenge.error }));
      return;
    }

    const response = Array.isArray(message)
      ? message
          .map((item) => handleJsonRpcMessage(item))
          .filter((item) => item !== undefined)
      : handleJsonRpcMessage(message);

    if (
      response === undefined ||
      (Array.isArray(response) && response.length === 0)
    ) {
      res.writeHead(202, { "mcp-session-id": "oauth-test-session" });
      res.end();
      return;
    }

    res.writeHead(200, {
      "content-type": "application/json",
      "mcp-session-id": "oauth-test-session",
    });
    res.end(JSON.stringify(response));
  });

  await new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(port, host, () => {
      server.off("error", reject);
      resolve();
    });
  });

  const address = server.address();
  if (!address || typeof address === "string") {
    throw new Error("Expected TCP server address");
  }

  return {
    url: `http://${host}:${address.port}`,
    requests,
    close: () =>
      new Promise((resolve, reject) =>
        server.close((err) => (err ? reject(err) : resolve())),
      ),
  };
}

function getReplacementChallenge(message, token, tokens, baseUrl) {
  const messages = Array.isArray(message) ? message : [message];
  const toolCall = messages.find((item) => item?.method === "tools/call");
  const scenario = toolCall?.params?.arguments?.scenario;

  if (scenario === "refresh" && token !== tokens.refresh) {
    return {
      statusCode: 401,
      wwwAuthenticate: 'Bearer error="invalid_token"',
      error: "token_expired",
    };
  }

  if (scenario === "upscope" && token !== tokens.upscope) {
    return {
      statusCode: 403,
      wwwAuthenticate: `Bearer resource_metadata="${baseUrl}${PROTECTED_RESOURCE_PATH}", scope="mcp.write", error="insufficient_scope"`,
      error: "insufficient_scope",
    };
  }

  if (scenario === "reauth" && token !== tokens.reauth) {
    return {
      statusCode: 401,
      wwwAuthenticate: 'Bearer error="invalid_token"',
      error: "reauth_required",
    };
  }

  if (scenario === "cancel" && token !== tokens.refresh) {
    return {
      statusCode: 401,
      wwwAuthenticate: 'Bearer error="invalid_token"',
      error: "token_expired",
    };
  }

  return undefined;
}

function handleJsonRpcMessage(message) {
  if (!message || typeof message !== "object" || !("id" in message)) {
    return undefined;
  }

  switch (message.method) {
    case "initialize":
      return {
        jsonrpc: "2.0",
        id: message.id,
        result: {
          protocolVersion: message.params?.protocolVersion ?? PROTOCOL_VERSION,
          capabilities: { tools: {} },
          serverInfo: { name: "oauth-test-server", version: "1.0.0" },
        },
      };
    case "tools/list":
      return {
        jsonrpc: "2.0",
        id: message.id,
        result: {
          tools: [
            {
              name: "whoami",
              description: "Returns the authenticated test principal.",
              inputSchema: {
                type: "object",
                properties: {
                  scenario: {
                    type: "string",
                    enum: ["initial", "refresh", "upscope", "reauth", "cancel"],
                  },
                },
                additionalProperties: false,
              },
              _meta: { "ui.visibility": ["model", "app"] },
            },
          ],
        },
      };
    case "tools/call":
      return {
        jsonrpc: "2.0",
        id: message.id,
        result: {
          content: [{ type: "text", text: "oauth-test-user" }],
          isError: false,
        },
      };
    default:
      return {
        jsonrpc: "2.0",
        id: message.id,
        error: { code: -32601, message: `Method not found: ${message.method}` },
      };
  }
}

function parseBearerToken(authorization) {
  const match = /^Bearer (.+)$/.exec(authorization ?? "");
  return match?.[1];
}

function challengeInitial(res, baseUrl) {
  const resourceMetadataUrl = `${baseUrl}${PROTECTED_RESOURCE_PATH}`;
  res.writeHead(401, {
    "www-authenticate": `Bearer resource_metadata="${resourceMetadataUrl}", scope="mcp.read", error="invalid_token"`,
    "content-type": "application/json",
  });
  res.end(JSON.stringify({ error: "missing_or_invalid_token" }));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on("data", (chunk) => chunks.push(chunk));
    req.on("error", reject);
    req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
  });
}

function parseJsonBody(body) {
  if (!body) {
    return { ok: true, value: undefined };
  }

  try {
    return { ok: true, value: JSON.parse(body) };
  } catch {
    return { ok: false, value: undefined };
  }
}

function respondJson(res, statusCode, body) {
  const data = JSON.stringify(body);
  res.writeHead(statusCode, {
    "content-type": "application/json",
    "content-length": Buffer.byteLength(data),
  });
  res.end(data);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const server = await startOAuthMcpServer({
    expectedToken: process.env.EXPECTED_TOKEN ?? DEFAULT_EXPECTED_TOKEN,
  });
  console.log(`Listening: ${server.url}`);
  process.on("SIGTERM", async () => {
    await server.close();
    process.exit(0);
  });
}
