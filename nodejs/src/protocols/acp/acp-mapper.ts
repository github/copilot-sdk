/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * ACP format translation utilities.
 * Maps between Copilot SDK formats and ACP formats.
 * @module protocols/acp/acp-mapper
 */

import type { SessionEvent, SessionConfig, MessageOptions, MCPLocalServerConfig } from "../../types.js";
import type {
    AcpContentPart,
    AcpSessionNewParams,
    AcpSessionPromptParams,
    AcpSessionUpdate,
    AcpSessionUpdateParams,
    AcpMcpServerConfig,
    AcpToolCallUpdateInner,
    AcpToolCallUpdateUpdateInner,
} from "./acp-types.js";

let eventIdCounter = 0;

/**
 * Generates a unique event ID for ACP-mapped events.
 */
function generateEventId(): string {
    return `acp-${Date.now()}-${++eventIdCounter}`;
}

/**
 * Creates common event metadata fields.
 */
function createEventMetadata(): { id: string; timestamp: string; parentId: null } {
    return {
        id: generateEventId(),
        timestamp: new Date().toISOString(),
        parentId: null,
    };
}

/**
 * Converts a string prompt to ACP content array format.
 */
export function stringToAcpContent(prompt: string): AcpContentPart[] {
    return [{ type: "text", text: prompt }];
}

/**
 * Maps an ACP session/update notification (Gemini format) to a Copilot SessionEvent.
 * This handles the actual wire format from Gemini CLI.
 */
export function acpSessionUpdateToSessionEvent(params: AcpSessionUpdateParams): SessionEvent | null {
    const meta = createEventMetadata();
    const { update } = params;

    switch (update.sessionUpdate) {
        case "agent_message_chunk":
            return {
                ...meta,
                ephemeral: true,
                type: "assistant.message_delta",
                data: {
                    messageId: meta.id,
                    deltaContent: update.content?.text ?? "",
                },
            };

        case "agent_thought_chunk":
            return {
                ...meta,
                ephemeral: true,
                type: "assistant.reasoning_delta",
                data: {
                    reasoningId: meta.id,
                    deltaContent: update.content?.text ?? "",
                },
            };

        case "agent_message":
            return {
                ...meta,
                type: "assistant.message",
                data: {
                    messageId: meta.id,
                    content: update.content?.text ?? "",
                    toolRequests: [],
                },
            };

        case "end_turn":
            return {
                ...meta,
                ephemeral: true,
                type: "session.idle",
                data: {},
            };

        case "error":
            return {
                ...meta,
                type: "session.error",
                data: {
                    errorType: "internal",
                    message: update.message ?? "Unknown error",
                },
            };

        default:
            return null;
    }
}

/**
 * Maps a flat ACP session update (legacy/test format) to a Copilot SessionEvent.
 * This is primarily used for unit tests.
 */
export function acpUpdateToSessionEvent(update: AcpSessionUpdate): SessionEvent | null {
    const meta = createEventMetadata();

    switch (update.type) {
        case "agent_message_chunk":
            return {
                ...meta,
                ephemeral: true,
                type: "assistant.message_delta",
                data: {
                    messageId: update.messageId,
                    deltaContent: update.content,
                },
            };

        case "agent_thought_chunk":
            return {
                ...meta,
                ephemeral: true,
                type: "assistant.reasoning_delta",
                data: {
                    reasoningId: update.messageId,
                    deltaContent: update.content,
                },
            };

        case "agent_message":
            return {
                ...meta,
                type: "assistant.message",
                data: {
                    messageId: update.messageId,
                    content: update.content,
                    toolRequests: [],
                },
            };

        case "end_turn":
            return {
                ...meta,
                ephemeral: true,
                type: "session.idle",
                data: {},
            };

        case "error":
            return {
                ...meta,
                type: "session.error",
                data: {
                    errorType: "internal",
                    message: update.message,
                },
            };

        default:
            return null;
    }
}

/**
 * Helper to extract text content from ACP tool call content array.
 */
function extractToolCallContent(content?: { type: string; text: string }[]): string {
    if (!content || content.length === 0) {
        return "";
    }
    return content.map((c) => c.text).join("\n");
}

/**
 * Maps ACP tool_call or tool_call_update to Copilot SessionEvent.
 * Handles both initial tool calls and updates to existing tool calls.
 */
export function acpToolCallToSessionEvent(
    update: AcpToolCallUpdateInner | AcpToolCallUpdateUpdateInner
): SessionEvent | null {
    const meta = createEventMetadata();

    if (update.sessionUpdate === "tool_call") {
        const toolCall = update as AcpToolCallUpdateInner;

        // If status is completed or failed, return tool.execution_complete
        if (toolCall.status === "completed" || toolCall.status === "failed") {
            const content = extractToolCallContent(toolCall.content);
            const success = toolCall.status === "completed";

            return {
                ...meta,
                type: "tool.execution_complete",
                data: {
                    toolCallId: toolCall.toolCallId,
                    success,
                    ...(success
                        ? { result: { content } }
                        : { error: { message: content || "Tool execution failed" } }),
                },
            };
        }

        // Otherwise, return tool.execution_start
        return {
            ...meta,
            type: "tool.execution_start",
            data: {
                toolCallId: toolCall.toolCallId,
                toolName: toolCall.kind,
                ...(toolCall.rawInput !== undefined && { arguments: toolCall.rawInput }),
            },
        };
    }

    if (update.sessionUpdate === "tool_call_update") {
        const toolCallUpdate = update as AcpToolCallUpdateUpdateInner;
        const content = extractToolCallContent(toolCallUpdate.content);

        // If status is completed, return tool.execution_complete with success
        if (toolCallUpdate.status === "completed") {
            return {
                ...meta,
                type: "tool.execution_complete",
                data: {
                    toolCallId: toolCallUpdate.toolCallId,
                    success: true,
                    result: { content },
                },
            };
        }

        // If status is failed, return tool.execution_complete with error
        if (toolCallUpdate.status === "failed") {
            return {
                ...meta,
                type: "tool.execution_complete",
                data: {
                    toolCallId: toolCallUpdate.toolCallId,
                    success: false,
                    error: { message: content || "Tool execution failed" },
                },
            };
        }

        // If status is running (or undefined), return tool.execution_progress
        return {
            ...meta,
            ephemeral: true,
            type: "tool.execution_progress",
            data: {
                toolCallId: toolCallUpdate.toolCallId,
                progressMessage: content,
            },
        };
    }

    return null;
}

/**
 * Converts Copilot SessionConfig to ACP session/new params.
 */
export function copilotSessionConfigToAcpParams(config: SessionConfig): AcpSessionNewParams {
    const params: AcpSessionNewParams = {};

    if (config.workingDirectory) {
        params.cwd = config.workingDirectory;
    }

    if (config.mcpServers) {
        const acpMcpServers: Record<string, AcpMcpServerConfig> = {};

        for (const [name, serverConfig] of Object.entries(config.mcpServers)) {
            // Only include local/stdio servers, not remote ones
            if (serverConfig.type === "http" || serverConfig.type === "sse") {
                continue;
            }

            const localConfig = serverConfig as MCPLocalServerConfig;
            const acpConfig: AcpMcpServerConfig = {
                command: localConfig.command,
                args: localConfig.args,
            };

            if (localConfig.env) {
                acpConfig.env = localConfig.env;
            }

            acpMcpServers[name] = acpConfig;
        }

        if (Object.keys(acpMcpServers).length > 0) {
            params.mcpServers = acpMcpServers;
        }
    }

    return params;
}

/**
 * Converts Copilot MessageOptions to ACP session/prompt params.
 */
export function copilotMessageOptionsToAcpParams(
    sessionId: string,
    options: MessageOptions
): AcpSessionPromptParams {
    return {
        sessionId,
        prompt: stringToAcpContent(options.prompt),
    };
}
