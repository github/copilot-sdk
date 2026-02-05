import { describe, expect, it } from "vitest";
import {
    stringToAcpContent,
    acpUpdateToSessionEvent,
    acpToolCallToSessionEvent,
    copilotSessionConfigToAcpParams,
    copilotMessageOptionsToAcpParams,
} from "../../../src/protocols/acp/acp-mapper.js";
import type {
    AcpAgentMessageChunkParams,
    AcpAgentThoughtChunkParams,
    AcpAgentMessageParams,
    AcpEndTurnParams,
    AcpErrorParams,
    AcpToolCallUpdateInner,
    AcpToolCallUpdateUpdateInner,
} from "../../../src/protocols/acp/acp-types.js";

describe("AcpMapper", () => {
    describe("stringToAcpContent", () => {
        it("should convert string prompt to ACP content array", () => {
            const result = stringToAcpContent("Hello, world!");

            expect(result).toEqual([{ type: "text", text: "Hello, world!" }]);
        });

        it("should handle empty string", () => {
            const result = stringToAcpContent("");

            expect(result).toEqual([{ type: "text", text: "" }]);
        });

        it("should handle multi-line string", () => {
            const result = stringToAcpContent("Line 1\nLine 2\nLine 3");

            expect(result).toEqual([{ type: "text", text: "Line 1\nLine 2\nLine 3" }]);
        });
    });

    describe("acpUpdateToSessionEvent", () => {
        it("should map agent_message_chunk to assistant.message_delta", () => {
            const update: AcpAgentMessageChunkParams = {
                sessionId: "sess-123",
                type: "agent_message_chunk",
                messageId: "msg-456",
                content: "Hello",
            };

            const event = acpUpdateToSessionEvent(update);

            expect(event).toMatchObject({
                type: "assistant.message_delta",
                ephemeral: true,
                data: {
                    messageId: "msg-456",
                    deltaContent: "Hello",
                },
            });
            expect(event).toHaveProperty("id");
            expect(event).toHaveProperty("timestamp");
            expect(event).toHaveProperty("parentId", null);
        });

        it("should map agent_thought_chunk to assistant.reasoning_delta", () => {
            const update: AcpAgentThoughtChunkParams = {
                sessionId: "sess-123",
                type: "agent_thought_chunk",
                messageId: "msg-456",
                content: "Let me think...",
            };

            const event = acpUpdateToSessionEvent(update);

            expect(event).toMatchObject({
                type: "assistant.reasoning_delta",
                ephemeral: true,
                data: {
                    reasoningId: "msg-456",
                    deltaContent: "Let me think...",
                },
            });
            expect(event).toHaveProperty("id");
            expect(event).toHaveProperty("timestamp");
        });

        it("should map agent_message to assistant.message", () => {
            const update: AcpAgentMessageParams = {
                sessionId: "sess-123",
                type: "agent_message",
                messageId: "msg-456",
                content: "Complete response here",
            };

            const event = acpUpdateToSessionEvent(update);

            expect(event).toMatchObject({
                type: "assistant.message",
                data: {
                    messageId: "msg-456",
                    content: "Complete response here",
                    toolRequests: [],
                },
            });
            expect(event).toHaveProperty("id");
            expect(event).toHaveProperty("timestamp");
        });

        it("should map end_turn to session.idle", () => {
            const update: AcpEndTurnParams = {
                sessionId: "sess-123",
                type: "end_turn",
            };

            const event = acpUpdateToSessionEvent(update);

            expect(event).toMatchObject({
                type: "session.idle",
                ephemeral: true,
                data: {},
            });
            expect(event).toHaveProperty("id");
            expect(event).toHaveProperty("timestamp");
        });

        it("should map error to session.error", () => {
            const update: AcpErrorParams = {
                sessionId: "sess-123",
                type: "error",
                message: "Something went wrong",
                code: "INTERNAL_ERROR",
            };

            const event = acpUpdateToSessionEvent(update);

            expect(event).toMatchObject({
                type: "session.error",
                data: {
                    errorType: "internal",
                    message: "Something went wrong",
                },
            });
            expect(event).toHaveProperty("id");
            expect(event).toHaveProperty("timestamp");
        });

        it("should return null for unknown update type", () => {
            const update = {
                sessionId: "sess-123",
                type: "unknown_type" as const,
            } as unknown as AcpAgentMessageChunkParams;

            const event = acpUpdateToSessionEvent(update);

            expect(event).toBeNull();
        });
    });

    describe("copilotSessionConfigToAcpParams", () => {
        it("should convert basic session config", () => {
            const result = copilotSessionConfigToAcpParams({});

            expect(result).toEqual({});
        });

        it("should include cwd from workingDirectory", () => {
            const result = copilotSessionConfigToAcpParams({
                workingDirectory: "/home/user/project",
            });

            expect(result).toEqual({
                cwd: "/home/user/project",
            });
        });

        it("should convert mcpServers to ACP format", () => {
            const result = copilotSessionConfigToAcpParams({
                mcpServers: {
                    myServer: {
                        type: "local",
                        command: "node",
                        args: ["server.js"],
                        tools: ["*"],
                        env: { DEBUG: "true" },
                    },
                },
            });

            expect(result).toEqual({
                mcpServers: {
                    myServer: {
                        command: "node",
                        args: ["server.js"],
                        env: { DEBUG: "true" },
                    },
                },
            });
        });

        it("should filter out remote MCP servers", () => {
            const result = copilotSessionConfigToAcpParams({
                mcpServers: {
                    localServer: {
                        type: "local",
                        command: "node",
                        args: ["local.js"],
                        tools: ["*"],
                    },
                    remoteServer: {
                        type: "http",
                        url: "http://example.com",
                        tools: ["*"],
                    },
                },
            });

            expect(result).toEqual({
                mcpServers: {
                    localServer: {
                        command: "node",
                        args: ["local.js"],
                    },
                },
            });
        });
    });

    describe("copilotMessageOptionsToAcpParams", () => {
        it("should convert prompt to prompt array", () => {
            const result = copilotMessageOptionsToAcpParams("sess-123", {
                prompt: "Hello!",
            });

            expect(result).toEqual({
                sessionId: "sess-123",
                prompt: [{ type: "text", text: "Hello!" }],
            });
        });

        it("should handle prompt with attachments (attachments ignored in ACP)", () => {
            const result = copilotMessageOptionsToAcpParams("sess-123", {
                prompt: "Analyze this file",
                attachments: [{ type: "file", path: "/path/to/file.ts" }],
            });

            // ACP doesn't support attachments in the same way, so we just include prompt
            expect(result).toEqual({
                sessionId: "sess-123",
                prompt: [{ type: "text", text: "Analyze this file" }],
            });
        });
    });

    describe("acpToolCallToSessionEvent", () => {
        it("should map tool_call with status running to tool.execution_start", () => {
            const update: AcpToolCallUpdateInner = {
                sessionUpdate: "tool_call",
                toolCallId: "tool-123",
                title: "Reading file config.json",
                kind: "file_read",
                status: "running",
                rawInput: { path: "config.json" },
            };

            const event = acpToolCallToSessionEvent(update);

            expect(event).toMatchObject({
                type: "tool.execution_start",
                data: {
                    toolCallId: "tool-123",
                    toolName: "file_read",
                    arguments: { path: "config.json" },
                },
            });
            expect(event).toHaveProperty("id");
            expect(event).toHaveProperty("timestamp");
            expect(event).toHaveProperty("parentId", null);
        });

        it("should map tool_call_update with status completed to tool.execution_complete", () => {
            const update: AcpToolCallUpdateUpdateInner = {
                sessionUpdate: "tool_call_update",
                toolCallId: "tool-123",
                status: "completed",
                rawOutput: { success: true },
                content: [{ type: "text", text: "File content here" }],
            };

            const event = acpToolCallToSessionEvent(update);

            expect(event).toMatchObject({
                type: "tool.execution_complete",
                data: {
                    toolCallId: "tool-123",
                    success: true,
                    result: {
                        content: "File content here",
                    },
                },
            });
            expect(event).toHaveProperty("id");
            expect(event).toHaveProperty("timestamp");
        });

        it("should map tool_call_update with status failed to tool.execution_complete with error", () => {
            const update: AcpToolCallUpdateUpdateInner = {
                sessionUpdate: "tool_call_update",
                toolCallId: "tool-123",
                status: "failed",
                content: [{ type: "text", text: "File not found" }],
            };

            const event = acpToolCallToSessionEvent(update);

            expect(event).toMatchObject({
                type: "tool.execution_complete",
                data: {
                    toolCallId: "tool-123",
                    success: false,
                    error: {
                        message: "File not found",
                    },
                },
            });
            expect(event).toHaveProperty("id");
            expect(event).toHaveProperty("timestamp");
        });

        it("should map tool_call_update with status running to tool.execution_progress", () => {
            const update: AcpToolCallUpdateUpdateInner = {
                sessionUpdate: "tool_call_update",
                toolCallId: "tool-123",
                status: "running",
                content: [{ type: "text", text: "Processing 50%..." }],
            };

            const event = acpToolCallToSessionEvent(update);

            expect(event).toMatchObject({
                type: "tool.execution_progress",
                ephemeral: true,
                data: {
                    toolCallId: "tool-123",
                    progressMessage: "Processing 50%...",
                },
            });
            expect(event).toHaveProperty("id");
            expect(event).toHaveProperty("timestamp");
        });

        it("should handle tool_call with completed status directly", () => {
            const update: AcpToolCallUpdateInner = {
                sessionUpdate: "tool_call",
                toolCallId: "tool-456",
                title: "Quick command",
                kind: "command",
                status: "completed",
                rawInput: { command: "echo hello" },
                rawOutput: { exitCode: 0 },
                content: [{ type: "text", text: "hello" }],
            };

            const event = acpToolCallToSessionEvent(update);

            expect(event).toMatchObject({
                type: "tool.execution_complete",
                data: {
                    toolCallId: "tool-456",
                    success: true,
                    result: {
                        content: "hello",
                    },
                },
            });
        });

        it("should handle tool_call with no content", () => {
            const update: AcpToolCallUpdateInner = {
                sessionUpdate: "tool_call",
                toolCallId: "tool-789",
                title: "Silent operation",
                kind: "other",
                status: "running",
            };

            const event = acpToolCallToSessionEvent(update);

            expect(event).toMatchObject({
                type: "tool.execution_start",
                data: {
                    toolCallId: "tool-789",
                    toolName: "other",
                },
            });
        });
    });
});
