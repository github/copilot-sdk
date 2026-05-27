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
                if (method === "session.options.update") {
                    return { success: true };
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

    it("always sends toolFilterPrecedence: excluded in copilot-cli mode", async () => {
        const { client, spy } = await setupClient("copilot-cli");
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: ["builtin:bash"],
        });
        const payload = spy.mock.calls.find(([m]) => m === "session.create")![1] as any;
        expect(payload.toolFilterPrecedence).toBe("excluded");
    });

    it("always sends toolFilterPrecedence: excluded in empty mode", async () => {
        const { client, spy } = await setupClient("empty");
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        const payload = spy.mock.calls.find(([m]) => m === "session.create")![1] as any;
        expect(payload.toolFilterPrecedence).toBe("excluded");
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
        expect(payload.toolFilterPrecedence).toBe("excluded");
    });
});

describe("Empty-mode safe defaults", () => {
    async function setupClient(mode: "empty" | "copilot-cli" = "empty") {
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
                if (method === "session.options.update") {
                    return { success: true };
                }
                throw new Error(`Unexpected method: ${method}`);
            });
        return { client, spy };
    }

    function createPayload(spy: ReturnType<typeof vi.spyOn>) {
        return (spy as any).mock.calls.find(([m]: [string]) => m === "session.create")![1] as any;
    }

    function patchCall(spy: ReturnType<typeof vi.spyOn>) {
        return (spy as any).mock.calls.find(
            ([m]: [string]) => m === "session.options.update"
        )![1] as any;
    }

    it("forces enableSessionTelemetry=false when app didn't opt in", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        expect(createPayload(spy).enableSessionTelemetry).toBe(false);
    });

    it("respects app-supplied enableSessionTelemetry=true override", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            enableSessionTelemetry: true,
        });
        expect(createPayload(spy).enableSessionTelemetry).toBe(true);
    });

    it("injects environment_context removal when app didn't pass systemMessage", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        const payload = createPayload(spy);
        expect(payload.systemMessage).toEqual({
            mode: "customize",
            sections: { environment_context: { action: "remove" } },
        });
    });

    it("passes through app-supplied systemMessage in replace mode", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            systemMessage: { mode: "replace", content: "you are a haiku bot" },
        });
        expect(createPayload(spy).systemMessage).toEqual({
            mode: "replace",
            content: "you are a haiku bot",
        });
    });

    it("promotes append-mode systemMessage to customize with env_context removal in empty mode", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            systemMessage: { mode: "append", content: "extra rules" },
        });
        expect(createPayload(spy).systemMessage).toEqual({
            mode: "customize",
            content: "extra rules",
            sections: { environment_context: { action: "remove" } },
        });
    });

    it("promotes default-mode (append) systemMessage in empty mode", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            systemMessage: { content: "extra rules" },
        });
        expect(createPayload(spy).systemMessage).toEqual({
            mode: "customize",
            content: "extra rules",
            sections: { environment_context: { action: "remove" } },
        });
    });

    it("adds environment_context removal to customize mode when app didn't set it", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            systemMessage: {
                mode: "customize",
                sections: { tool_use: { action: "remove" } },
            },
        });
        expect(createPayload(spy).systemMessage).toEqual({
            mode: "customize",
            sections: {
                tool_use: { action: "remove" },
                environment_context: { action: "remove" },
            },
        });
    });

    it("leaves customize-mode systemMessage alone when app set environment_context", async () => {
        const { client, spy } = await setupClient();
        const supplied = {
            mode: "customize" as const,
            sections: {
                environment_context: { action: "replace" as const, content: "custom env" },
            },
        };
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            systemMessage: supplied,
        });
        expect(createPayload(spy).systemMessage).toEqual(supplied);
    });

    it("sends session.options.update with safe defaults after session.create", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        const patch = patchCall(spy);
        expect(patch).toMatchObject({
            skipCustomInstructions: true,
            customAgentsLocalOnly: true,
            coauthorEnabled: false,
            manageScheduleEnabled: false,
            installedPlugins: [],
        });
        expect(patch.sessionId).toBeDefined();
    });

    it("sends the patch AFTER session.create succeeds (order matters)", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        const methods = spy.mock.calls.map(([m]) => m);
        const createIdx = methods.indexOf("session.create");
        const patchIdx = methods.indexOf("session.options.update");
        expect(createIdx).toBeGreaterThanOrEqual(0);
        expect(patchIdx).toBeGreaterThan(createIdx);
    });

    it("does NOT send patch or systemMessage override in copilot-cli mode", async () => {
        const { client, spy } = await setupClient("copilot-cli");
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: ["builtin:bash"],
        });
        const methods = spy.mock.calls.map(([m]) => m);
        expect(methods).not.toContain("session.options.update");
        expect(createPayload(spy).systemMessage).toBeUndefined();
        expect(createPayload(spy).enableSessionTelemetry).toBeUndefined();
    });

    it("tears the session down if the post-create patch fails", async () => {
        const client = new CopilotClient({ mode: "empty", baseDirectory: "/tmp/copilot-test" });
        await client.start();
        onTestFinished(() => client.forceStop());
        vi.spyOn((client as any).connection!, "sendRequest").mockImplementation(
            async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                if (method === "session.options.update") {
                    throw new Error("update rejected");
                }
                throw new Error(`Unexpected method: ${method}`);
            }
        );
        await expect(
            client.createSession({
                onPermissionRequest: approveAll,
                availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            })
        ).rejects.toThrowError(/update rejected/);
        // Session must not remain registered after the failed patch.
        expect((client as any).sessions.size).toBe(0);
    });

    it("also applies overrides on session.resume", async () => {
        const { client, spy } = await setupClient();
        // First create so we have a session id to resume.
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        spy.mockClear();
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        const resumePayload = spy.mock.calls.find(([m]) => m === "session.resume")![1] as any;
        expect(resumePayload.enableSessionTelemetry).toBe(false);
        expect(resumePayload.systemMessage).toEqual({
            mode: "customize",
            sections: { environment_context: { action: "remove" } },
        });
        const patch = spy.mock.calls.find(([m]) => m === "session.options.update")![1] as any;
        expect(patch.skipCustomInstructions).toBe(true);
    });

    it("respects app-supplied overrides for the four post-create flags in empty mode", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            skipCustomInstructions: false,
            customAgentsLocalOnly: false,
            coauthorEnabled: true,
            manageScheduleEnabled: true,
        });
        const patch = patchCall(spy);
        expect(patch).toMatchObject({
            skipCustomInstructions: false,
            customAgentsLocalOnly: false,
            coauthorEnabled: true,
            manageScheduleEnabled: true,
            installedPlugins: [],
        });
    });

    it("applies restrictive defaults for granular multitenancy flags in empty mode", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        const payload = createPayload(spy);
        expect(payload.skipEmbeddingRetrieval).toBe(true);
        expect(payload.enableOnDemandInstructionDiscovery).toBe(false);
        expect(payload.enableFileHooks).toBe(false);
        expect(payload.enableHostGitOperations).toBe(false);
        expect(payload.enableSessionStore).toBe(false);
        expect(payload.enableSkills).toBe(false);
    });

    it("respects app-supplied overrides for granular multitenancy flags in empty mode", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            skipEmbeddingRetrieval: false,
            enableOnDemandInstructionDiscovery: true,
            enableFileHooks: true,
            enableHostGitOperations: true,
            enableSessionStore: true,
            enableSkills: true,
        });
        const payload = createPayload(spy);
        expect(payload.skipEmbeddingRetrieval).toBe(false);
        expect(payload.enableOnDemandInstructionDiscovery).toBe(true);
        expect(payload.enableFileHooks).toBe(true);
        expect(payload.enableHostGitOperations).toBe(true);
        expect(payload.enableSessionStore).toBe(true);
        expect(payload.enableSkills).toBe(true);
    });

    it("passes organizationCustomInstructions through on create", async () => {
        const { client, spy } = await setupClient();
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
            organizationCustomInstructions: "Follow org coding standards",
        });
        const payload = createPayload(spy);
        expect(payload.organizationCustomInstructions).toBe("Follow org coding standards");
    });

    it("does NOT apply granular multitenancy flag defaults in copilot-cli mode", async () => {
        const { client, spy } = await setupClient("copilot-cli");
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: ["builtin:bash"],
        });
        const payload = createPayload(spy);
        expect(payload.skipEmbeddingRetrieval).toBeUndefined();
        expect(payload.enableOnDemandInstructionDiscovery).toBeUndefined();
        expect(payload.enableFileHooks).toBeUndefined();
        expect(payload.enableHostGitOperations).toBeUndefined();
        expect(payload.enableSessionStore).toBeUndefined();
        expect(payload.enableSkills).toBeUndefined();
    });

    it("applies granular multitenancy flag defaults on session.resume in empty mode", async () => {
        const { client, spy } = await setupClient();
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        spy.mockClear();
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            availableTools: new ToolSet().addBuiltIn(BuiltInTools.Isolated),
        });
        const resumePayload = spy.mock.calls.find(([m]) => m === "session.resume")![1] as any;
        expect(resumePayload.skipEmbeddingRetrieval).toBe(true);
        expect(resumePayload.enableOnDemandInstructionDiscovery).toBe(false);
        expect(resumePayload.enableFileHooks).toBe(false);
        expect(resumePayload.enableHostGitOperations).toBe(false);
        expect(resumePayload.enableSessionStore).toBe(false);
        expect(resumePayload.enableSkills).toBe(false);
    });

    it("forwards the four flags in copilot-cli mode when the app sets them", async () => {
        const { client, spy } = await setupClient("copilot-cli");
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: ["builtin:bash"],
            skipCustomInstructions: true,
            manageScheduleEnabled: true,
        });
        const patch = patchCall(spy);
        expect(patch).toMatchObject({
            skipCustomInstructions: true,
            manageScheduleEnabled: true,
        });
        expect(patch.customAgentsLocalOnly).toBeUndefined();
        expect(patch.coauthorEnabled).toBeUndefined();
        expect(patch.installedPlugins).toBeUndefined();
    });
});
