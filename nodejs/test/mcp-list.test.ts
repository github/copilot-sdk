/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { PassThrough } from "node:stream";
import { createMessageConnection } from "vscode-jsonrpc/node.js";
import { describe, expect, it, vi } from "vitest";
import { createSessionRpc } from "../src/generated/rpc.js";

describe("session MCP wire contracts", () => {
    it("uses parameterless list endpoints with the wrapper session", async () => {
        const input = new PassThrough();
        const output = new PassThrough();
        const connection = createMessageConnection(input, output);
        const sendRequest = vi.spyOn(connection, "sendRequest").mockResolvedValue({ servers: [] });
        try {
            const mcp = createSessionRpc(connection, "wrapper-session").mcp;
            await mcp.list();
            await mcp.listConfigured();
            expect(sendRequest.mock.calls).toEqual([
                ["session.mcp.list", { sessionId: "wrapper-session" }],
                ["session.mcp.listConfigured", { sessionId: "wrapper-session" }],
            ]);
        } finally {
            connection.dispose();
            input.destroy();
            output.destroy();
        }
    });

    it("keeps the wrapper session authoritative over a supplied sessionId", async () => {
        const input = new PassThrough();
        const output = new PassThrough();
        const connection = createMessageConnection(input, output);
        const sendRequest = vi.spyOn(connection, "sendRequest").mockResolvedValue(undefined);
        try {
            const mcp = createSessionRpc(connection, "wrapper-session").mcp;
            const params = { serverName: "test-server", sessionId: "forged-session" };
            await mcp.enable(params);
            expect(sendRequest.mock.calls).toEqual([
                ["session.mcp.enable", { serverName: "test-server", sessionId: "wrapper-session" }],
            ]);
        } finally {
            connection.dispose();
            input.destroy();
            output.destroy();
        }
    });
});
