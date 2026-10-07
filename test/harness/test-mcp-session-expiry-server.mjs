#!/usr/bin/env node
/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Minimal Streamable HTTP MCP server for SDK E2E tests that need to observe
 * remote session closure and reconnection.
 *
 * The `/mcp` endpoint issues a real `mcp-session-id` per connection and serves
 * enough JSON-RPC MCP methods for the runtime to initialize, list tools and call
 * one tool. `POST /__expire` invalidates every active session so subsequent
 * requests (including the runtime's liveness `ping`) answer `404`, which is how a
 * server-side idle expiry is observed by a real Streamable HTTP client.
 * `GET /__stats` reports the counters tests assert on.
 */

import http from "node:http";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { fileURLToPath } from "node:url";

const PROTOCOL_VERSION = "2025-03-26";

export async function startSessionExpiryMcpServer({ host = "127.0.0.1", port = 0 } = {}) {
  const activeSessions = new Set();
  const expiredSessions = new Set();
  const stats = { initializations: 0, toolsListRequests: 0, toolCalls: 0 };

  const server = http.createServer(async (req, res) => {
    const url = new URL(req.url ?? "/", `http://${req.headers.host ?? `${host}:${port}`}`);

    if (req.method === "GET" && url.pathname === "/__stats") {
      respondJson(res, 200, { ...stats, activeSessions: activeSessions.size });
      return;
    }

    if (req.method === "POST" && url.pathname === "/__expire") {
      for (const sessionId of activeSessions) {
        expiredSessions.add(sessionId);
      }
      activeSessions.clear();
      respondJson(res, 200, { expired: expiredSessions.size });
      return;
    }

    if (url.pathname !== "/mcp") {
      respondJson(res, 404, { error: "not_found" });
      return;
    }

    const sessionId = req.headers["mcp-session-id"];
    if (typeof sessionId === "string" && expiredSessions.has(sessionId)) {
      respondJson(res, 404, { error: "session_expired" });
      return;
    }

    if (req.method !== "POST") {
      respondJson(res, 405, { error: "method_not_allowed" });
      return;
    }

    const parsedBody = parseJsonBody(await readBody(req));
    if (!parsedBody.ok) {
      respondJson(res, 400, { error: "invalid_json" });
      return;
    }

    const messages = Array.isArray(parsedBody.value) ? parsedBody.value : [parsedBody.value];
    const isInitialize = messages.some((message) => message?.method === "initialize");
    let responseSessionId = typeof sessionId === "string" ? sessionId : undefined;

    if (isInitialize) {
      responseSessionId = randomUUID();
      activeSessions.add(responseSessionId);
      stats.initializations++;
    } else if (responseSessionId === undefined || !activeSessions.has(responseSessionId)) {
      respondJson(res, 404, { error: "session_not_found" });
      return;
    }

    const responses = messages
      .map((message) => handleJsonRpcMessage(message, stats))
      .filter((message) => message !== undefined);

    if (responses.length === 0) {
      res.writeHead(202, { "mcp-session-id": responseSessionId });
      res.end();
      return;
    }

    const payload = JSON.stringify(Array.isArray(parsedBody.value) ? responses : responses[0]);
    res.writeHead(200, {
      "content-type": "application/json",
      "content-length": Buffer.byteLength(payload),
      "mcp-session-id": responseSessionId,
    });
    res.end(payload);
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
    stats,
    close: () =>
      new Promise((resolve, reject) => {
        server.close((err) => (err ? reject(err) : resolve()));
        server.closeAllConnections();
      }),
  };
}

function handleJsonRpcMessage(message, stats) {
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
          serverInfo: { name: "session-expiry-test-server", version: "1.0.0" },
        },
      };
    case "ping":
      return { jsonrpc: "2.0", id: message.id, result: {} };
    case "tools/list":
      stats.toolsListRequests++;
      return {
        jsonrpc: "2.0",
        id: message.id,
        result: {
          tools: [
            {
              name: "remote_ping",
              description: "Returns pong to prove the remote MCP connection is functional.",
              inputSchema: { type: "object", properties: {}, additionalProperties: false },
              _meta: { "ui.visibility": ["model", "app"] },
            },
          ],
        },
      };
    case "tools/call":
      stats.toolCalls++;
      return {
        jsonrpc: "2.0",
        id: message.id,
        result: { content: [{ type: "text", text: "remote pong" }], isError: false },
      };
    default:
      return {
        jsonrpc: "2.0",
        id: message.id,
        error: { code: -32601, message: `Method not found: ${message.method}` },
      };
  }
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
  const server = await startSessionExpiryMcpServer();
  console.log(`Listening: ${server.url}`);
  process.on("SIGTERM", async () => {
    await server.close();
    process.exit(0);
  });
}
