/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient } from "../../src/index.js";

function startEphemeralClient(): CopilotClient {
    const client = new CopilotClient({ useStdio: true });
    onTestFinished(async () => {
        try {
            await client.forceStop();
        } catch {
            // Ignore cleanup errors
        }
    });
    return client;
}

function uniqueName(prefix: string): string {
    return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

type ServerEntry = Record<string, unknown>;

function getServerConfig(list: { servers: Record<string, unknown> }, name: string): ServerEntry {
    expect(list.servers).toHaveProperty(name);
    const entry = list.servers[name] as ServerEntry;
    expect(entry).toBeDefined();
    return entry;
}

describe("Server-scoped MCP config RPC", () => {
    it("should call server mcp config rpcs", async () => {
        const client = startEphemeralClient();
        await client.start();

        const serverName = uniqueName("sdk-test");
        const config = {
            type: "local" as const,
            command: "node",
            args: [] as string[],
        };
        const updatedConfig = {
            type: "local" as const,
            command: "node",
            args: ["--version"],
        };

        const initial = await client.rpc.mcp.config.list();
        expect(initial.servers[serverName]).toBeUndefined();

        try {
            await client.rpc.mcp.config.add({ name: serverName, config });
            const afterAdd = await client.rpc.mcp.config.list();
            expect(afterAdd.servers[serverName]).toBeDefined();

            await client.rpc.mcp.config.update({ name: serverName, config: updatedConfig });
            const afterUpdate = await client.rpc.mcp.config.list();
            const updated = getServerConfig(afterUpdate, serverName) as {
                command?: string;
                args?: string[];
            };
            expect(updated.command).toBe("node");
            expect(updated.args?.[0]).toBe("--version");

            await client.rpc.mcp.config.disable({ names: [serverName] });
            await client.rpc.mcp.config.enable({ names: [serverName] });
        } finally {
            await client.rpc.mcp.config.remove({ name: serverName });
        }

        const afterRemove = await client.rpc.mcp.config.list();
        expect(afterRemove.servers[serverName]).toBeUndefined();

        await client.stop();
    });

    it("should roundtrip http mcp oauth config rpc", async () => {
        const client = startEphemeralClient();
        await client.start();

        const serverName = uniqueName("sdk-http-oauth");
        const config = {
            type: "http" as const,
            url: "https://example.com/mcp",
            headers: { Authorization: "Bearer token" } as Record<string, string>,
            oauthClientId: "client-id",
            oauthPublicClient: false,
            oauthGrantType: "client_credentials" as const,
            tools: ["*"],
            timeout: 3000,
        };
        const updatedConfig = {
            type: "http" as const,
            url: "https://example.com/updated-mcp",
            oauthClientId: "updated-client-id",
            oauthPublicClient: true,
            oauthGrantType: "authorization_code" as const,
            tools: ["updated-tool"],
            timeout: 4000,
        };

        try {
            await client.rpc.mcp.config.add({ name: serverName, config });
            const afterAdd = await client.rpc.mcp.config.list();
            const added = getServerConfig(afterAdd, serverName) as Record<string, unknown> & {
                headers?: Record<string, string>;
            };
            expect(added.type).toBe("http");
            expect(added.url).toBe("https://example.com/mcp");
            expect(added.headers?.Authorization).toBe("Bearer token");
            expect(added.oauthClientId).toBe("client-id");
            expect(added.oauthPublicClient).toBe(false);
            expect(added.oauthGrantType).toBe("client_credentials");

            await client.rpc.mcp.config.update({ name: serverName, config: updatedConfig });
            const afterUpdate = await client.rpc.mcp.config.list();
            const updated = getServerConfig(afterUpdate, serverName) as Record<string, unknown> & {
                tools?: string[];
            };
            expect(updated.url).toBe("https://example.com/updated-mcp");
            expect(updated.oauthClientId).toBe("updated-client-id");
            expect(updated.oauthPublicClient).toBe(true);
            expect(updated.oauthGrantType).toBe("authorization_code");
            expect(updated.tools?.[0]).toBe("updated-tool");
            expect(updated.timeout).toBe(4000);
        } finally {
            await client.rpc.mcp.config.remove({ name: serverName });
        }

        const afterRemove = await client.rpc.mcp.config.list();
        expect(afterRemove.servers[serverName]).toBeUndefined();

        await client.stop();
    });
});
