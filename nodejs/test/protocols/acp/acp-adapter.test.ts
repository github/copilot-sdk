import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { spawn } from "node:child_process";
import { PassThrough } from "node:stream";
import { AcpProtocolAdapter } from "../../../src/protocols/acp/acp-adapter.js";
import type { CopilotClientOptions } from "../../../src/types.js";
import { ACP_PROTOCOL_VERSION } from "../../../src/protocols/acp/acp-types.js";

// Mock child_process
vi.mock("node:child_process", () => ({
    spawn: vi.fn(),
}));

describe("AcpProtocolAdapter", () => {
    let mockProcess: {
        stdin: PassThrough;
        stdout: PassThrough;
        stderr: PassThrough;
        on: ReturnType<typeof vi.fn>;
        kill: ReturnType<typeof vi.fn>;
    };
    let adapter: AcpProtocolAdapter;
    let options: CopilotClientOptions;

    beforeEach(() => {
        mockProcess = {
            stdin: new PassThrough(),
            stdout: new PassThrough(),
            stderr: new PassThrough(),
            on: vi.fn(),
            kill: vi.fn(),
        };

        (spawn as ReturnType<typeof vi.fn>).mockReturnValue(mockProcess);

        options = {
            cliPath: "gemini",
            cliArgs: ["--experimental-acp"],
            cwd: "/test/dir",
            protocol: "acp",
        };
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe("start", () => {
        it("should spawn CLI with correct arguments for ACP (no --headless, --stdio, --log-level)", async () => {
            adapter = new AcpProtocolAdapter(options);

            // Don't await, just trigger start
            void adapter.start();

            // Simulate successful spawn - process stays running
            await new Promise((resolve) => setImmediate(resolve));

            // Verify spawn was called with only cliArgs (no SDK-managed args)
            expect(spawn).toHaveBeenCalledWith(
                "gemini",
                ["--experimental-acp"],
                expect.objectContaining({
                    cwd: "/test/dir",
                    stdio: ["pipe", "pipe", "pipe"],
                })
            );

            // Clean up by simulating process ready
            adapter.forceStop();
        });

        it("should not add --headless flag for ACP mode", async () => {
            adapter = new AcpProtocolAdapter(options);

            void adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const spawnCall = (spawn as ReturnType<typeof vi.fn>).mock.calls[0];
            const args = spawnCall[1] as string[];

            expect(args).not.toContain("--headless");
            expect(args).not.toContain("--stdio");
            expect(args).not.toContain("--log-level");

            adapter.forceStop();
        });
    });

    describe("connection methods", () => {
        beforeEach(() => {
            adapter = new AcpProtocolAdapter(options);
        });

        afterEach(async () => {
            await adapter.forceStop();
        });

        it("should translate ping to initialize request", async () => {
            void adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            // Call ping
            const pingPromise = connection.sendRequest("ping", { message: "test" });

            // Read what was sent
            const sentData = mockProcess.stdin.read();
            const sentMessage = JSON.parse(sentData.toString().trim());

            expect(sentMessage.method).toBe("initialize");
            expect(sentMessage.params).toEqual({ protocolVersion: ACP_PROTOCOL_VERSION });

            // Send response
            const response = {
                jsonrpc: "2.0",
                id: sentMessage.id,
                result: { protocolVersion: ACP_PROTOCOL_VERSION },
            };
            mockProcess.stdout.write(JSON.stringify(response) + "\n");

            const result = await pingPromise;
            expect(result).toEqual({
                message: "pong",
                timestamp: expect.any(Number),
                protocolVersion: ACP_PROTOCOL_VERSION,
            });
        });

        it("should translate session.create to session/new", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            const createPromise = connection.sendRequest("session.create", {
                model: "gemini-pro",
                workingDirectory: "/test/project",
            });

            const sentData = mockProcess.stdin.read();
            const sentMessage = JSON.parse(sentData.toString().trim());

            expect(sentMessage.method).toBe("session/new");
            expect(sentMessage.params).toEqual({
                cwd: "/test/project",
                mcpServers: [],
            });

            // Send response
            const response = {
                jsonrpc: "2.0",
                id: sentMessage.id,
                result: { sessionId: "acp-session-123" },
            };
            mockProcess.stdout.write(JSON.stringify(response) + "\n");

            const result = await createPromise;
            expect(result).toEqual({ sessionId: "acp-session-123" });
        });

        it("should translate session.send to session/prompt with prompt array", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            const sendPromise = connection.sendRequest("session.send", {
                sessionId: "sess-123",
                prompt: "Hello, world!",
            });

            const sentData = mockProcess.stdin.read();
            const sentMessage = JSON.parse(sentData.toString().trim());

            expect(sentMessage.method).toBe("session/prompt");
            expect(sentMessage.params).toEqual({
                sessionId: "sess-123",
                prompt: [{ type: "text", text: "Hello, world!" }],
            });

            // Send response
            const response = {
                jsonrpc: "2.0",
                id: sentMessage.id,
                result: { messageId: "msg-456" },
            };
            mockProcess.stdout.write(JSON.stringify(response) + "\n");

            const result = await sendPromise;
            expect(result).toEqual({ messageId: "msg-456" });
        });

        it("should emit session.idle when response contains stopReason end_turn", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            const eventHandler = vi.fn();
            connection.onNotification("session.event", eventHandler);

            const sendPromise = connection.sendRequest("session.send", {
                sessionId: "sess-123",
                prompt: "Hello!",
            });

            const sentData = mockProcess.stdin.read();
            const sentMessage = JSON.parse(sentData.toString().trim());

            // Gemini returns stopReason in the response instead of a separate notification
            const response = {
                jsonrpc: "2.0",
                id: sentMessage.id,
                result: { stopReason: "end_turn" },
            };
            mockProcess.stdout.write(JSON.stringify(response) + "\n");

            await sendPromise;

            // Wait for queueMicrotask to execute
            await new Promise((resolve) => setImmediate(resolve));

            expect(eventHandler).toHaveBeenCalled();
            const callArg = eventHandler.mock.calls[0][0];
            expect(callArg.sessionId).toBe("sess-123");
            expect(callArg.event.type).toBe("session.idle");
            expect(callArg.event.data).toEqual({});
            expect(callArg.event.ephemeral).toBe(true);
        });

        it("should throw for unsupported methods", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            await expect(connection.sendRequest("models.list", {})).rejects.toThrow(
                /not supported in ACP mode/
            );

            await expect(connection.sendRequest("session.resume", {})).rejects.toThrow(
                /not supported in ACP mode/
            );

            await expect(connection.sendRequest("session.getMessages", {})).rejects.toThrow(
                /not supported in ACP mode/
            );
        });
    });

    describe("notification handling", () => {
        beforeEach(() => {
            adapter = new AcpProtocolAdapter(options);
        });

        afterEach(async () => {
            await adapter.forceStop();
        });

        it("should translate session/update notifications to session.event format", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            const eventHandler = vi.fn();
            connection.onNotification("session.event", eventHandler);

            // Send ACP update notification (Gemini format with nested update object)
            const acpNotification = {
                jsonrpc: "2.0",
                method: "session/update",
                params: {
                    sessionId: "sess-123",
                    update: {
                        sessionUpdate: "agent_message_chunk",
                        content: { type: "text", text: "Hello" },
                    },
                },
            };
            mockProcess.stdout.write(JSON.stringify(acpNotification) + "\n");

            await new Promise((resolve) => setImmediate(resolve));

            expect(eventHandler).toHaveBeenCalled();
            const callArg = eventHandler.mock.calls[0][0];
            expect(callArg.sessionId).toBe("sess-123");
            expect(callArg.event.type).toBe("assistant.message_delta");
            expect(callArg.event.data.deltaContent).toBe("Hello");
            expect(callArg.event.ephemeral).toBe(true);
            expect(callArg.event.id).toBeDefined();
            expect(callArg.event.timestamp).toBeDefined();
        });

        it("should translate end_turn to session.idle", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            const eventHandler = vi.fn();
            connection.onNotification("session.event", eventHandler);

            // Gemini format for end_turn
            const acpNotification = {
                jsonrpc: "2.0",
                method: "session/update",
                params: {
                    sessionId: "sess-123",
                    update: {
                        sessionUpdate: "end_turn",
                    },
                },
            };
            mockProcess.stdout.write(JSON.stringify(acpNotification) + "\n");

            await new Promise((resolve) => setImmediate(resolve));

            expect(eventHandler).toHaveBeenCalled();
            const callArg = eventHandler.mock.calls[0][0];
            expect(callArg.sessionId).toBe("sess-123");
            expect(callArg.event.type).toBe("session.idle");
            expect(callArg.event.data).toEqual({});
            expect(callArg.event.ephemeral).toBe(true);
        });
    });

    describe("tool_call handling", () => {
        beforeEach(() => {
            adapter = new AcpProtocolAdapter(options);
        });

        afterEach(async () => {
            await adapter.forceStop();
        });

        it("should translate tool_call updates to tool events", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            const eventHandler = vi.fn();
            connection.onNotification("session.event", eventHandler);

            // Send ACP tool_call notification
            const acpNotification = {
                jsonrpc: "2.0",
                method: "session/update",
                params: {
                    sessionId: "sess-123",
                    update: {
                        sessionUpdate: "tool_call",
                        toolCallId: "tool-456",
                        title: "Reading file",
                        kind: "file_read",
                        status: "running",
                        rawInput: { path: "test.txt" },
                    },
                },
            };
            mockProcess.stdout.write(JSON.stringify(acpNotification) + "\n");

            await new Promise((resolve) => setImmediate(resolve));

            expect(eventHandler).toHaveBeenCalled();
            const callArg = eventHandler.mock.calls[0][0];
            expect(callArg.sessionId).toBe("sess-123");
            expect(callArg.event.type).toBe("tool.execution_start");
            expect(callArg.event.data.toolCallId).toBe("tool-456");
            expect(callArg.event.data.toolName).toBe("file_read");
        });

        it("should translate tool_call_update with completed status", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            const eventHandler = vi.fn();
            connection.onNotification("session.event", eventHandler);

            const acpNotification = {
                jsonrpc: "2.0",
                method: "session/update",
                params: {
                    sessionId: "sess-123",
                    update: {
                        sessionUpdate: "tool_call_update",
                        toolCallId: "tool-456",
                        status: "completed",
                        content: [{ type: "text", text: "File content here" }],
                    },
                },
            };
            mockProcess.stdout.write(JSON.stringify(acpNotification) + "\n");

            await new Promise((resolve) => setImmediate(resolve));

            expect(eventHandler).toHaveBeenCalled();
            const callArg = eventHandler.mock.calls[0][0];
            expect(callArg.event.type).toBe("tool.execution_complete");
            expect(callArg.event.data.toolCallId).toBe("tool-456");
            expect(callArg.event.data.success).toBe(true);
            expect(callArg.event.data.result.content).toBe("File content here");
        });
    });

    describe("permission request handling", () => {
        beforeEach(() => {
            adapter = new AcpProtocolAdapter(options);
        });

        afterEach(async () => {
            await adapter.forceStop();
        });

        it("should handle session/request_permission and return user choice", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            // Register a permission handler that always allows
            const permissionHandler = vi.fn().mockResolvedValue({
                result: { optionId: "allow_once" },
            });
            connection.onRequest("permission.request", permissionHandler);

            // Send ACP permission request
            const acpRequest = {
                jsonrpc: "2.0",
                id: 100,
                method: "session/request_permission",
                params: {
                    sessionId: "sess-123",
                    toolCall: {
                        toolCallId: "tool-456",
                        title: "Write to file",
                        kind: "file_edit",
                        rawInput: { path: "test.txt" },
                    },
                    options: [
                        { optionId: "allow_once", name: "Allow once", kind: "allow_once" },
                        { optionId: "reject_once", name: "Reject", kind: "reject_once" },
                    ],
                },
            };
            mockProcess.stdout.write(JSON.stringify(acpRequest) + "\n");

            await new Promise((resolve) => setImmediate(resolve));

            // Check that the permission handler was called
            expect(permissionHandler).toHaveBeenCalled();

            // Read the response sent back
            const sentData = mockProcess.stdin.read();
            if (sentData) {
                const sentMessage = JSON.parse(sentData.toString().trim());
                expect(sentMessage.id).toBe(100);
                expect(sentMessage.result).toMatchObject({
                    outcome: "selected",
                    optionId: "allow_once",
                });
            }
        });

        it("should return cancelled when permission handler rejects", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            // Register a permission handler that rejects
            const permissionHandler = vi.fn().mockRejectedValue(new Error("User cancelled"));
            connection.onRequest("permission.request", permissionHandler);

            // Send ACP permission request
            const acpRequest = {
                jsonrpc: "2.0",
                id: 101,
                method: "session/request_permission",
                params: {
                    sessionId: "sess-123",
                    toolCall: {
                        toolCallId: "tool-789",
                        title: "Delete file",
                        kind: "file_edit",
                    },
                    options: [
                        { optionId: "allow_once", name: "Allow", kind: "allow_once" },
                    ],
                },
            };
            mockProcess.stdout.write(JSON.stringify(acpRequest) + "\n");

            await new Promise((resolve) => setImmediate(resolve));

            // Read the response - should indicate cancelled
            const sentData = mockProcess.stdin.read();
            if (sentData) {
                const sentMessage = JSON.parse(sentData.toString().trim());
                expect(sentMessage.id).toBe(101);
                expect(sentMessage.result.outcome).toBe("cancelled");
            }
        });
    });

    describe("verifyProtocolVersion", () => {
        beforeEach(() => {
            adapter = new AcpProtocolAdapter(options);
        });

        afterEach(async () => {
            await adapter.forceStop();
        });

        it("should verify ACP protocol version", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            const verifyPromise = adapter.verifyProtocolVersion();

            // Read the initialize request
            const sentData = mockProcess.stdin.read();
            const sentMessage = JSON.parse(sentData.toString().trim());

            // Respond with matching version
            const response = {
                jsonrpc: "2.0",
                id: sentMessage.id,
                result: { protocolVersion: ACP_PROTOCOL_VERSION },
            };
            mockProcess.stdout.write(JSON.stringify(response) + "\n");

            await expect(verifyPromise).resolves.toBeUndefined();
        });

        it("should reject on version mismatch", async () => {
            adapter.start();
            await new Promise((resolve) => setImmediate(resolve));

            const connection = adapter.getConnection();
            connection.listen();

            const verifyPromise = adapter.verifyProtocolVersion();

            const sentData = mockProcess.stdin.read();
            const sentMessage = JSON.parse(sentData.toString().trim());

            // Respond with different version
            const response = {
                jsonrpc: "2.0",
                id: sentMessage.id,
                result: { protocolVersion: 999 },
            };
            mockProcess.stdout.write(JSON.stringify(response) + "\n");

            await expect(verifyPromise).rejects.toThrow(/version mismatch/i);
        });
    });
});
