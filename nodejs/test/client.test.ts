/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, expect, it, onTestFinished, vi } from "vitest";
import { approveAll, CopilotClient, type ModelInfo } from "../src/index.js";

// This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.test.ts instead

describe("CopilotClient", () => {
    it("throws when createSession is called without onPermissionRequest", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => client.forceStop());

        await expect((client as any).createSession({})).rejects.toThrow(
            /onPermissionRequest.*is required/
        );
    });

    it("throws when resumeSession is called without onPermissionRequest", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => client.forceStop());

        const session = await client.createSession({ onPermissionRequest: approveAll });
        await expect((client as any).resumeSession(session.sessionId, {})).rejects.toThrow(
            /onPermissionRequest.*is required/
        );
    });

    it("does not respond to v3 permission requests when handler returns no-result", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => client.forceStop());

        const session = await client.createSession({
            onPermissionRequest: () => ({ kind: "no-result" }),
        });
        const spy = vi.spyOn(session.rpc.permissions, "handlePendingPermissionRequest");

        await (session as any)._executePermissionAndRespond("request-1", { kind: "write" });

        expect(spy).not.toHaveBeenCalled();
    });

    it("throws when a v2 permission handler returns no-result", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => client.forceStop());

        const session = await client.createSession({
            onPermissionRequest: () => ({ kind: "no-result" }),
        });

        await expect(
            (client as any).handlePermissionRequestV2({
                sessionId: session.sessionId,
                permissionRequest: { kind: "write" },
            })
        ).rejects.toThrow(/protocol v2 server/);
    });

    it("forwards clientName in session.create request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => client.forceStop());

        const spy = vi.spyOn((client as any).connection!, "sendRequest");
        await client.createSession({ clientName: "my-app", onPermissionRequest: approveAll });

        expect(spy).toHaveBeenCalledWith(
            "session.create",
            expect.objectContaining({ clientName: "my-app" })
        );
    });

    it("forwards clientName in session.resume request", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => client.forceStop());

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

    it("sends session.model.switchTo RPC with correct params", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => client.forceStop());

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

    it("sends reasoningEffort with session.model.switchTo when provided", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => client.forceStop());

        const session = await client.createSession({ onPermissionRequest: approveAll });

        const spy = vi
            .spyOn((client as any).connection!, "sendRequest")
            .mockImplementation(async (method: string, _params: any) => {
                if (method === "session.model.switchTo") return {};
                throw new Error(`Unexpected method: ${method}`);
            });

        await session.setModel("claude-sonnet-4.6", { reasoningEffort: "high" });

        expect(spy).toHaveBeenCalledWith("session.model.switchTo", {
            sessionId: session.sessionId,
            modelId: "claude-sonnet-4.6",
            reasoningEffort: "high",
        });

        spy.mockRestore();
    });

    describe("URL parsing", () => {
        it("should parse port-only URL format", () => {
            const client = new CopilotClient({
                cliUrl: "8080",
                logLevel: "error",
            });

            // Verify internal state
            expect((client as any).actualPort).toBe(8080);
            expect((client as any).actualHost).toBe("localhost");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should parse host:port URL format", () => {
            const client = new CopilotClient({
                cliUrl: "127.0.0.1:9000",
                logLevel: "error",
            });

            expect((client as any).actualPort).toBe(9000);
            expect((client as any).actualHost).toBe("127.0.0.1");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should parse http://host:port URL format", () => {
            const client = new CopilotClient({
                cliUrl: "http://localhost:7000",
                logLevel: "error",
            });

            expect((client as any).actualPort).toBe(7000);
            expect((client as any).actualHost).toBe("localhost");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should parse https://host:port URL format", () => {
            const client = new CopilotClient({
                cliUrl: "https://example.com:443",
                logLevel: "error",
            });

            expect((client as any).actualPort).toBe(443);
            expect((client as any).actualHost).toBe("example.com");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should throw error for invalid URL format", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "invalid-url",
                    logLevel: "error",
                });
            }).toThrow(/Invalid cliUrl format/);
        });

        it("should throw error for invalid port - too high", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:99999",
                    logLevel: "error",
                });
            }).toThrow(/Invalid port in cliUrl/);
        });

        it("should throw error for invalid port - zero", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:0",
                    logLevel: "error",
                });
            }).toThrow(/Invalid port in cliUrl/);
        });

        it("should throw error for invalid port - negative", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:-1",
                    logLevel: "error",
                });
            }).toThrow(/Invalid port in cliUrl/);
        });

        it("should throw error when cliUrl is used with useStdio", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:8080",
                    useStdio: true,
                    logLevel: "error",
                });
            }).toThrow(/cliUrl is mutually exclusive/);
        });

        it("should throw error when cliUrl is used with cliPath", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:8080",
                    cliPath: "/path/to/cli",
                    logLevel: "error",
                });
            }).toThrow(/cliUrl is mutually exclusive/);
        });

        it("should set useStdio to false when cliUrl is provided", () => {
            const client = new CopilotClient({
                cliUrl: "8080",
                logLevel: "error",
            });

            expect(client["options"].useStdio).toBe(false);
        });

        it("should mark client as using external server", () => {
            const client = new CopilotClient({
                cliUrl: "localhost:8080",
                logLevel: "error",
            });

            expect((client as any).isExternalServer).toBe(true);
        });

        it("should not resolve cliPath when cliUrl is provided", () => {
            const client = new CopilotClient({
                cliUrl: "localhost:8080",
                logLevel: "error",
            });

            expect(client["options"].cliPath).toBeUndefined();
        });
    });

    describe("Auth options", () => {
        it("should accept githubToken option", () => {
            const client = new CopilotClient({
                githubToken: "gho_test_token",
                logLevel: "error",
            });

            expect((client as any).options.githubToken).toBe("gho_test_token");
        });

        it("should default useLoggedInUser to true when no githubToken", () => {
            const client = new CopilotClient({
                logLevel: "error",
            });

            expect((client as any).options.useLoggedInUser).toBe(true);
        });

        it("should default useLoggedInUser to false when githubToken is provided", () => {
            const client = new CopilotClient({
                githubToken: "gho_test_token",
                logLevel: "error",
            });

            expect((client as any).options.useLoggedInUser).toBe(false);
        });

        it("should allow explicit useLoggedInUser: true with githubToken", () => {
            const client = new CopilotClient({
                githubToken: "gho_test_token",
                useLoggedInUser: true,
                logLevel: "error",
            });

            expect((client as any).options.useLoggedInUser).toBe(true);
        });

        it("should allow explicit useLoggedInUser: false without githubToken", () => {
            const client = new CopilotClient({
                useLoggedInUser: false,
                logLevel: "error",
            });

            expect((client as any).options.useLoggedInUser).toBe(false);
        });

        it("should throw error when githubToken is used with cliUrl", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:8080",
                    githubToken: "gho_test_token",
                    logLevel: "error",
                });
            }).toThrow(/githubToken and useLoggedInUser cannot be used with cliUrl/);
        });

        it("should throw error when useLoggedInUser is used with cliUrl", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:8080",
                    useLoggedInUser: false,
                    logLevel: "error",
                });
            }).toThrow(/githubToken and useLoggedInUser cannot be used with cliUrl/);
        });
    });

    describe("overridesBuiltInTool in tool definitions", () => {
        it("sends overridesBuiltInTool in tool definition on session.create", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

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
            onTestFinished(() => client.forceStop());

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

    describe("agent parameter in session creation", () => {
        it("forwards agent in session.create request", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

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

        it("forwards agent in session.resume request", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

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
                },
            ];

            const handler = vi.fn().mockReturnValue(customModels);
            const client = new CopilotClient({ onListModels: handler });
            await client.start();
            onTestFinished(() => client.forceStop());

            const models = await client.listModels();
            expect(handler).toHaveBeenCalledTimes(1);
            expect(models).toEqual(customModels);
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
            onTestFinished(() => client.forceStop());

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
            onTestFinished(() => client.forceStop());

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
        it("transitions to disconnected when child process is killed", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            expect(client.getState()).toBe("connected");

            // Kill the child process to simulate unexpected termination
            const proc = (client as any).cliProcess as import("node:child_process").ChildProcess;
            proc.kill();

            // Wait for the connection.onClose handler to fire
            await vi.waitFor(() => {
                expect(client.getState()).toBe("disconnected");
            });
        });
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
            onTestFinished(() => client.forceStop());

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
            onTestFinished(() => client.forceStop());

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
            onTestFinished(() => client.forceStop());

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

        it("does not include trace context when no callback is provided", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({ onPermissionRequest: approveAll });

            const [, params] = spy.mock.calls.find(([method]) => method === "session.create")!;
            expect(params.traceparent).toBeUndefined();
            expect(params.tracestate).toBeUndefined();
        });
    });

    describe("commands in session creation", () => {
        it("forwards commands metadata in session.create request", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({
                onPermissionRequest: approveAll,
                commands: [
                    { name: "deploy", description: "Deploy to production", handler: async () => {} },
                    { name: "status", handler: async () => {} },
                ],
            });

            expect(spy).toHaveBeenCalledWith(
                "session.create",
                expect.objectContaining({
                    commands: [
                        { name: "deploy", description: "Deploy to production" },
                        { name: "status", description: undefined },
                    ],
                })
            );
        });

        it("forwards commands metadata in session.resume request", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.resume") return { sessionId: params.sessionId };
                    throw new Error(`Unexpected method: ${method}`);
                });

            await client.resumeSession(session.sessionId, {
                onPermissionRequest: approveAll,
                commands: [{ name: "test-cmd", description: "A test", handler: async () => {} }],
            });

            expect(spy).toHaveBeenCalledWith(
                "session.resume",
                expect.objectContaining({
                    commands: [{ name: "test-cmd", description: "A test" }],
                })
            );
            spy.mockRestore();
        });

        it("sends undefined commands when none are provided", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            const spy = vi.spyOn((client as any).connection!, "sendRequest");
            await client.createSession({ onPermissionRequest: approveAll });

            const [, params] = spy.mock.calls.find(([method]) => method === "session.create")!;
            expect(params.commands).toBeUndefined();
        });
    });

    describe("session.ui capability negotiation", () => {
        it("session.ui is undefined when host does not report ui capability", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            // Default CLI response doesn't include capabilities.ui
            const session = await client.createSession({ onPermissionRequest: approveAll });
            expect(session.ui).toBeUndefined();
        });

        it("session.ui is wired up when host reports ui capability", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            // Mock session.create to return capabilities.ui = true
            const origSendRequest = (client as any).connection!.sendRequest.bind(
                (client as any).connection!
            );
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.create") {
                        const result = await origSendRequest(method, params);
                        return { ...result, capabilities: { ui: true } };
                    }
                    return origSendRequest(method, params);
                });

            const session = await client.createSession({ onPermissionRequest: approveAll });
            expect(session.ui).toBeDefined();

            spy.mockRestore();
        });

        it("session.ui.confirm sends correct RPC", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            // Mock to return ui capability + handle ui.confirm
            const origSendRequest = (client as any).connection!.sendRequest.bind(
                (client as any).connection!
            );
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.create") {
                        const result = await origSendRequest(method, params);
                        return { ...result, capabilities: { ui: true } };
                    }
                    if (method === "session.ui.confirm") {
                        return { confirmed: true };
                    }
                    return origSendRequest(method, params);
                });

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const result = await session.ui!.confirm("Deploy?", "Push to production?");

            expect(result).toBe(true);
            expect(spy).toHaveBeenCalledWith(
                "session.ui.confirm",
                expect.objectContaining({
                    title: "Deploy?",
                    message: "Push to production?",
                })
            );

            spy.mockRestore();
        });

        it("session.ui.select sends correct RPC with labeled options", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            const origSendRequest = (client as any).connection!.sendRequest.bind(
                (client as any).connection!
            );
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.create") {
                        const result = await origSendRequest(method, params);
                        return { ...result, capabilities: { ui: true } };
                    }
                    if (method === "session.ui.select") {
                        return { selected: "prod" };
                    }
                    return origSendRequest(method, params);
                });

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const result = await session.ui!.select("Target", [
                { value: "prod", label: "Production" },
                { value: "staging", label: "Staging" },
            ]);

            expect(result).toBe("prod");
            expect(spy).toHaveBeenCalledWith(
                "session.ui.select",
                expect.objectContaining({
                    title: "Target",
                    options: [
                        { value: "prod", label: "Production" },
                        { value: "staging", label: "Staging" },
                    ],
                })
            );

            spy.mockRestore();
        });

        it("session.ui.select normalizes string options to value/label pairs", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            const origSendRequest = (client as any).connection!.sendRequest.bind(
                (client as any).connection!
            );
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.create") {
                        const result = await origSendRequest(method, params);
                        return { ...result, capabilities: { ui: true } };
                    }
                    if (method === "session.ui.select") {
                        return { selected: "MySQL" };
                    }
                    return origSendRequest(method, params);
                });

            const session = await client.createSession({ onPermissionRequest: approveAll });
            await session.ui!.select("Pick DB", ["PostgreSQL", "MySQL"]);

            expect(spy).toHaveBeenCalledWith(
                "session.ui.select",
                expect.objectContaining({
                    options: [
                        { value: "PostgreSQL", label: "PostgreSQL" },
                        { value: "MySQL", label: "MySQL" },
                    ],
                })
            );

            spy.mockRestore();
        });

        it("session.ui.input sends correct RPC with options", async () => {
            const client = new CopilotClient();
            await client.start();
            onTestFinished(() => client.forceStop());

            const origSendRequest = (client as any).connection!.sendRequest.bind(
                (client as any).connection!
            );
            const spy = vi
                .spyOn((client as any).connection!, "sendRequest")
                .mockImplementation(async (method: string, params: any) => {
                    if (method === "session.create") {
                        const result = await origSendRequest(method, params);
                        return { ...result, capabilities: { ui: true } };
                    }
                    if (method === "session.ui.input") {
                        return { value: "user@test.com" };
                    }
                    return origSendRequest(method, params);
                });

            const session = await client.createSession({ onPermissionRequest: approveAll });
            const result = await session.ui!.input("Email", {
                placeholder: "you@example.com",
                format: "email",
                default: "test@test.com",
            });

            expect(result).toBe("user@test.com");
            expect(spy).toHaveBeenCalledWith(
                "session.ui.input",
                expect.objectContaining({
                    title: "Email",
                    placeholder: "you@example.com",
                    format: "email",
                    default: "test@test.com",
                })
            );

            spy.mockRestore();
        });
    });
});
