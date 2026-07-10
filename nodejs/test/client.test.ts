/* eslint-disable @typescript-eslint/no-explicit-any */
import { EventEmitter } from "node:events";
import { PassThrough } from "stream";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    approveAll,
    CopilotClient,
    createCanvas,
    RuntimeConnection,
    type GitHubTelemetryNotification,
    type ModelInfo,
} from "../src/index.js";
import { CopilotSession } from "../src/session.js";
import { defaultJoinSessionPermissionHandler } from "../src/types.js";

// This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.test.ts instead

async function stopClient(client: CopilotClient): Promise<void> {
    await client.stop();
}

describe("CopilotClient", () => {
    it("disposes the stdio connection when child stdin emits an error", async () => {
        const client = new CopilotClient();
        onTestFinished(() => client.forceStop());

        const stdin = new PassThrough();
        const stdout = new PassThrough();
        (client as any).cliProcess = { stdin, stdout };
        await (client as any).connectToChildProcessViaStdio();

        const dispose = vi.spyOn((client as any).connection, "dispose");

        const boom = new Error("broken pipe");
        expect(() => stdin.emit("error", boom)).not.toThrow();
        expect(dispose).toHaveBeenCalledOnce();
    });

    it("does not respond to v3 permission requests when handler returns no-result", async () => {
        const session = new CopilotSession("session-1", {} as any);
        session.registerPermissionHandler(() => ({ kind: "no-result" }));
        const spy = vi.spyOn(session.rpc.permissions, "handlePendingPermissionRequest");

        await (session as any)._executePermissionAndRespond("request-1", { kind: "write" });

        expect(spy).not.toHaveBeenCalled();
    });

    it("responds to MCP OAuth requests with host token data", async () => {
        const sendRequest = vi.fn(async () => ({ success: true }));
        let observedRequest: any;
        const session = new CopilotSession(
            "session-1",
            { sendRequest } as any,
            undefined,
            undefined,
            {
                mcpAuthHandler: async (request) => {
                    observedRequest = request;
                    return {
                        accessToken: "host-token",
                        tokenType: "Bearer",
                        expiresIn: 3600,
                    };
                },
            }
        );

        await (session as any)._executeMcpAuthAndRespond({
            requestId: "oauth-request",
            serverName: "oauth-server",
            serverUrl: "https://example.com/mcp",
            reason: "initial",
            wwwAuthenticateParams: {
                resourceMetadataUrl: "https://example.com/.well-known/oauth-protected-resource",
            },
            resourceMetadata: '{"resource":"https://example.com/mcp"}',
            staticClientConfig: {
                clientId: "static-client",
                clientSecret: "static-secret",
                grantType: "client_credentials",
                publicClient: false,
            },
        });

        expect(observedRequest.resourceMetadata).toBe('{"resource":"https://example.com/mcp"}');
        expect(observedRequest.staticClientConfig).toEqual({
            clientId: "static-client",
            clientSecret: "static-secret",
            grantType: "client_credentials",
            publicClient: false,
        });
        expect(sendRequest).toHaveBeenCalledWith("session.mcp.oauth.handlePendingRequest", {
            sessionId: "session-1",
            requestId: "oauth-request",
            result: {
                kind: "token",
                accessToken: "host-token",
                tokenType: "Bearer",
                expiresIn: 3600,
            },
        });
    });

    it("passes MCP OAuth requests through when optional metadata is absent", async () => {
        let observedRequest: any;
        const session = new CopilotSession(
            "session-1",
            { sendRequest: vi.fn(async () => ({ success: true })) } as any,
            undefined,
            undefined,
            {
                mcpAuthHandler: async (request) => {
                    observedRequest = request;
                    return { kind: "cancelled" };
                },
            }
        );

        await (session as any)._executeMcpAuthAndRespond({
            requestId: "oauth-request",
            serverName: "oauth-server",
            serverUrl: "https://example.com/mcp",
            reason: "initial",
        });

        expect(observedRequest.reason).toBe("initial");
        expect(observedRequest.resourceMetadata).toBeUndefined();
        expect(observedRequest.wwwAuthenticateParams).toBeUndefined();
    });

    it("registers interest in MCP OAuth required events after create when an auth handler is configured", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.eventLog.registerInterest") {
                    return { id: "interest-1" };
                }
                if (method === "session.create") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.createSession({
            onPermissionRequest: approveAll,
            onMcpAuthRequest: () => ({ kind: "cancelled" }),
        });

        expect(spy.mock.calls[0][0]).toBe("session.create");
        expect(spy.mock.calls[1]).toEqual([
            "session.eventLog.registerInterest",
            expect.objectContaining({ eventType: "mcp.oauth_required" }),
        ]);
        expect(spy.mock.calls[1][1].sessionId).toBe(spy.mock.calls[0][1].sessionId);
    });

    it("does not register MCP OAuth interest without an auth handler", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.createSession({
            onPermissionRequest: approveAll,
            onEvent: () => {},
        });

        expect(spy).not.toHaveBeenCalledWith(
            "session.eventLog.registerInterest",
            expect.objectContaining({ eventType: "mcp.oauth_required" })
        );
        expect(spy).toHaveBeenCalledWith(
            "session.create",
            expect.objectContaining({ requestPermission: true })
        );
    });

    it("registers MCP OAuth interest after cloud create only when an auth handler is configured", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        let cloudCreateCount = 0;
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, _params: any) => {
                if (method === "session.eventLog.registerInterest") {
                    return { id: "interest-1" };
                }
                if (method === "session.create")
                    return { sessionId: `server-assigned-session-${++cloudCreateCount}` };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.createSession({
            onPermissionRequest: approveAll,
            cloud: { repository: { owner: "github", name: "copilot-sdk", branch: "main" } },
        });

        expect(spy).not.toHaveBeenCalledWith(
            "session.eventLog.registerInterest",
            expect.objectContaining({ eventType: "mcp.oauth_required" })
        );

        spy.mockClear();
        await client.createSession({
            onPermissionRequest: approveAll,
            onMcpAuthRequest: () => ({ kind: "cancelled" }),
            cloud: { repository: { owner: "github", name: "copilot-sdk", branch: "main" } },
        });

        expect(spy.mock.calls[0][0]).toBe("session.create");
        expect(spy.mock.calls[1]).toEqual([
            "session.eventLog.registerInterest",
            { sessionId: "server-assigned-session-2", eventType: "mcp.oauth_required" },
        ]);
    });

    it("registers MCP OAuth interest after resuming only when an auth handler is configured", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.eventLog.registerInterest") {
                    return { id: "interest-1" };
                }
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.resumeSession("session-with-auth", {
            onPermissionRequest: approveAll,
            onMcpAuthRequest: () => ({ kind: "cancelled" }),
        });

        // `session.eventLog.registerInterest` is session-scoped: the runtime only
        // registers the session id while handling `session.resume`, so resume must
        // be sent BEFORE registering interest.
        const resumeIndex = spy.mock.calls.findIndex(([method]) => method === "session.resume");
        const interestIndex = spy.mock.calls.findIndex(
            ([method]) => method === "session.eventLog.registerInterest"
        );
        expect(resumeIndex).toBeGreaterThanOrEqual(0);
        expect(interestIndex).toBeGreaterThanOrEqual(0);
        expect(resumeIndex).toBeLessThan(interestIndex);
        expect(spy.mock.calls[resumeIndex][1]).toEqual(
            expect.objectContaining({ sessionId: "session-with-auth", requestPermission: true })
        );
        expect(spy.mock.calls[interestIndex][1]).toEqual({
            sessionId: "session-with-auth",
            eventType: "mcp.oauth_required",
        });

        spy.mockClear();
        await client.resumeSession("session-without-auth", {
            onPermissionRequest: approveAll,
            onEvent: () => {},
        });

        expect(spy).not.toHaveBeenCalledWith(
            "session.eventLog.registerInterest",
            expect.objectContaining({ eventType: "mcp.oauth_required" })
        );
        expect(spy).toHaveBeenCalledWith(
            "session.resume",
            expect.objectContaining({ sessionId: "session-without-auth", requestPermission: true })
        );
    });

    it("forwards canvas declarations and request flags in session.create", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const canvas = createCanvas({
            id: "counter",
            displayName: "Counter",
            description: "A counter canvas",
            actions: [{ name: "increment", description: "Increment the counter" }],
            open: () => ({ url: "https://example.test/counter" }),
        });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create")
                    return { sessionId: params.sessionId ?? "session-id" };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [canvas],
            requestCanvasRenderer: true,
            requestExtensions: true,
            extensionInfo: { source: "github-app", name: "counter-provider" },
            canvasProvider: { id: "app:builtin:window-1", name: "Built-in" },
        });

        const payload = spy.mock.calls.find(([method]) => method === "session.create")![1] as any;
        expect(payload.canvases).toEqual([
            expect.objectContaining({
                id: "counter",
                displayName: "Counter",
                description: "A counter canvas",
                actions: [{ name: "increment", description: "Increment the counter" }],
            }),
        ]);
        expect(payload.requestCanvasRenderer).toBe(true);
        expect(payload.requestExtensions).toBe(true);
        expect(payload.extensionInfo).toEqual({
            source: "github-app",
            name: "counter-provider",
        });
        expect(payload.canvasProvider).toEqual({
            id: "app:builtin:window-1",
            name: "Built-in",
        });
    });

    it("forwards canvas declarations in session.resume", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const canvas = createCanvas({
            id: "counter",
            displayName: "Counter",
            description: "A counter canvas",
            open: () => ({ url: "https://example.test/counter" }),
        });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            canvases: [canvas],
            requestCanvasRenderer: true,
            requestExtensions: true,
            extensionInfo: { source: "github-app", name: "counter-provider" },
            canvasProvider: { id: "app:builtin:window-1" },
        });

        const payload = spy.mock.calls.find(([method]) => method === "session.resume")![1] as any;
        expect(payload.canvases).toEqual([expect.objectContaining({ id: "counter" })]);
        expect(payload.requestCanvasRenderer).toBe(true);
        expect(payload.requestExtensions).toBe(true);
        expect(payload.extensionInfo).toEqual({
            source: "github-app",
            name: "counter-provider",
        });
        expect(payload.canvasProvider).toEqual({ id: "app:builtin:window-1" });
        expect(payload.openCanvasInstances).toBeUndefined();
    });

    it("forwards reasoningSummary in session.create and session.resume", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            reasoningSummary: "concise",
        });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            reasoningSummary: "none",
        });

        const createPayload = spy.mock.calls.find(
            ([method]) => method === "session.create"
        )![1] as any;
        const resumePayload = spy.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as any;
        expect(createPayload.reasoningSummary).toBe("concise");
        expect(resumePayload.reasoningSummary).toBe("none");
    });

    it("forwards contextTier in session.create and session.resume", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            contextTier: "long_context",
        });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            contextTier: "default",
        });

        const createPayload = spy.mock.calls.find(
            ([method]) => method === "session.create"
        )![1] as any;
        const resumePayload = spy.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as any;
        expect(createPayload.contextTier).toBe("long_context");
        expect(resumePayload.contextTier).toBe("default");
    });

    it("forwards new session options in session.create and session.resume", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            enableCitations: true,
            excludedBuiltinAgents: ["explore"],
            sessionLimits: { maxAiCredits: 30 },
        });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            enableCitations: false,
            excludedBuiltinAgents: ["task"],
            sessionLimits: { maxAiCredits: 15 },
        });

        const createPayload = spy.mock.calls.find(
            ([method]) => method === "session.create"
        )![1] as any;
        const resumePayload = spy.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as any;
        expect(createPayload.enableCitations).toBe(true);
        expect(createPayload.excludedBuiltinAgents).toEqual(["explore"]);
        expect(createPayload.sessionLimits).toEqual({ maxAiCredits: 30 });
        expect(resumePayload.enableCitations).toBe(false);
        expect(resumePayload.excludedBuiltinAgents).toEqual(["task"]);
        expect(resumePayload.sessionLimits).toEqual({ maxAiCredits: 15 });
    });

    it("opts into GitHub telemetry forwarding when onGitHubTelemetry is provided", async () => {
        const client = new CopilotClient({ onGitHubTelemetry: () => {} });
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        const session = await client.createSession({ onPermissionRequest: approveAll });
        await client.resumeSession(session.sessionId, { onPermissionRequest: approveAll });

        const createPayload = spy.mock.calls.find(
            ([method]) => method === "session.create"
        )![1] as any;
        const resumePayload = spy.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as any;
        expect(createPayload.enableGitHubTelemetryForwarding).toBe(true);
        expect(resumePayload.enableGitHubTelemetryForwarding).toBe(true);
    });

    it("opts into GitHub telemetry forwarding on the connect handshake when a handler is provided", async () => {
        const client = new CopilotClient({ onGitHubTelemetry: () => {} });
        onTestFinished(() => stopClient(client));

        const sendRequest = vi.fn(async (method: string) => {
            if (method === "connect") return { ok: true, protocolVersion: 3, version: "test" };
            throw new Error(`Unexpected method: ${method}`);
        });
        (client as any).connection = { sendRequest };

        await (client as any).verifyProtocolVersion();

        const connectCall = sendRequest.mock.calls.find(([method]) => method === "connect");
        expect(connectCall).toBeDefined();
        expect((connectCall![1] as any).enableGitHubTelemetryForwarding).toBe(true);
    });

    it("does not opt into GitHub telemetry forwarding on the connect handshake without a handler", async () => {
        const client = new CopilotClient();
        onTestFinished(() => stopClient(client));

        const sendRequest = vi.fn(async (method: string) => {
            if (method === "connect") return { ok: true, protocolVersion: 3, version: "test" };
            throw new Error(`Unexpected method: ${method}`);
        });
        (client as any).connection = { sendRequest };

        await (client as any).verifyProtocolVersion();

        const connectCall = sendRequest.mock.calls.find(([method]) => method === "connect");
        expect(connectCall).toBeDefined();
        expect((connectCall![1] as any).enableGitHubTelemetryForwarding).toBeUndefined();
    });

    it("does not opt into GitHub telemetry forwarding without a handler", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.createSession({ onPermissionRequest: approveAll });

        const createPayload = spy.mock.calls.find(
            ([method]) => method === "session.create"
        )![1] as any;
        expect(createPayload.enableGitHubTelemetryForwarding).toBeUndefined();
    });

    it("dispatches a real gitHubTelemetry.event wire message to the handler", async () => {
        const { createMessageConnection, StreamMessageReader, StreamMessageWriter } =
            await import("vscode-jsonrpc/node.js");
        const { registerClientGlobalApiHandlers } = await import("../src/generated/rpc.js");

        const clientToServer = new PassThrough();
        const serverToClient = new PassThrough();

        const clientConn = createMessageConnection(
            new StreamMessageReader(serverToClient),
            new StreamMessageWriter(clientToServer)
        );
        const serverConn = createMessageConnection(
            new StreamMessageReader(clientToServer),
            new StreamMessageWriter(serverToClient)
        );
        onTestFinished(() => {
            clientConn.dispose();
            serverConn.dispose();
        });

        const received: GitHubTelemetryNotification[] = [];
        let resolveReceived: () => void;
        const got = new Promise<void>((resolve) => {
            resolveReceived = resolve;
        });

        registerClientGlobalApiHandlers(clientConn, {
            gitHubTelemetry: {
                event: async (notification) => {
                    received.push(notification);
                    resolveReceived();
                },
            },
        });

        clientConn.listen();
        serverConn.listen();

        const notification: GitHubTelemetryNotification = {
            sessionId: "session-1",
            restricted: false,
            event: {
                kind: "tool_call_executed",
                properties: { tool: "shell" },
                metrics: { duration_ms: 42 },
            },
        };

        // Deliver the event as a real JSON-RPC *notification* (no id) and confirm
        // the generated dispatcher routes it to the registered handler. The runtime
        // forwards telemetry via `sendNotification`, which only fires `onNotification`
        // handlers — an `onRequest` registration would never be invoked, so sending a
        // notification here guards against regressing back to request-style dispatch.
        serverConn.sendNotification("gitHubTelemetry.event", notification);
        await got;

        expect(received).toEqual([notification]);
    });

    it("registers no gitHubTelemetry handler when onGitHubTelemetry is omitted", () => {
        const client = new CopilotClient();
        onTestFinished(() => stopClient(client));

        const handlers = (client as any).clientGlobalHandlers;
        expect(handlers.gitHubTelemetry).toBeUndefined();
    });

    it("forwards gitHubTelemetry events to the onGitHubTelemetry handler", () => {
        const received: GitHubTelemetryNotification[] = [];
        const client = new CopilotClient({ onGitHubTelemetry: (n) => received.push(n) });
        onTestFinished(() => stopClient(client));

        const handlers = (client as any).clientGlobalHandlers;
        expect(handlers.gitHubTelemetry).toBeDefined();

        const notification: GitHubTelemetryNotification = {
            sessionId: "session-1",
            restricted: false,
            event: { kind: "tool_call_executed", properties: {}, metrics: {} },
        };
        handlers.gitHubTelemetry.event(notification);
        expect(received).toEqual([notification]);
    });

    it("forwards expAssignments in session.create and session.resume", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        const assignments = {
            Parameters: { copilot_exp_flag: "treatment" },
            AssignmentContext: "ctx-123",
        };

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            expAssignments: assignments,
        });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            expAssignments: assignments,
        });

        const createPayload = spy.mock.calls.find(
            ([method]) => method === "session.create"
        )![1] as any;
        const resumePayload = spy.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as any;
        expect(createPayload.expAssignments).toEqual(assignments);
        expect(resumePayload.expAssignments).toEqual(assignments);
    });

    it("omits expAssignments from session.create and session.resume when unset", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        const session = await client.createSession({ onPermissionRequest: approveAll });
        await client.resumeSession(session.sessionId, { onPermissionRequest: approveAll });

        const createPayload = spy.mock.calls.find(
            ([method]) => method === "session.create"
        )![1] as any;
        const resumePayload = spy.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as any;
        expect(createPayload.expAssignments).toBeUndefined();
        expect(resumePayload.expAssignments).toBeUndefined();
    });

    it("forwards capi options in session.create and session.resume", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            capi: { enableWebSocketResponses: false },
        });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            capi: { enableWebSocketResponses: false },
        });

        const createPayload = spy.mock.calls.find(
            ([method]) => method === "session.create"
        )![1] as any;
        const resumePayload = spy.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as any;
        expect(createPayload.capi).toEqual({ enableWebSocketResponses: false });
        expect(resumePayload.capi).toEqual({ enableWebSocketResponses: false });
    });

    it("forwards pluginDirectories and largeOutput in session.create and session.resume", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId };
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        const pluginDirs = ["/tmp/plugins/a", "/tmp/plugins/b"];
        const largeOutput = {
            enabled: true,
            maxSizeBytes: 1024,
            outputDirectory: "/tmp/large-output",
        };
        const expectedWireLargeOutput = {
            enabled: true,
            maxSizeBytes: 1024,
            outputDir: "/tmp/large-output",
        };

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            pluginDirectories: pluginDirs,
            largeOutput,
        });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            pluginDirectories: pluginDirs,
            largeOutput,
        });

        const createPayload = spy.mock.calls.find(
            ([method]) => method === "session.create"
        )![1] as any;
        const resumePayload = spy.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as any;
        expect(createPayload.pluginDirectories).toEqual(pluginDirs);
        expect(createPayload.largeOutput).toEqual(expectedWireLargeOutput);
        expect(resumePayload.pluginDirectories).toEqual(pluginDirs);
        expect(resumePayload.largeOutput).toEqual(expectedWireLargeOutput);
    });

    it("routes canvas.action.invoke to registered canvas action handlers via clientSessionApis", async () => {
        const canvas = createCanvas({
            id: "counter",
            displayName: "Counter",
            description: "A counter canvas",
            open: ({ instanceId }) => ({ url: `https://example.test/${instanceId}` }),
            actions: [
                {
                    name: "increment",
                    handler: ({ actionName, input }) => ({ actionName, input }),
                },
            ],
        });
        const session = new CopilotSession("session-1", {} as any);
        session.registerCanvases([canvas]);

        const result = await session.clientSessionApis.canvas!.invoke({
            sessionId: session.sessionId,
            extensionId: "project:counter",
            canvasId: "counter",
            instanceId: "counter-1",
            actionName: "increment",
            input: { amount: 1 },
        });

        expect(result).toEqual({ actionName: "increment", input: { amount: 1 } });
    });

    it("tracks open canvases from live session.canvas.opened events", () => {
        const session = new CopilotSession("session-1", {} as any);
        const warn = vi.spyOn(console, "warn").mockImplementation(() => {});

        (session as any)._dispatchEvent({
            type: "session.canvas.opened",
            data: { instanceId: "missing-required-fields" },
        });
        (session as any)._dispatchEvent({
            type: "session.canvas.opened",
            data: {
                extensionId: "project:counter",
                extensionName: "Counter Provider",
                canvasId: "counter",
                instanceId: "counter-1",
                title: "Counter",
                status: "ready",
                url: "https://example.test/counter",
                input: { seed: 1 },
            },
        });
        (session as any)._dispatchEvent({
            type: "session.canvas.opened",
            data: {
                extensionId: "project:logs",
                canvasId: "logs",
                instanceId: "logs-1",
                title: "Logs",
            },
        });

        expect(warn).toHaveBeenCalledWith("failed to deserialize session.canvas.opened payload");
        expect(session.openCanvases.map((canvas) => canvas.instanceId)).toEqual([
            "counter-1",
            "logs-1",
        ]);

        (session as any)._dispatchEvent({
            type: "session.canvas.opened",
            data: {
                extensionId: "project:counter",
                extensionName: "Counter Provider",
                canvasId: "counter",
                instanceId: "counter-1",
                title: "Counter Updated",
                status: "reconnected",
                url: "https://example.test/counter-updated",
                input: { seed: 2 },
            },
        });

        expect(session.openCanvases).toHaveLength(2);
        expect(session.openCanvases[0]).toMatchObject({
            instanceId: "counter-1",
            title: "Counter Updated",
            status: "reconnected",
            url: "https://example.test/counter-updated",
            input: { seed: 2 },
        });
        expect(session.openCanvases[1].instanceId).toBe("logs-1");
        warn.mockRestore();
    });

    it("removes open canvases on live session.canvas.closed events", () => {
        const session = new CopilotSession("session-1", {} as any);
        const warn = vi.spyOn(console, "warn").mockImplementation(() => {});

        (session as any)._dispatchEvent({
            type: "session.canvas.opened",
            data: {
                extensionId: "project:counter",
                canvasId: "counter",
                instanceId: "counter-1",
                title: "Counter",
            },
        });
        (session as any)._dispatchEvent({
            type: "session.canvas.opened",
            data: {
                extensionId: "project:logs",
                canvasId: "logs",
                instanceId: "logs-1",
                title: "Logs",
            },
        });
        expect(session.openCanvases.map((canvas) => canvas.instanceId)).toEqual([
            "counter-1",
            "logs-1",
        ]);

        // Closing one instance removes it; the other remains.
        (session as any)._dispatchEvent({
            type: "session.canvas.closed",
            data: {
                extensionId: "project:counter",
                canvasId: "counter",
                instanceId: "counter-1",
            },
        });
        expect(session.openCanvases.map((canvas) => canvas.instanceId)).toEqual(["logs-1"]);

        // Closing an absent instance is a no-op (idempotent).
        (session as any)._dispatchEvent({
            type: "session.canvas.closed",
            data: {
                extensionId: "project:counter",
                canvasId: "counter",
                instanceId: "counter-1",
            },
        });
        expect(session.openCanvases.map((canvas) => canvas.instanceId)).toEqual(["logs-1"]);

        // A closed event missing instanceId warns and leaves the snapshot intact.
        (session as any)._dispatchEvent({
            type: "session.canvas.closed",
            data: { extensionId: "project:logs", canvasId: "logs" },
        });
        expect(warn).toHaveBeenCalledWith("failed to deserialize session.canvas.closed payload");
        expect(session.openCanvases.map((canvas) => canvas.instanceId)).toEqual(["logs-1"]);
        warn.mockRestore();
    });

    it("returns canvas_action_no_handler when no per-action handler is registered", async () => {
        const canvas = createCanvas({
            id: "counter",
            displayName: "Counter",
            description: "A counter canvas",
            open: () => ({ url: "https://example.test/counter" }),
        });

        const session = new CopilotSession("session-1", {} as any);
        session.registerCanvases([canvas]);

        await expect(
            session.clientSessionApis.canvas!.invoke({
                sessionId: session.sessionId,
                extensionId: "project:counter",
                canvasId: "counter",
                instanceId: "counter-1",
                actionName: "ghost",
                input: undefined,
            })
        ).rejects.toMatchObject({ code: "canvas_action_no_handler" });
    });

    it("throws for unknown canvasId in canvas.open via clientSessionApis", async () => {
        const session = new CopilotSession("session-1", {} as any);
        const canvas = createCanvas({
            id: "other",
            displayName: "Other",
            description: "Some other canvas",
            open: () => ({ url: "https://example.test/other" }),
        });
        session.registerCanvases([canvas]);

        await expect(
            session.clientSessionApis.canvas!.open({
                sessionId: session.sessionId,
                extensionId: "project:missing",
                canvasId: "missing",
                instanceId: "missing-1",
            })
        ).rejects.toThrow('No canvas registered with id "missing"');
    });

    it("forwards clientName in session.create request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({ clientName: "my-app", onPermissionRequest: approveAll });

        expect(spy).toHaveBeenCalledWith(
            "session.create",
            expect.objectContaining({ clientName: "my-app" })
        );
    });

    it("forwards cloud options in session.create request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockResolvedValue({ sessionId: "cloud-session" });
        await client.createSession({
            onPermissionRequest: approveAll,
            cloud: {
                repository: { owner: "github", name: "copilot-sdk", branch: "main" },
            },
        });

        expect(spy).toHaveBeenCalledWith(
            "session.create",
            expect.objectContaining({
                cloud: {
                    repository: { owner: "github", name: "copilot-sdk", branch: "main" },
                },
            })
        );
    });

    it("forwards clientName in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        // Mock sendRequest to capture the call without hitting the runtime
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, {
            clientName: "my-app",
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.resume",
            expect.objectContaining({ clientName: "my-app", sessionId: session.sessionId })
        );
        spy.mockRestore();
    });

    it("forwards enableSessionTelemetry in session.create request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({
            enableSessionTelemetry: false,
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.create",
            expect.objectContaining({ enableSessionTelemetry: false })
        );
    });

    it("forwards enableSessionTelemetry in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, {
            enableSessionTelemetry: false,
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.resume",
            expect.objectContaining({ enableSessionTelemetry: false, sessionId: session.sessionId })
        );
        spy.mockRestore();
    });

    it("forwards enableOnDemandInstructionDiscovery in session.create request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({
            enableOnDemandInstructionDiscovery: false,
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.create",
            expect.objectContaining({ enableOnDemandInstructionDiscovery: false })
        );
    });

    it("forwards enableOnDemandInstructionDiscovery in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, {
            enableOnDemandInstructionDiscovery: false,
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.resume",
            expect.objectContaining({
                enableOnDemandInstructionDiscovery: false,
                sessionId: session.sessionId,
            })
        );
        spy.mockRestore();
    });

    it("defaults includeSubAgentStreamingEvents to true in session.create when not specified", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({ onPermissionRequest: approveAll });

        const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
        expect(payload.includeSubAgentStreamingEvents).toBe(true);
    });

    it("forwards explicit false for includeSubAgentStreamingEvents in session.create", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({
            onPermissionRequest: approveAll,
            includeSubAgentStreamingEvents: false,
        });

        const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
        expect(payload.includeSubAgentStreamingEvents).toBe(false);
    });

    it("defaults includeSubAgentStreamingEvents to true in session.resume when not specified", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, { onPermissionRequest: approveAll });

        const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
        expect(payload.includeSubAgentStreamingEvents).toBe(true);
        spy.mockRestore();
    });

    it("forwards explicit false for includeSubAgentStreamingEvents in session.resume", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            includeSubAgentStreamingEvents: false,
        });

        const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
        expect(payload.includeSubAgentStreamingEvents).toBe(false);
        spy.mockRestore();
    });

    it("defaults mcpOAuthTokenStorage to 'in-memory' in session.create when mode is empty", async () => {
        const client = new CopilotClient({ mode: "empty", baseDirectory: "/tmp/copilot-test" });
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId ?? "s1" };
                if (method === "session.options.update") return {};
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.createSession({ onPermissionRequest: approveAll, availableTools: [] });

        const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
        expect(payload.mcpOAuthTokenStorage).toBe("in-memory");
    });

    it("does not send mcpOAuthTokenStorage in session.create when mode is copilot-cli", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({ onPermissionRequest: approveAll });

        const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
        expect(payload.mcpOAuthTokenStorage).toBeUndefined();
    });

    it("forwards explicit 'persistent' for mcpOAuthTokenStorage in session.create", async () => {
        const client = new CopilotClient({ mode: "empty", baseDirectory: "/tmp/copilot-test" });
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId ?? "s1" };
                if (method === "session.options.update") return {};
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: [],
            mcpOAuthTokenStorage: "persistent",
        });

        const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
        expect(payload.mcpOAuthTokenStorage).toBe("persistent");
    });

    it("defaults mcpOAuthTokenStorage to 'in-memory' in session.resume when mode is empty", async () => {
        const client = new CopilotClient({ mode: "empty", baseDirectory: "/tmp/copilot-test" });
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId ?? "s1" };
                if (method === "session.resume") return { sessionId: params.sessionId };
                if (method === "session.options.update") return {};
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.createSession({ onPermissionRequest: approveAll, availableTools: [] });
        await client.resumeSession("s1", { onPermissionRequest: approveAll, availableTools: [] });

        const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
        expect(payload.mcpOAuthTokenStorage).toBe("in-memory");
    });

    it("forwards explicit 'persistent' for mcpOAuthTokenStorage in session.resume", async () => {
        const client = new CopilotClient({ mode: "empty", baseDirectory: "/tmp/copilot-test" });
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId ?? "s1" };
                if (method === "session.resume") return { sessionId: params.sessionId };
                if (method === "session.options.update") return {};
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.createSession({ onPermissionRequest: approveAll, availableTools: [] });
        await client.resumeSession("s1", {
            onPermissionRequest: approveAll,
            availableTools: [],
            mcpOAuthTokenStorage: "persistent",
        });

        const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
        expect(payload.mcpOAuthTokenStorage).toBe("persistent");
    });

    it("defaults memory to { enabled: false } in session.create when mode is empty", async () => {
        const client = new CopilotClient({ mode: "empty", baseDirectory: "/tmp/copilot-test" });
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId ?? "s1" };
                if (method === "session.options.update") return {};
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.createSession({ onPermissionRequest: approveAll, availableTools: [] });

        const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
        expect(payload.memory).toEqual({ enabled: false });
    });

    it("does not send memory in session.create when mode is copilot-cli", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({ onPermissionRequest: approveAll });

        const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
        expect(payload.memory).toBeUndefined();
    });

    it("forwards explicit memory config in session.create even in empty mode", async () => {
        const client = new CopilotClient({ mode: "empty", baseDirectory: "/tmp/copilot-test" });
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId ?? "s1" };
                if (method === "session.options.update") return {};
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: [],
            memory: { enabled: true },
        });

        const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
        expect(payload.memory).toEqual({ enabled: true });
    });

    it("defaults memory to { enabled: false } in session.resume when mode is empty", async () => {
        const client = new CopilotClient({ mode: "empty", baseDirectory: "/tmp/copilot-test" });
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create") return { sessionId: params.sessionId ?? "s1" };
                if (method === "session.resume") return { sessionId: params.sessionId };
                if (method === "session.options.update") return {};
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.createSession({ onPermissionRequest: approveAll, availableTools: [] });
        await client.resumeSession("s1", { onPermissionRequest: approveAll, availableTools: [] });

        const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
        expect(payload.memory).toEqual({ enabled: false });
    });

    it("does not send memory in session.resume when mode is copilot-cli", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, { onPermissionRequest: approveAll });

        const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
        expect(payload.memory).toBeUndefined();
        spy.mockRestore();
    });

    it("forwards continuePendingWork in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            continuePendingWork: true,
        });

        const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
        expect(payload.continuePendingWork).toBe(true);
        spy.mockRestore();
    });

    it("omits continuePendingWork from session.resume payload when not specified", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, { onPermissionRequest: approveAll });

        const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
        expect(payload.continuePendingWork).toBeUndefined();
        spy.mockRestore();
    });

    it("forwards memory configuration in session.create request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create")
                    return { sessionId: params.sessionId ?? "session-id" };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.createSession({
            onPermissionRequest: approveAll,
            memory: { enabled: true },
        });

        const payload = spy.mock.calls.find(([method]) => method === "session.create")![1] as any;
        expect(payload.memory).toEqual({ enabled: true });
        spy.mockRestore();
    });

    it("forwards memory configuration in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            memory: { enabled: false },
        });

        const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
        expect(payload.memory).toEqual({ enabled: false });
        spy.mockRestore();
    });

    it("omits memory from session.create payload when not specified", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create")
                    return { sessionId: params.sessionId ?? "session-id" };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.createSession({ onPermissionRequest: approveAll });

        const payload = spy.mock.calls.find(([method]) => method === "session.create")![1] as any;
        const serialized = JSON.parse(JSON.stringify(payload));
        expect(serialized).not.toHaveProperty("memory");
        spy.mockRestore();
    });

    it("forwards provider headers in session.create request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.create")
                    return { sessionId: params.sessionId ?? "session-id" };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.createSession({
            onPermissionRequest: approveAll,
            provider: {
                baseUrl: "https://example.com/provider",
                headers: { Authorization: "Bearer provider-token" },
                modelId: "gpt-4o",
                wireModel: "my-finetune-v3",
                maxPromptTokens: 100_000,
                maxOutputTokens: 4096,
                transport: "websockets",
            },
        });

        const payload = spy.mock.calls.find(([method]) => method === "session.create")![1] as any;
        expect(payload.provider).toEqual(
            expect.objectContaining({
                baseUrl: "https://example.com/provider",
                headers: { Authorization: "Bearer provider-token" },
                modelId: "gpt-4o",
                wireModel: "my-finetune-v3",
                maxPromptTokens: 100_000,
                maxOutputTokens: 4096,
                transport: "websockets",
            })
        );
        spy.mockRestore();
    });

    it("forwards provider headers in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            provider: {
                baseUrl: "https://example.com/provider",
                headers: { Authorization: "Bearer resume-token" },
                modelId: "gpt-4o",
                wireModel: "my-finetune-v3",
                maxPromptTokens: 100_000,
                maxOutputTokens: 4096,
                transport: "websockets",
            },
        });

        const payload = spy.mock.calls.find(([method]) => method === "session.resume")![1] as any;
        expect(payload.provider).toEqual(
            expect.objectContaining({
                baseUrl: "https://example.com/provider",
                headers: { Authorization: "Bearer resume-token" },
                modelId: "gpt-4o",
                wireModel: "my-finetune-v3",
                maxPromptTokens: 100_000,
                maxOutputTokens: 4096,
                transport: "websockets",
            })
        );
        spy.mockRestore();
    });

    it("forwards defaultAgent in session.create request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({
            defaultAgent: { excludedTools: ["heavy-tool"] },
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.create",
            expect.objectContaining({
                defaultAgent: { excludedTools: ["heavy-tool"] },
            })
        );
    });

    it("forwards defaultAgent in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.resumeSession(session.sessionId, {
            defaultAgent: { excludedTools: ["heavy-tool"] },
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.resume",
            expect.objectContaining({
                defaultAgent: { excludedTools: ["heavy-tool"] },
            })
        );
    });

    it("forwards instructionDirectories in session.create request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const instructionDirectories = ["C:\\extra-instructions", "C:\\more-instructions"];
        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({
            instructionDirectories,
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.create",
            expect.objectContaining({ instructionDirectories })
        );
    });

    it("forwards instructionDirectories in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const instructionDirectories = ["C:\\resume-instructions"];
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });
        await client.resumeSession(session.sessionId, {
            instructionDirectories,
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.resume",
            expect.objectContaining({
                instructionDirectories,
                sessionId: session.sessionId,
            })
        );
        spy.mockRestore();
    });

    it("does not request permissions on session.resume when using the default joinSession handler", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.resumeSession(session.sessionId, {
            onPermissionRequest: defaultJoinSessionPermissionHandler,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.resume",
            expect.objectContaining({
                sessionId: session.sessionId,
                requestPermission: false,
            })
        );
        spy.mockRestore();
    });

    it("requests permissions on session.resume when using an explicit handler", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
        });

        expect(spy).toHaveBeenCalledWith(
            "session.resume",
            expect.objectContaining({
                sessionId: session.sessionId,
                requestPermission: true,
            })
        );
        spy.mockRestore();
    });

    it("forwards mode callback request flags in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, params: any) => {
                if (method === "session.resume") return { sessionId: params.sessionId };
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            onExitPlanModeRequest: () => ({ approved: true }),
            onAutoModeSwitchRequest: () => "yes",
        });

        expect(spy).toHaveBeenCalledWith(
            "session.resume",
            expect.objectContaining({
                sessionId: session.sessionId,
                requestExitPlanMode: true,
                requestAutoModeSwitch: true,
            })
        );
        spy.mockRestore();
    });

    it("sends session.model.switchTo RPC with correct params", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });

        // Mock sendRequest to capture the call without hitting the runtime
        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, _params: any) => {
                if (method === "session.model.switchTo") return {};
                // Fall through for other methods (shouldn't be called)
                throw new Error(`Unexpected method: ${method}`);
            });

        await session.setModel("gpt-4.1");

        expect(spy).toHaveBeenCalledWith("session.model.switchTo", {
            sessionId: session.sessionId,
            modelId: "gpt-4.1",
        });

        spy.mockRestore();
    });

    it("sends reasoning options with session.model.switchTo when provided", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const session = await client.createSession({ onPermissionRequest: approveAll });

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, _params: any) => {
                if (method === "session.model.switchTo") return {};
                throw new Error(`Unexpected method: ${method}`);
            });

        await session.setModel("claude-sonnet-4.6", {
            reasoningEffort: "high",
            reasoningSummary: "detailed",
            contextTier: "long_context",
        });

        expect(spy).toHaveBeenCalledWith("session.model.switchTo", {
            sessionId: session.sessionId,
            modelId: "claude-sonnet-4.6",
            reasoningEffort: "high",
            reasoningSummary: "detailed",
            contextTier: "long_context",
        });

        spy.mockRestore();
    });

    describe("URL parsing", () => {
        it("should parse port-only URL format", () => {
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri("8080"),
                logLevel: "error",
            });

            expect((client as any).runtimePort).toBe(8080);
            expect((client as any).actualHost).toBe("localhost");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should parse host:port URL format", () => {
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri("127.0.0.1:9000"),
                logLevel: "error",
            });

            expect((client as any).runtimePort).toBe(9000);
            expect((client as any).actualHost).toBe("127.0.0.1");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should parse http://host:port URL format", () => {
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri("http://localhost:7000"),
                logLevel: "error",
            });

            expect((client as any).runtimePort).toBe(7000);
            expect((client as any).actualHost).toBe("localhost");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should parse https://host:port URL format", () => {
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri("https://example.com:443"),
                logLevel: "error",
            });

            expect((client as any).runtimePort).toBe(443);
            expect((client as any).actualHost).toBe("example.com");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should throw error for invalid URL format", () => {
            expect(() => {
                new CopilotClient({
                    connection: RuntimeConnection.forUri("invalid-url"),
                    logLevel: "error",
                });
            }).toThrow(/Invalid cliUrl format/);
        });

        it("should throw error for invalid port - too high", () => {
            expect(() => {
                new CopilotClient({
                    connection: RuntimeConnection.forUri("localhost:99999"),
                    logLevel: "error",
                });
            }).toThrow(/Invalid port in cliUrl/);
        });

        it("should throw error for invalid port - zero", () => {
            expect(() => {
                new CopilotClient({
                    connection: RuntimeConnection.forUri("localhost:0"),
                    logLevel: "error",
                });
            }).toThrow(/Invalid port in cliUrl/);
        });

        it("should throw error for invalid port - negative", () => {
            expect(() => {
                new CopilotClient({
                    connection: RuntimeConnection.forUri("localhost:-1"),
                    logLevel: "error",
                });
            }).toThrow(/Invalid port in cliUrl/);
        });

        it("should mark client as using external server", () => {
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri("localhost:8080"),
                logLevel: "error",
            });

            expect((client as any).isExternalServer).toBe(true);
        });

        it("should not resolve a CLI path when forUri is used", () => {
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri("localhost:8080"),
                logLevel: "error",
            });

            expect((client as any).resolvedCliPath).toBeUndefined();
        });
    });

    describe("SessionFs config", () => {
        it("throws when initialCwd is missing", () => {
            expect(() => {
                new CopilotClient({
                    sessionFs: {
                        initialCwd: "",
                        sessionStatePath: "/session-state",
                        conventions: "posix",
                    },
                    logLevel: "error",
                });
            }).toThrow(/sessionFs\.initialCwd is required/);
        });

        it("throws when sessionStatePath is missing", () => {
            expect(() => {
                new CopilotClient({
                    sessionFs: {
                        initialCwd: "/",
                        sessionStatePath: "",
                        conventions: "posix",
                    },
                    logLevel: "error",
                });
            }).toThrow(/sessionFs\.sessionStatePath is required/);
        });
    });

    describe("Auth options", () => {
        it("should accept gitHubToken option", () => {
            const client = new CopilotClient({
                gitHubToken: "gho_test_token",
                logLevel: "error",
            });

            expect((client as any).options.gitHubToken).toBe("gho_test_token");
        });

        it("should default useLoggedInUser to true when no gitHubToken", () => {
            const client = new CopilotClient({
                logLevel: "error",
            });

            expect((client as any).options.useLoggedInUser).toBe(true);
        });

        it("should default useLoggedInUser to false when gitHubToken is provided", () => {
            const client = new CopilotClient({
                gitHubToken: "gho_test_token",
                logLevel: "error",
            });

            expect((client as any).options.useLoggedInUser).toBe(false);
        });

        it("should allow explicit useLoggedInUser: true with gitHubToken", () => {
            const client = new CopilotClient({
                gitHubToken: "gho_test_token",
                useLoggedInUser: true,
                logLevel: "error",
            });

            expect((client as any).options.useLoggedInUser).toBe(true);
        });

        it("should allow explicit useLoggedInUser: false without gitHubToken", () => {
            const client = new CopilotClient({
                useLoggedInUser: false,
                logLevel: "error",
            });

            expect((client as any).options.useLoggedInUser).toBe(false);
        });

        it("should accept baseDirectory option", () => {
            const client = new CopilotClient({
                baseDirectory: "/custom/copilot/home",
                logLevel: "error",
            });

            expect((client as any).options.baseDirectory).toBe("/custom/copilot/home");
        });

        it("should leave baseDirectory undefined when not provided", () => {
            const client = new CopilotClient({
                logLevel: "error",
            });

            expect((client as any).options.baseDirectory).toBeUndefined();
        });

        it("should throw error when gitHubToken is used with forUri", () => {
            expect(() => {
                new CopilotClient({
                    connection: RuntimeConnection.forUri("localhost:8080"),
                    gitHubToken: "gho_test_token",
                    logLevel: "error",
                });
            }).toThrow(
                /gitHubToken and useLoggedInUser cannot be used with RuntimeConnection.forUri/
            );
        });

        it("should throw error when useLoggedInUser is used with forUri", () => {
            expect(() => {
                new CopilotClient({
                    connection: RuntimeConnection.forUri("localhost:8080"),
                    useLoggedInUser: false,
                    logLevel: "error",
                });
            }).toThrow(
                /gitHubToken and useLoggedInUser cannot be used with RuntimeConnection.forUri/
            );
        });
    });

    describe("overridesBuiltInTool in tool definitions", () => {
        it("sends overridesBuiltInTool in tool definition on session.create", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({
                onPermissionRequest: approveAll,
                tools: [
                    {
                        name: "grep",
                        description: "custom grep",
                        handler: async () => "ok",
                        overridesBuiltInTool: true,
                    },
                ],
            });

            const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
            expect(payload.tools).toEqual([
                expect.objectContaining({ name: "grep", overridesBuiltInTool: true }),
            ]);
        });

        it("sends overridesBuiltInTool in tool definition on session.resume", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({ onPermissionRequest: approveAll });
            // Mock sendRequest to capture the call without hitting the runtime
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.resume") return { sessionId: params.sessionId };
                    throw new Error(`Unexpected method: ${method}`);
                });
            await client.resumeSession(session.sessionId, {
                onPermissionRequest: approveAll,
                tools: [
                    {
                        name: "grep",
                        description: "custom grep",
                        handler: async () => "ok",
                        overridesBuiltInTool: true,
                    },
                ],
            });

            const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
            expect(payload.tools).toEqual([
                expect.objectContaining({ name: "grep", overridesBuiltInTool: true }),
            ]);
            spy.mockRestore();
        });
    });

    describe("defer in tool definitions", () => {
        it("sends defer in tool definition on session.create", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({
                onPermissionRequest: approveAll,
                tools: [
                    {
                        name: "lookup_issue",
                        description: "Fetch issue details",
                        handler: async () => "ok",
                        defer: "auto",
                    },
                ],
            });

            const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
            expect(payload.tools).toEqual([
                expect.objectContaining({ name: "lookup_issue", defer: "auto" }),
            ]);
        });

        it("sends defer in tool definition on session.resume", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.resume") return { sessionId: params.sessionId };
                    throw new Error(`Unexpected method: ${method}`);
                });
            await client.resumeSession(session.sessionId, {
                onPermissionRequest: approveAll,
                tools: [
                    {
                        name: "lookup_issue",
                        description: "Fetch issue details",
                        handler: async () => "ok",
                        defer: "auto",
                    },
                ],
            });

            const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
            expect(payload.tools).toEqual([
                expect.objectContaining({ name: "lookup_issue", defer: "auto" }),
            ]);
            spy.mockRestore();
        });
    });

    describe("agent parameter in session creation", () => {
        it("forwards agent in session.create request", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({
                onPermissionRequest: approveAll,
                customAgents: [
                    {
                        name: "test-agent",
                        prompt: "You are a test agent.",
                    },
                ],
                agent: "test-agent",
            });

            const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
            expect(payload.agent).toBe("test-agent");
            expect(payload.customAgents).toEqual([expect.objectContaining({ name: "test-agent" })]);
        });

        it("forwards custom agent model in session.create request", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({
                onPermissionRequest: approveAll,
                customAgents: [
                    {
                        name: "model-agent",
                        prompt: "You are a model agent.",
                        model: "claude-haiku-4.5",
                    },
                ],
            });

            const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
            expect(payload.customAgents).toEqual([
                expect.objectContaining({ name: "model-agent", model: "claude-haiku-4.5" }),
            ]);
        });

        it("forwards agent in session.resume request", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.resume") return { sessionId: params.sessionId };
                    throw new Error(`Unexpected method: ${method}`);
                });
            await client.resumeSession(session.sessionId, {
                onPermissionRequest: approveAll,
                customAgents: [
                    {
                        name: "test-agent",
                        prompt: "You are a test agent.",
                    },
                ],
                agent: "test-agent",
            });

            const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
            expect(payload.agent).toBe("test-agent");
            spy.mockRestore();
        });
    });

    describe("onListModels", () => {
        it("calls onListModels handler instead of RPC when provided", async () => {
            const customModels: ModelInfo[] = [
                {
                    id: "my-custom-model",
                    name: "My Custom Model",
                    capabilities: {
                        supports: { vision: false, reasoningEffort: false },
                        limits: { max_context_window_tokens: 128000 },
                    },
                    billing: {
                        multiplier: 1.5,
                        tokenPrices: {
                            inputPrice: 2.0,
                            outputPrice: 8.0,
                            cachePrice: 0.5,
                            batchSize: 1000000,
                            contextMax: 128000,
                            longContext: {
                                inputPrice: 4.0,
                                outputPrice: 16.0,
                                cachePrice: 1.0,
                                contextMax: 1000000,
                            },
                        },
                    },
                },
            ];

            const handler = vi.fn().mockReturnValue(customModels);
            const client = new CopilotClient({ onListModels: handler });
            await client.start();
            onTestFinished(() => stopClient(client));

            const models = await client.listModels();
            expect(handler).toHaveBeenCalledTimes(1);
            expect(models).toEqual(customModels);
            expect(models[0].billing?.tokenPrices?.longContext?.contextMax).toBe(1000000);
        });

        it("caches onListModels results on subsequent calls", async () => {
            const customModels: ModelInfo[] = [
                {
                    id: "cached-model",
                    name: "Cached Model",
                    capabilities: {
                        supports: { vision: false, reasoningEffort: false },
                        limits: { max_context_window_tokens: 128000 },
                    },
                },
            ];

            const handler = vi.fn().mockReturnValue(customModels);
            const client = new CopilotClient({ onListModels: handler });
            await client.start();
            onTestFinished(() => stopClient(client));

            await client.listModels();
            await client.listModels();
            expect(handler).toHaveBeenCalledTimes(1); // Only called once due to caching
        });

        it("supports async onListModels handler", async () => {
            const customModels: ModelInfo[] = [
                {
                    id: "async-model",
                    name: "Async Model",
                    capabilities: {
                        supports: { vision: false, reasoningEffort: false },
                        limits: { max_context_window_tokens: 128000 },
                    },
                },
            ];

            const handler = vi.fn().mockResolvedValue(customModels);
            const client = new CopilotClient({ onListModels: handler });
            await client.start();
            onTestFinished(() => stopClient(client));

            const models = await client.listModels();
            expect(models).toEqual(customModels);
        });

        it("does not require client.start when onListModels is provided", async () => {
            const customModels: ModelInfo[] = [
                {
                    id: "no-start-model",
                    name: "No Start Model",
                    capabilities: {
                        supports: { vision: false, reasoningEffort: false },
                        limits: { max_context_window_tokens: 128000 },
                    },
                },
            ];

            const handler = vi.fn().mockReturnValue(customModels);
            const client = new CopilotClient({ onListModels: handler });

            const models = await client.listModels();
            expect(handler).toHaveBeenCalledTimes(1);
            expect(models).toEqual(customModels);
        });
    });

    describe("unexpected disconnection", () => {
        // No child process exists over the in-process (FFI) transport, so this
        // child-process-kill scenario does not apply there. Covered by the default
        // (stdio) cell.
        it.skipIf((process.env.COPILOT_SDK_DEFAULT_CONNECTION ?? "").toLowerCase() === "inprocess")(
            "transitions to disconnected when child process is killed",
            async () => {
                const client = new CopilotClient();
                await client.start();
                onTestFinished(() => stopClient(client));

                expect((client as any).state).toBe("connected");

                // Kill the child process to simulate unexpected termination
                const proc = (client as any)
                    .cliProcess as import("node:child_process").ChildProcess;
                proc.kill();

                // Wait for the connection.onClose handler to fire
                await vi.waitFor(() => {
                    expect((client as any).state).toBe("disconnected");
                });
            }
        );
    });

    describe("onGetTraceContext", () => {
        it("includes trace context from callback in session.create request", async () => {
            const traceContext = {
                traceparent: "00-abcdef1234567890abcdef1234567890-1234567890abcdef-01",
                tracestate: "vendor=opaque",
            };
            const provider = vi.fn().mockReturnValue(traceContext);
            const client = new CopilotClient({ onGetTraceContext: provider });
            await client.start();
            onTestFinished(() => stopClient(client));

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({ onPermissionRequest: approveAll });

            expect(provider).toHaveBeenCalled();
            expect(spy).toHaveBeenCalledWith(
                "session.create",
                expect.objectContaining({
                    traceparent: "00-abcdef1234567890abcdef1234567890-1234567890abcdef-01",
                    tracestate: "vendor=opaque",
                })
            );
        });

        it("includes trace context from callback in session.resume request", async () => {
            const traceContext = {
                traceparent: "00-abcdef1234567890abcdef1234567890-1234567890abcdef-01",
            };
            const provider = vi.fn().mockReturnValue(traceContext);
            const client = new CopilotClient({ onGetTraceContext: provider });
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.resume") return { sessionId: params.sessionId };
                    throw new Error(`Unexpected method: ${method}`);
                });
            await client.resumeSession(session.sessionId, { onPermissionRequest: approveAll });

            expect(spy).toHaveBeenCalledWith(
                "session.resume",
                expect.objectContaining({
                    traceparent: "00-abcdef1234567890abcdef1234567890-1234567890abcdef-01",
                })
            );
        });

        it("includes trace context from callback in session.send request", async () => {
            const traceContext = {
                traceparent: "00-fedcba0987654321fedcba0987654321-abcdef1234567890-01",
            };
            const provider = vi.fn().mockReturnValue(traceContext);
            const client = new CopilotClient({ onGetTraceContext: provider });
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string) => {
                    if (method === "session.send") return { responseId: "r1" };
                    throw new Error(`Unexpected method: ${method}`);
                });
            await session.send({ prompt: "hello" });

            expect(spy).toHaveBeenCalledWith(
                "session.send",
                expect.objectContaining({
                    traceparent: "00-fedcba0987654321fedcba0987654321-abcdef1234567890-01",
                })
            );
        });

        it("forwards requestHeaders in session.send request", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string) => {
                    if (method === "session.send") return { messageId: "m1" };
                    throw new Error(`Unexpected method: ${method}`);
                });

            await session.send({
                prompt: "hello",
                requestHeaders: { Authorization: "Bearer turn-token" },
            });

            expect(spy).toHaveBeenCalledWith(
                "session.send",
                expect.objectContaining({
                    prompt: "hello",
                    requestHeaders: { Authorization: "Bearer turn-token" },
                })
            );
        });

        it("does not include trace context when no callback is provided", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({ onPermissionRequest: approveAll });

            const [, params] = spy.mock.calls.find(([method]) => method === "session.create")!;
            expect(params.traceparent).toBeUndefined();
            expect(params.tracestate).toBeUndefined();
        });
    });

    describe("commands", () => {
        it("forwards commands in session.create RPC", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({
                onPermissionRequest: approveAll,
                commands: [
                    { name: "deploy", description: "Deploy the app", handler: async () => {} },
                    { name: "rollback", handler: async () => {} },
                ],
            });

            const payload = spy.mock.calls.find((c) => c[0] === "session.create")![1] as any;
            expect(payload.commands).toEqual([
                { name: "deploy", description: "Deploy the app" },
                { name: "rollback", description: undefined },
            ]);
        });

        it("forwards commands in session.resume RPC", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.resume") return { sessionId: params.sessionId };
                    throw new Error(`Unexpected method: ${method}`);
                });
            await client.resumeSession(session.sessionId, {
                onPermissionRequest: approveAll,
                commands: [{ name: "deploy", description: "Deploy", handler: async () => {} }],
            });

            const payload = spy.mock.calls.find((c) => c[0] === "session.resume")![1] as any;
            expect(payload.commands).toEqual([{ name: "deploy", description: "Deploy" }]);
            spy.mockRestore();
        });

        it("routes command.execute event to the correct handler", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const handler = vi.fn();
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                commands: [{ name: "deploy", handler }],
            });

            // Mock the RPC response so handlePendingCommand doesn't fail
            const rpcSpy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string) => {
                    if (method === "session.commands.handlePendingCommand")
                        return { success: true };
                    throw new Error(`Unexpected method: ${method}`);
                });

            // Simulate a command.execute event
            (session as any)._dispatchEvent({
                id: "evt-1",
                timestamp: new Date().toISOString(),
                parentId: null,
                ephemeral: true,
                type: "command.execute",
                data: {
                    requestId: "req-1",
                    command: "/deploy production",
                    commandName: "deploy",
                    args: "production",
                },
            });

            // Wait for the async handler to complete
            await vi.waitFor(() => expect(handler).toHaveBeenCalledTimes(1));
            expect(handler).toHaveBeenCalledWith(
                expect.objectContaining({
                    sessionId: session.sessionId,
                    command: "/deploy production",
                    commandName: "deploy",
                    args: "production",
                })
            );

            // Verify handlePendingCommand was called with the requestId
            expect(rpcSpy).toHaveBeenCalledWith(
                "session.commands.handlePendingCommand",
                expect.objectContaining({ requestId: "req-1" })
            );
            rpcSpy.mockRestore();
        });

        it("sends error when command handler throws", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                commands: [
                    {
                        name: "fail",
                        handler: () => {
                            throw new Error("deploy failed");
                        },
                    },
                ],
            });

            const rpcSpy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string) => {
                    if (method === "session.commands.handlePendingCommand")
                        return { success: true };
                    throw new Error(`Unexpected method: ${method}`);
                });

            (session as any)._dispatchEvent({
                id: "evt-2",
                timestamp: new Date().toISOString(),
                parentId: null,
                ephemeral: true,
                type: "command.execute",
                data: {
                    requestId: "req-2",
                    command: "/fail",
                    commandName: "fail",
                    args: "",
                },
            });

            await vi.waitFor(() =>
                expect(rpcSpy).toHaveBeenCalledWith(
                    "session.commands.handlePendingCommand",
                    expect.objectContaining({ requestId: "req-2", error: "deploy failed" })
                )
            );
            rpcSpy.mockRestore();
        });

        it("sends error for unknown command", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                commands: [{ name: "deploy", handler: async () => {} }],
            });

            const rpcSpy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string) => {
                    if (method === "session.commands.handlePendingCommand")
                        return { success: true };
                    throw new Error(`Unexpected method: ${method}`);
                });

            (session as any)._dispatchEvent({
                id: "evt-3",
                timestamp: new Date().toISOString(),
                parentId: null,
                ephemeral: true,
                type: "command.execute",
                data: {
                    requestId: "req-3",
                    command: "/unknown",
                    commandName: "unknown",
                    args: "",
                },
            });

            await vi.waitFor(() =>
                expect(rpcSpy).toHaveBeenCalledWith(
                    "session.commands.handlePendingCommand",
                    expect.objectContaining({
                        requestId: "req-3",
                        error: expect.stringContaining("Unknown command"),
                    })
                )
            );
            rpcSpy.mockRestore();
        });
    });

    describe("ui elicitation", () => {
        it("reads capabilities from session.create response", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            // Intercept session.create to inject capabilities
            const origSendRequest = (client as any).connection!.sendRequest.bind(
                (client as any).connection
            );
            vi.spyOn((client as any).connection!, "sendRequest").mockImplementation(
                async (method: string, params: any) => {
                    if (method === "session.create") {
                        const result = await origSendRequest(method, params);
                        return {
                            ...result,
                            capabilities: { ui: { elicitation: true } },
                        };
                    }
                    return origSendRequest(method, params);
                }
            );

            const session = await client.createSession({ onPermissionRequest: approveAll });
            expect(session.capabilities).toEqual({ ui: { elicitation: true } });
        });

        it("defaults capabilities when not injected", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({ onPermissionRequest: approveAll });
            // CLI returns actual capabilities (elicitation false in headless mode)
            expect(session.capabilities.ui?.elicitation).toBe(false);
        });

        it("elicitation throws when capability is missing", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({ onPermissionRequest: approveAll });

            await expect(
                session.ui.elicitation({
                    message: "Enter name",
                    requestedSchema: {
                        type: "object",
                        properties: { name: { type: "string", minLength: 1 } },
                        required: ["name"],
                    },
                })
            ).rejects.toThrow(/not supported/);
        });

        it("sends requestElicitation flag when onElicitationRequest is provided", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const rpcSpy = vi.spyOn((client as any).connection!, "sendRequest");

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                onElicitationRequest: async () => ({
                    action: "accept" as const,
                    content: {},
                }),
            });
            expect(session).toBeDefined();

            const createCall = rpcSpy.mock.calls.find((c) => c[0] === "session.create");
            expect(createCall).toBeDefined();
            expect(createCall![1]).toEqual(
                expect.objectContaining({
                    requestElicitation: true,
                })
            );
            rpcSpy.mockRestore();
        });

        it("does not send requestElicitation when no handler provided", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const rpcSpy = vi.spyOn((client as any).connection!, "sendRequest");

            const session = await client.createSession({
                onPermissionRequest: approveAll,
            });
            expect(session).toBeDefined();

            const createCall = rpcSpy.mock.calls.find((c) => c[0] === "session.create");
            expect(createCall).toBeDefined();
            expect(createCall![1]).toEqual(
                expect.objectContaining({
                    requestElicitation: false,
                })
            );
            rpcSpy.mockRestore();
        });

        it("sends mode callback request flags based on handler presence", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const rpcSpy = vi.spyOn((client as any).connection!, "sendRequest");

            await client.createSession({
                onPermissionRequest: approveAll,
                onExitPlanModeRequest: () => ({ approved: true }),
                onAutoModeSwitchRequest: () => "yes_always",
            });

            const createCallWithHandlers = rpcSpy.mock.calls.find((c) => c[0] === "session.create");
            expect(createCallWithHandlers![1]).toEqual(
                expect.objectContaining({
                    requestExitPlanMode: true,
                    requestAutoModeSwitch: true,
                })
            );

            rpcSpy.mockClear();
            await client.createSession({ onPermissionRequest: approveAll });
            const createCallWithoutHandlers = rpcSpy.mock.calls.find(
                (c) => c[0] === "session.create"
            );
            expect(createCallWithoutHandlers![1]).toEqual(
                expect.objectContaining({
                    requestExitPlanMode: false,
                    requestAutoModeSwitch: false,
                })
            );
            rpcSpy.mockRestore();
        });

        it("dispatches mode callback requests to registered handlers", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                onExitPlanModeRequest: (request, invocation) => {
                    expect(invocation.sessionId).toBeDefined();
                    expect(request.summary).toBe("Review the plan");
                    expect(request.planContent).toBe("Plan body");
                    expect(request.actions).toEqual(["interactive", "autopilot"]);
                    expect(request.recommendedAction).toBe("autopilot");
                    return {
                        approved: true,
                        selectedAction: "interactive",
                        feedback: "Looks good",
                    };
                },
                onAutoModeSwitchRequest: (request, invocation) => {
                    expect(invocation.sessionId).toBeDefined();
                    expect(request.errorCode).toBe("user_weekly_rate_limited");
                    expect(request.retryAfterSeconds).toBe(3600);
                    return "yes_always";
                },
            });

            const exitResult = await (client as any).handleExitPlanModeRequest({
                sessionId: session.sessionId,
                summary: "Review the plan",
                planContent: "Plan body",
                actions: ["interactive", "autopilot"],
                recommendedAction: "autopilot",
            });
            expect(exitResult).toEqual({
                approved: true,
                selectedAction: "interactive",
                feedback: "Looks good",
            });

            const autoResult = await (client as any).handleAutoModeSwitchRequest({
                sessionId: session.sessionId,
                errorCode: "user_weekly_rate_limited",
                retryAfterSeconds: 3600,
            });
            expect(autoResult).toEqual({ response: "yes_always" });
        });

        it("sends cancel when elicitation handler throws", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                onElicitationRequest: async () => {
                    throw new Error("handler exploded");
                },
            });

            const rpcSpy = vi.spyOn((client as any).connection!, "sendRequest");

            await session._handleElicitationRequest(
                { sessionId: session.sessionId, message: "Pick a color" },
                "req-123"
            );

            const cancelCall = rpcSpy.mock.calls.find(
                (c) =>
                    c[0] === "session.ui.handlePendingElicitation" &&
                    (c[1] as any)?.result?.action === "cancel"
            );
            expect(cancelCall).toBeDefined();
            expect(cancelCall![1]).toEqual(
                expect.objectContaining({
                    requestId: "req-123",
                    result: { action: "cancel" },
                })
            );
            rpcSpy.mockRestore();
        });
    });

    describe("sessionIdleTimeoutSeconds", () => {
        it("should default to 0 when not specified", () => {
            const client = new CopilotClient({
                logLevel: "error",
            });

            expect((client as any).options.sessionIdleTimeoutSeconds).toBe(0);
        });

        it("should store a custom value", () => {
            const client = new CopilotClient({
                sessionIdleTimeoutSeconds: 600,
                logLevel: "error",
            });

            expect((client as any).options.sessionIdleTimeoutSeconds).toBe(600);
        });
    });

    describe("hooks dispatcher", () => {
        // Direct unit tests for CopilotSession._handleHooksInvoke. The hook
        // dispatch logic maps the CLI-emitted hook type (string) to the
        // corresponding SessionHooks handler. These tests guard against
        // regressions like the one fixed for postToolUseFailure (issue #1220).

        it("dispatches postToolUseFailure to onPostToolUseFailure handler", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const received: { input: any; invocation: any }[] = [];
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                hooks: {
                    onPostToolUseFailure: async (input, invocation) => {
                        received.push({ input, invocation });
                        return { additionalContext: "failure observed" };
                    },
                },
            });

            const failureInput = {
                toolName: "failing-tool",
                toolArgs: { foo: "bar" },
                error: "exit 1",
                timestamp: 1234,
                cwd: "/tmp",
            };
            const expectedInput = {
                toolName: "failing-tool",
                toolArgs: { foo: "bar" },
                error: "exit 1",
                timestamp: new Date(1234),
                workingDirectory: "/tmp",
            };
            const result = await (session as any)._handleHooksInvoke(
                "postToolUseFailure",
                failureInput
            );

            expect(received).toHaveLength(1);
            expect(received[0].input).toEqual(expectedInput);
            expect(received[0].invocation.sessionId).toBe(session.sessionId);
            expect(result).toEqual({ additionalContext: "failure observed" });
        });

        it("does not fall back to onPostToolUse for postToolUseFailure events", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const postUseCalls: string[] = [];
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                hooks: {
                    // Only onPostToolUse registered; postToolUseFailure events
                    // must not be routed here.
                    onPostToolUse: async (input) => {
                        postUseCalls.push(input.toolName);
                    },
                },
            });

            const result = await (session as any)._handleHooksInvoke("postToolUseFailure", {
                toolName: "failing-tool",
                toolArgs: {},
                error: "boom",
                timestamp: 0,
                cwd: "/tmp",
            });

            expect(postUseCalls).toHaveLength(0);
            expect(result).toBeUndefined();
        });

        it("dispatches postToolUse and postToolUseFailure to their respective handlers", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const postCalls: string[] = [];
            const failureCalls: string[] = [];
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                hooks: {
                    onPostToolUse: async (input) => {
                        postCalls.push(input.toolName);
                    },
                    onPostToolUseFailure: async (input) => {
                        failureCalls.push(input.toolName);
                    },
                },
            });

            await (session as any)._handleHooksInvoke("postToolUse", {
                toolName: "success-tool",
                toolArgs: {},
                toolResult: {
                    textResultForLlm: "ok",
                    resultType: "success" as const,
                },
                timestamp: 0,
                cwd: "/tmp",
            });
            await (session as any)._handleHooksInvoke("postToolUseFailure", {
                toolName: "fail-tool",
                toolArgs: {},
                error: "bad",
                timestamp: 0,
                cwd: "/tmp",
            });

            expect(postCalls).toEqual(["success-tool"]);
            expect(failureCalls).toEqual(["fail-tool"]);
        });

        it("routes hooks.invoke JSON-RPC requests to the SessionHooks handler", async () => {
            // Validates the full JSON-RPC entry point used by the CLI:
            // CopilotClient.handleHooksInvoke({sessionId, hookType, input})
            // → CopilotSession._handleHooksInvoke(hookType, input)
            // → SessionHooks.onPostToolUseFailure(normalizedInput, {sessionId})
            //
            // This guards the wire-format contract that the bundled Copilot
            // CLI relies on: the hookType string "postToolUseFailure" and the
            // input shape `{toolName, toolArgs, error, timestamp, cwd}`.
            // The SDK maps that to public `{..., timestamp: Date, workingDirectory}`.
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => stopClient(client));

            const received: { input: any; invocation: any }[] = [];
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                hooks: {
                    onPostToolUseFailure: async (input, invocation) => {
                        received.push({ input, invocation });
                        return { additionalContext: "context from failure hook" };
                    },
                },
            });

            const failureInput = {
                toolName: "shell",
                toolArgs: { command: "false" },
                error: "exit 1",
                timestamp: 1700000000000,
                cwd: "/tmp",
            };

            const response = await (client as any).handleHooksInvoke({
                sessionId: session.sessionId,
                hookType: "postToolUseFailure",
                input: failureInput,
            });

            expect(received).toHaveLength(1);
            expect(received[0].input).toEqual({
                toolName: "shell",
                toolArgs: { command: "false" },
                error: "exit 1",
                timestamp: new Date(1700000000000),
                workingDirectory: "/tmp",
            });
            expect(received[0].invocation.sessionId).toBe(session.sessionId);
            // The CLI only consumes output.additionalContext; the SDK returns
            // it wrapped in `{ output }` per the JSON-RPC contract.
            expect(response).toEqual({
                output: { additionalContext: "context from failure hook" },
            });
        });
    });

    describe("shutdown", () => {
        it("requests runtime shutdown when stopping an SDK-owned process", async () => {
            const client = new CopilotClient();
            const calls: string[] = [];
            const child = new EventEmitter() as EventEmitter & {
                exitCode: number | null;
                signalCode: string | null;
                kill: ReturnType<typeof vi.fn>;
            };
            child.exitCode = null;
            child.signalCode = null;
            child.kill = vi.fn(() => {
                calls.push("kill");
                child.signalCode = "SIGTERM";
                child.emit("exit", null, "SIGTERM");
                return true;
            });

            (client as any).connection = {
                sendRequest: vi.fn(async (method: string) => {
                    calls.push(method);
                    if (method === "runtime.shutdown") {
                        child.exitCode = 0;
                        child.emit("exit", 0, null);
                        return {};
                    }
                    throw new Error(`unexpected method ${method}`);
                }),
                dispose: vi.fn(() => calls.push("dispose")),
            };
            (client as any).cliProcess = child;
            (client as any).isExternalServer = false;

            await expect(client.stop()).resolves.toEqual([]);
            expect(calls).toEqual(["runtime.shutdown", "dispose"]);
            expect(child.kill).not.toHaveBeenCalled();
        });

        it("does not request runtime shutdown for force stop or external runtimes", async () => {
            const forceClient = new CopilotClient();
            const forceChild = new EventEmitter() as EventEmitter & {
                exitCode: number | null;
                signalCode: string | null;
                kill: ReturnType<typeof vi.fn>;
            };
            forceChild.exitCode = null;
            forceChild.signalCode = null;
            forceChild.kill = vi.fn(() => true);
            const forceSendRequest = vi.fn();
            (forceClient as any).connection = {
                sendRequest: forceSendRequest,
                dispose: vi.fn(),
            };
            (forceClient as any).cliProcess = forceChild;
            (forceClient as any).isExternalServer = false;

            await forceClient.forceStop();
            expect(forceSendRequest).not.toHaveBeenCalled();
            expect(forceChild.kill).toHaveBeenCalledWith("SIGKILL");

            const externalClient = new CopilotClient();
            const externalSendRequest = vi.fn();
            (externalClient as any).connection = {
                sendRequest: externalSendRequest,
                dispose: vi.fn(),
            };
            (externalClient as any).isExternalServer = true;

            await expect(externalClient.stop()).resolves.toEqual([]);
            expect(externalSendRequest).not.toHaveBeenCalled();
        });
    });
});
