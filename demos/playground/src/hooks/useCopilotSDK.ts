/**
 * useCopilotSDK - Main hook for interacting with the Copilot SDK
 *
 * This hook encapsulates all SDK interaction logic, making it easy to:
 * - Monitor connection status
 * - Create and manage sessions
 * - Send messages and receive streaming responses
 * - Track events and logs
 *
 * Example usage:
 * ```tsx
 * function MyComponent() {
 *     const sdk = useCopilotSDK();
 *
 *     // Check connection
 *     if (!sdk.isConnected) return <div>Connecting...</div>;
 *
 *     // Create a session
 *     await sdk.createSession({ model: "claude-sonnet-4.5" });
 *
 *     // Send a message
 *     await sdk.sendMessage("Hello!");
 * }
 * ```
 */

import { useCallback, useEffect, useRef, useState } from "react";
import * as api from "../api";
import type { CreateSessionOptions, LogEntry, Message, SDKEvent, Session } from "../types";

/** Known CLI tools with display information */
const KNOWN_TOOLS: Record<string, { icon: string; label: string }> = {
    bash: { icon: "💻", label: "Shell Command" },
    read_bash: { icon: "📖", label: "Read Shell Output" },
    write_bash: { icon: "✏️", label: "Write to Shell" },
    stop_bash: { icon: "🛑", label: "Stop Shell" },
    list_bash: { icon: "📋", label: "List Shells" },
    grep: { icon: "🔍", label: "Search Content" },
    glob: { icon: "📂", label: "Find Files" },
    view: { icon: "👁️", label: "View File" },
    edit: { icon: "✏️", label: "Edit File" },
    create: { icon: "📄", label: "Create File" },
    task: { icon: "📋", label: "Task" },
    web_search: { icon: "🌐", label: "Web Search" },
};

export function getToolInfo(toolName: string) {
    return KNOWN_TOOLS[toolName] || { icon: "🔧", label: toolName };
}

export function useCopilotSDK() {
    // Connection state
    const [isConnected, setIsConnected] = useState(false);

    // Sessions state
    const [sessions, setSessions] = useState<Session[]>([]);
    const [activeSessionId, setActiveSessionId] = useState<string | null>(null);

    // Messages for the active session
    const [messages, setMessages] = useState<Message[]>([]);

    // Event tracking for polling
    const lastEventIndexRef = useRef<Record<string, number>>({});

    // Logs for debugging
    const [logs, setLogs] = useState<LogEntry[]>([]);

    // Helper to add a log entry
    const log = useCallback((level: LogEntry["level"], message: string) => {
        const timestamp = new Date().toLocaleTimeString();
        setLogs((prev) => [...prev.slice(-999), { timestamp, level, message }]);
        console.log(`[${level.toUpperCase()}]`, message);
    }, []);

    // Get the active session object
    const activeSession = sessions.find((s) => s.id === activeSessionId) || null;

    /**
     * Poll server status to check SDK connection
     */
    const checkStatus = useCallback(async () => {
        try {
            const status = await api.getStatus();
            setIsConnected(status.connected);

            // Restore sessions if we don't have them locally
            if (status.sessions.length > 0 && sessions.length === 0) {
                const restoredSessions = status.sessions.map((s) => ({
                    id: s.id,
                    model: s.model,
                }));
                setSessions(restoredSessions);
                log("info", `Restored ${restoredSessions.length} session(s)`);

                // Select the first session if none is active
                if (!activeSessionId && restoredSessions.length > 0) {
                    setActiveSessionId(restoredSessions[0].id);
                }
            }
        } catch (error) {
            setIsConnected(false);
            log("error", `Status check failed: ${(error as Error).message}`);
        }
    }, [sessions.length, activeSessionId, log]);

    /**
     * Create a new session
     */
    const createSession = useCallback(
        async (options: CreateSessionOptions) => {
            try {
                log("info", `Creating session with model: ${options.model}`);
                const response = await api.createSession(options);

                const newSession: Session = {
                    id: response.sessionId,
                    model: options.model,
                };

                setSessions((prev) => [...prev, newSession]);
                setActiveSessionId(response.sessionId);
                setMessages([]);
                lastEventIndexRef.current[response.sessionId] = 0;

                log("info", `Session created: ${response.sessionId}`);
                return response.sessionId;
            } catch (error) {
                log("error", `Failed to create session: ${(error as Error).message}`);
                throw error;
            }
        },
        [log]
    );

    /**
     * Destroy a session
     */
    const destroySession = useCallback(
        async (sessionId: string) => {
            try {
                log("info", `Destroying session: ${sessionId}`);
                await api.destroySession(sessionId);

                setSessions((prev) => prev.filter((s) => s.id !== sessionId));
                delete lastEventIndexRef.current[sessionId];

                // If this was the active session, clear it
                if (activeSessionId === sessionId) {
                    setActiveSessionId(null);
                    setMessages([]);
                }

                log("info", `Session destroyed: ${sessionId}`);
            } catch (error) {
                log("error", `Failed to destroy session: ${(error as Error).message}`);
                throw error;
            }
        },
        [activeSessionId, log]
    );

    /**
     * Select a session to view
     */
    const selectSession = useCallback(
        async (sessionId: string) => {
            setActiveSessionId(sessionId);
            setMessages([]);

            // Fetch existing messages for this session
            try {
                const response = await api.getMessages(sessionId);
                const processedMessages = processServerMessages(response.messages);
                setMessages(processedMessages);

                // Update event index to avoid re-processing
                const eventsResponse = await api.getEvents(sessionId);
                lastEventIndexRef.current[sessionId] = eventsResponse.events.length;
            } catch (error) {
                log("error", `Failed to load session messages: ${(error as Error).message}`);
            }
        },
        [log]
    );

    /**
     * Send a message to the active session
     */
    const sendMessage = useCallback(
        async (prompt: string) => {
            if (!activeSessionId) {
                throw new Error("No active session");
            }

            try {
                // Add user message immediately
                setMessages((prev) => [...prev, { type: "user", content: prompt }]);

                log("info", `Sending message to ${activeSessionId}`);
                await api.sendMessage(activeSessionId, prompt);
            } catch (error) {
                log("error", `Failed to send message: ${(error as Error).message}`);
                setMessages((prev) => [
                    ...prev,
                    { type: "error", content: (error as Error).message },
                ]);
                throw error;
            }
        },
        [activeSessionId, log]
    );

    /**
     * Poll for new events from the active session
     */
    const pollEvents = useCallback(async () => {
        if (!activeSessionId) return;

        try {
            const response = await api.getEvents(activeSessionId);
            const lastIndex = lastEventIndexRef.current[activeSessionId] || 0;
            const newEvents = response.events.slice(lastIndex);

            if (newEvents.length > 0) {
                lastEventIndexRef.current[activeSessionId] = response.events.length;
                processNewEvents(newEvents, setMessages, log);
            }
        } catch (error) {
            // Session might have been destroyed, ignore 404s
            if ((error as Error).message !== "Session not found") {
                log("debug", `Event polling error: ${(error as Error).message}`);
            }
        }
    }, [activeSessionId, log]);

    // Poll status on mount and periodically
    useEffect(() => {
        checkStatus();
        const interval = setInterval(checkStatus, 5000);
        return () => clearInterval(interval);
    }, [checkStatus]);

    // Poll events for active session
    useEffect(() => {
        if (!activeSessionId) return;

        const interval = setInterval(pollEvents, 1000);
        return () => clearInterval(interval);
    }, [activeSessionId, pollEvents]);

    return {
        // Connection
        isConnected,

        // Sessions
        sessions,
        activeSessionId,
        activeSession,
        createSession,
        destroySession,
        selectSession,

        // Messages
        messages,
        sendMessage,

        // Logs
        logs,
    };
}

/**
 * Process server messages into our Message format
 */
function processServerMessages(events: SDKEvent[]): Message[] {
    const messages: Message[] = [];
    const toolCalls = new Map<string, { toolName: string; arguments: unknown }>();

    // First pass: collect tool calls
    for (const event of events) {
        if (event.type === "tool.execution_start") {
            const data = event.data as { toolCallId: string; toolName: string; arguments: unknown };
            toolCalls.set(data.toolCallId, {
                toolName: data.toolName,
                arguments: data.arguments,
            });
        }
    }

    // Second pass: process messages
    for (const event of events) {
        if (event.type === "user.message") {
            const data = event.data as { content?: string; prompt?: string };
            messages.push({
                type: "user",
                content: data.content || data.prompt || "",
            });
        } else if (event.type === "assistant.message") {
            const data = event.data as { content?: string };
            const content = data.content || "";
            if (content.trim()) {
                messages.push({ type: "assistant", content });
            }
        } else if (event.type === "tool.execution_start") {
            const data = event.data as { toolCallId: string; toolName: string; arguments: unknown };
            messages.push({
                type: "tool",
                content: "",
                toolName: data.toolName,
                toolCallId: data.toolCallId,
                arguments: data.arguments,
                completed: false,
            });
        } else if (event.type === "tool.execution_complete") {
            const data = event.data as {
                toolCallId: string;
                success: boolean;
                result?: { content?: unknown };
                error?: { message: string };
            };
            const existing = messages.find(
                (m) => m.type === "tool" && m.toolCallId === data.toolCallId
            );
            if (existing) {
                existing.completed = true;
                existing.success = data.success;
                existing.result = data.result?.content || data.result;
                existing.error = data.error?.message;
            }
        }
    }

    return messages;
}

/**
 * Process new events and update messages
 */
function processNewEvents(
    events: SDKEvent[],
    setMessages: React.Dispatch<React.SetStateAction<Message[]>>,
    log: (level: LogEntry["level"], message: string) => void
) {
    for (const event of events) {
        log("debug", `Event: ${event.type}`);

        if (event.type === "assistant.message") {
            const data = event.data as { content?: string };
            const content = data.content || "";
            if (content.trim()) {
                setMessages((prev) => [...prev, { type: "assistant", content }]);
            }
        } else if (event.type === "assistant.message_chunk") {
            const data = event.data as { content?: string };
            setMessages((prev) => {
                const last = prev[prev.length - 1];
                if (last?.type === "assistant") {
                    return [
                        ...prev.slice(0, -1),
                        { ...last, content: last.content + (data.content || "") },
                    ];
                }
                return [...prev, { type: "assistant", content: data.content || "" }];
            });
        } else if (event.type === "tool.execution_start") {
            const data = event.data as { toolCallId: string; toolName: string; arguments: unknown };
            setMessages((prev) => [
                ...prev,
                {
                    type: "tool",
                    content: "",
                    toolName: data.toolName,
                    toolCallId: data.toolCallId,
                    arguments: data.arguments,
                    completed: false,
                },
            ]);
        } else if (event.type === "tool.execution_complete") {
            const data = event.data as {
                toolCallId: string;
                success: boolean;
                result?: { content?: unknown };
                error?: { message: string };
            };
            setMessages((prev) =>
                prev.map((m) =>
                    m.type === "tool" && m.toolCallId === data.toolCallId
                        ? {
                              ...m,
                              completed: true,
                              success: data.success,
                              result: data.result?.content || data.result,
                              error: data.error?.message,
                          }
                        : m
                )
            );
        } else if (event.type === "session.error") {
            const data = event.data as { message?: string; error?: string; details?: string };
            setMessages((prev) => [
                ...prev,
                {
                    type: "error",
                    content: data.message || data.error || "An error occurred",
                    details: data.details,
                },
            ]);
        } else if (event.type === "assistant.usage") {
            const data = event.data as {
                model?: string;
                inputTokens?: number;
                outputTokens?: number;
                cacheReadTokens?: number;
                cacheWriteTokens?: number;
                cost?: number;
                duration?: number;
            };
            setMessages((prev) => [
                ...prev,
                {
                    type: "usage",
                    content: "",
                    usage: {
                        model: data.model,
                        inputTokens: data.inputTokens,
                        outputTokens: data.outputTokens,
                        cacheReadTokens: data.cacheReadTokens,
                        cacheWriteTokens: data.cacheWriteTokens,
                        cost: data.cost,
                        duration: data.duration,
                    },
                },
            ]);
        } else if (event.type === "session.usage_info") {
            const data = event.data as {
                tokenLimit?: number;
                currentTokens?: number;
                messagesLength?: number;
            };
            setMessages((prev) => [
                ...prev,
                {
                    type: "usage",
                    content: "",
                    usage: {
                        tokenLimit: data.tokenLimit,
                        currentTokens: data.currentTokens,
                        messagesLength: data.messagesLength,
                    },
                },
            ]);
        } else if (event.type === "session.compaction_start") {
            setMessages((prev) => [
                ...prev,
                {
                    type: "compaction",
                    content: "",
                    compaction: {
                        status: "started",
                    },
                },
            ]);
        } else if (event.type === "session.compaction_complete") {
            const data = event.data as {
                success: boolean;
                error?: string;
                preCompactionTokens?: number;
                postCompactionTokens?: number;
                preCompactionMessagesLength?: number;
                messagesRemoved?: number;
                tokensRemoved?: number;
                summaryContent?: string;
                compactionTokensUsed?: {
                    input: number;
                    output: number;
                    cachedInput: number;
                };
            };
            setMessages((prev) => [
                ...prev,
                {
                    type: "compaction",
                    content: "",
                    compaction: {
                        status: "complete",
                        success: data.success,
                        error: data.error,
                        preCompactionTokens: data.preCompactionTokens,
                        postCompactionTokens: data.postCompactionTokens,
                        preCompactionMessagesLength: data.preCompactionMessagesLength,
                        messagesRemoved: data.messagesRemoved,
                        tokensRemoved: data.tokensRemoved,
                        summaryContent: data.summaryContent,
                        compactionTokensUsed: data.compactionTokensUsed,
                    },
                },
            ]);
        }
    }
}
