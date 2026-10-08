/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { onTestFinished, test } from "vitest";
import { startSessionExpiryMcpServer } from "./test-mcp-session-expiry-server.mjs";

test.for(["2025-03-26", "2026-07-28", "2099-01-01"])(
  "negotiates a stateful session when the client proposes %s",
  async (protocolVersion, { expect }) => {
    const startingServer = startSessionExpiryMcpServer();
    onTestFinished(async () => {
      const [result] = await Promise.allSettled([startingServer]);
      if (result.status === "fulfilled") {
        await result.value.close();
      }
    });
    const server = await startingServer;
    const initialized = await fetch(`${server.url}/mcp`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        jsonrpc: "2.0",
        id: 1,
        method: "initialize",
        params: {
          protocolVersion,
          capabilities: {},
          clientInfo: { name: "test", version: "1" },
        },
      }),
    });
    expect(initialized.status).toBe(200);
    expect((await initialized.json()).result.protocolVersion).toBe("2025-03-26");
    const sessionId = initialized.headers.get("mcp-session-id");
    expect(sessionId).not.toBeNull();
    if (sessionId === null) {
      throw new Error("The stateful fixture did not issue an MCP session ID");
    }

    const ping = () =>
      fetch(`${server.url}/mcp`, {
        method: "POST",
        headers: { "content-type": "application/json", "mcp-session-id": sessionId },
        body: JSON.stringify({ jsonrpc: "2.0", id: 2, method: "ping" }),
      });
    const alive = await ping();
    expect(alive.status).toBe(200);
    expect(await alive.json()).toEqual({ jsonrpc: "2.0", id: 2, result: {} });

    const expired = await fetch(`${server.url}/__expire`, { method: "POST" });
    expect(expired.status).toBe(200);
    expect(await expired.json()).toEqual({ expired: 1 });
    const lost = await ping();
    expect(lost.status).toBe(404);
    expect(await lost.json()).toEqual({ error: "session_expired" });
    const lostStream = await fetch(`${server.url}/mcp`, {
      headers: { "mcp-session-id": sessionId },
    });
    expect(lostStream.status).toBe(404);
    expect(await lostStream.json()).toEqual({ error: "session_expired" });
  },
);
