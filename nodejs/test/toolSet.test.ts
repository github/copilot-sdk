/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    approveAll,
    BuiltInTools,
    CopilotClient,
    RuntimeConnection,
    ToolSet,
} from "../src/index.js";

describe("ToolSet builder", () => {
    it("emits source-qualified strings", () => {
        const items = new ToolSet()
            .addBuiltIn("bash")
            .addBuiltIn("*")
            .addCustom("my_tool")
            .addCustom("*")
            .addMcp("github-list_issues")
            .addMcp("*")
            .toArray();
        expect(items).toEqual([
            "builtin:bash",
            "builtin:*",
            "custom:my_tool",
            "custom:*",
            "mcp:github-list_issues",
            "mcp:*",
        ]);
    });

    it("supports array form of addBuiltIn", () => {
        const items = new ToolSet().addBuiltIn(["bash", "view"]).toArray();
        expect(items).toEqual(["builtin:bash", "builtin:view"]);
    });

    it("toArray returns a defensive copy", () => {
        const set = new ToolSet().addBuiltIn("bash");
        const a = set.toArray();
        a.push("builtin:tampered");
        expect(set.toArray()).toEqual(["builtin:bash"]);
    });

    it("rejects invalid tool names with a clear message", () => {
        expect(() => new ToolSet().addBuiltIn("has:colon")).toThrowError(/match/i);
        expect(() => new ToolSet().addMcp("has space")).toThrowError(/match/i);
        expect(() => new ToolSet().addCustom("")).toThrowError(/match/i);
    });

    it("BuiltInTools.Isolated contains expected within-session-only tools", () => {
        // Spot-check: shell / fs / network / cross-session tools must NOT appear.
        expect(BuiltInTools.Isolated).not.toContain("bash");
        expect(BuiltInTools.Isolated).not.toContain("edit");
        expect(BuiltInTools.Isolated).not.toContain("grep");
        expect(BuiltInTools.Isolated).not.toContain("web_fetch");
        // And a couple of expected members.
        expect(BuiltInTools.Isolated).toContain("ask_user");
        expect(BuiltInTools.Isolated).toContain("task_complete");
    });
});

describe("CopilotClient mode = 'empty'", () => {
    it("rejects construction without baseDirectory or sessionFs", () => {
        expect(
            () =>
                new CopilotClient({
                    mode: "empty",
                    connection: RuntimeConnection.forStdio(),
                })
        ).toThrowError(/empty mode|baseDirectory|sessionFs/i);
    });

    it("accepts construction with baseDirectory", () => {
        const c = new CopilotClient({
            mode: "empty",
            baseDirectory: "/tmp/copilot-test",
            connection: RuntimeConnection.forStdio(),
        });
        expect(c).toBeInstanceOf(CopilotClient);
    });

    it("accepts construction with sessionFs", () => {
        const c = new CopilotClient({
            mode: "empty",
            sessionFs: {
                initialCwd: "/tmp/copilot-test-cwd",
                sessionStatePath: "/tmp/copilot-test-state",
                conventions: "posix",
                createProvider: (() => ({}) as any) as any,
            },
            connection: RuntimeConnection.forStdio(),
        });
        expect(c).toBeInstanceOf(CopilotClient);
    });

    it("rejects createSession without availableTools", async () => {
        const client = new CopilotClient({
            mode: "empty",
            baseDirectory: "/tmp/copilot-test",
        });
        await client.start();
        onTestFinished(() => client.forceStop());
        // Stub the wire so we don't actually need a runtime; the empty-mode
        // guard runs before the RPC is issued so this still fails fast.
        vi.spyOn((client as any).connection!, "sendRequest").mockResolvedValue({
            sessionId: "irrelevant",
        });

        await expect(
            client.createSession({ onPermissionRequest: approveAll })
        ).rejects.toThrowError(/empty.*availableTools/i);
    });
});

describe("Tool filter wiring", () => {
    async function setupClient(mode?: "empty" | "copilot-cli") {
        const client = new CopilotClient({
            mode,
            baseDirectory: mode === "empty" ? "/tmp/copilot-test" : undefined,
        });
        await client.start();
        onTestFinished(() => client.forceStop());
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create" || method === "session.resume") {
                    return { sessionId: params.sessionId };
                }
                throw new Error(`Unexpected method: ${method}`);
            });
        return { client, spy };
    }

    it("converts ToolSet to plain string[] on the wire", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn("bash").addMcp("*"),
        });
        const payload = spy.mock.calls.find(([m]) => m === "session.create")![1] as any;
        expect(payload.availableTools).toEqual(["builtin:bash", "mcp:*"]);
    });

    it("forwards plain string[] unchanged", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: ["view", "builtin:bash"],
        });
        const payload = spy.mock.calls.find(([m]) => m === "session.create")![1] as any;
        expect(payload.availableTools).toEqual(["view", "builtin:bash"]);
    });

    it("rejects bare '*' in availableTools with actionable error", async () => {
        const { client } = await setupClient();
        await expect(
            client.createSession({
                onPermissionRequest: approveAll,
                availableTools: ["*"],
            })
        ).rejects.toThrowError(/bare wildcard|addBuiltIn|addMcp|addCustom/);
    });

    it("rejects bare '*' in excludedTools", async () => {
        const { client } = await setupClient();
        await expect(
            client.createSession({
                onPermissionRequest: approveAll,
                excludedTools: ["*"],
            })
        ).rejects.toThrowError(/bare wildcard/);
    });

    it("always sends toolFilterMode: denyPrecedence in copilot-cli mode", async () => {
        const { client, spy } = await setupClient("copilot-cli");
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: ["builtin:bash"],
        });
        const payload = spy.mock.calls.find(([m]) => m === "session.create")![1] as any;
        expect(payload.toolFilterMode).toBe("denyPrecedence");
    });

    it("always sends toolFilterMode: denyPrecedence in empty mode", async () => {
        const { client, spy } = await setupClient("empty");
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        const payload = spy.mock.calls.find(([m]) => m === "session.create")![1] as any;
        expect(payload.toolFilterMode).toBe("denyPrecedence");
    });

    it("applies the same filter normalization on session.resume", async () => {
        const { client, spy } = await setupClient("empty");
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn("bash"),
        });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(["view", "task_complete"]),
        });
        const payload = spy.mock.calls.find(([m]) => m === "session.resume")![1] as any;
        expect(payload.availableTools).toEqual(["builtin:view", "builtin:task_complete"]);
        expect(payload.toolFilterMode).toBe("denyPrecedence");
    });
});
