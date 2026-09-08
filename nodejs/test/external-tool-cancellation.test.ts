/* eslint-disable @typescript-eslint/no-explicit-any */
import { expect, it, vi } from "vitest";
import { CopilotSession } from "../src/session.js";
import type { ToolInvocation } from "../src/types.js";

it("cancels a blocked external tool when completion arrives", async () => {
    const session = new CopilotSession("session-1", {} as never);
    let invocation: ToolInvocation | undefined;
    let started!: () => void;
    const toolStarted = new Promise<void>((resolve) => {
        started = resolve;
    });

    (session as any).toolHandlers.set(
        "blocked_tool",
        async (_args: unknown, context: ToolInvocation) => {
            invocation = context;
            started();
            await new Promise((_, reject) =>
                context.signal?.addEventListener("abort", () => reject(context.signal?.reason), {
                    once: true,
                })
            );
        }
    );

    (session as any)._handleBroadcastEvent({
        type: "external_tool.requested",
        data: {
            requestId: "request-1",
            sessionId: "session-1",
            toolCallId: "tool-call-1",
            toolName: "blocked_tool",
            arguments: {},
        },
    });
    await toolStarted;

    (session as any)._handleBroadcastEvent({
        type: "external_tool.completed",
        data: { requestId: "request-1" },
    });

    expect(invocation?.signal?.aborted).toBe(true);
});

it("does not respond when a cancelled handler returns a late result", async () => {
    const sendRequest = vi.fn().mockResolvedValue(undefined);
    const session = new CopilotSession("session-1", { sendRequest } as never);
    let started!: () => void;
    const toolStarted = new Promise<void>((resolve) => {
        started = resolve;
    });

    (session as any).toolHandlers.set(
        "late_tool",
        async (_args: unknown, context: ToolInvocation) => {
            started();
            await new Promise<void>((resolve) =>
                context.signal?.addEventListener("abort", () => resolve(), { once: true })
            );
            return "late result";
        }
    );

    (session as any)._handleBroadcastEvent({
        type: "external_tool.requested",
        data: {
            requestId: "request-late",
            sessionId: "session-1",
            toolCallId: "tool-call-late",
            toolName: "late_tool",
            arguments: {},
        },
    });
    await toolStarted;
    (session as any)._handleBroadcastEvent({
        type: "external_tool.completed",
        data: { requestId: "request-late" },
    });
    await vi.waitFor(() => expect((session as any).pendingExternalTools.size).toBe(0));

    expect(sendRequest).not.toHaveBeenCalled();
});

it("aborts the invocation signal after a normal tool result", async () => {
    const sendRequest = vi.fn().mockResolvedValue(undefined);
    const session = new CopilotSession("session-1", { sendRequest } as never);
    let invocation: ToolInvocation | undefined;
    (session as any).toolHandlers.set(
        "completed_tool",
        async (_args: unknown, context: ToolInvocation) => {
            invocation = context;
            return "done";
        }
    );

    (session as any)._handleBroadcastEvent({
        type: "external_tool.requested",
        data: {
            requestId: "request-completed",
            sessionId: "session-1",
            toolCallId: "tool-call-completed",
            toolName: "completed_tool",
            arguments: {},
        },
    });

    await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(1));
    expect(invocation?.signal?.aborted).toBe(true);
});

it("remains retryable when disconnect fails", async () => {
    const sendRequest = vi
        .fn()
        .mockRejectedValueOnce(new Error("transient"))
        .mockResolvedValueOnce({ success: true });
    const session = new CopilotSession("session-1", { sendRequest } as never);
    const controller = new AbortController();
    (session as any).pendingExternalTools.set("request-1", controller);

    await expect(session.disconnect()).rejects.toThrow("transient");
    expect(controller.signal.aborted).toBe(false);
    expect((session as any).pendingExternalTools.get("request-1")).toBe(controller);
    await session.disconnect();

    expect(sendRequest).toHaveBeenCalledTimes(2);
    expect(controller.signal.aborted).toBe(true);
});

it("accepts tool requests while a failing disconnect is pending", async () => {
    let rejectDetach!: (error: Error) => void;
    const sendRequest = vi.fn(
        () =>
            new Promise((_, reject) => {
                rejectDetach = reject;
            })
    );
    const session = new CopilotSession("session-1", { sendRequest } as never);
    const handler = vi.fn(
        (_args: unknown, context: ToolInvocation) =>
            new Promise((_, reject) =>
                context.signal?.addEventListener("abort", () => reject(context.signal?.reason), {
                    once: true,
                })
            )
    );
    (session as any).toolHandlers.set("blocked_tool", handler);

    const disconnect = session.disconnect();
    (session as any)._handleBroadcastEvent({
        type: "external_tool.requested",
        data: {
            requestId: "request-during-disconnect",
            sessionId: "session-1",
            toolCallId: "tool-call-during-disconnect",
            toolName: "blocked_tool",
            arguments: {},
        },
    });
    await vi.waitFor(() => expect(handler).toHaveBeenCalledTimes(1));

    rejectDetach(new Error("transient"));
    await expect(disconnect).rejects.toThrow("transient");
    (session as any)._handleBroadcastEvent({
        type: "external_tool.completed",
        data: { requestId: "request-during-disconnect" },
    });
});

it("cancels tool-search metadata preflight before invoking the handler", async () => {
    const sendRequest = vi.fn(() => new Promise(() => {}));
    const session = new CopilotSession("session-1", { sendRequest } as never);
    const handler = vi.fn();
    (session as any).toolHandlers.set("tool_search_tool", handler);

    (session as any)._handleBroadcastEvent({
        type: "external_tool.requested",
        data: {
            requestId: "request-search",
            sessionId: "session-1",
            toolCallId: "tool-call-search",
            toolName: "tool_search_tool",
            arguments: {},
        },
    });
    await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(1));

    (session as any)._handleBroadcastEvent({
        type: "external_tool.completed",
        data: { requestId: "request-search" },
    });
    await vi.waitFor(() => expect((session as any).pendingExternalTools.size).toBe(0));

    expect(handler).not.toHaveBeenCalled();
});

it("invokes duplicate request IDs only once", async () => {
    const session = new CopilotSession("session-1", {} as never);
    const handler = vi.fn(
        (_args: unknown, context: ToolInvocation) =>
            new Promise((_, reject) =>
                context.signal?.addEventListener("abort", () => reject(context.signal?.reason), {
                    once: true,
                })
            )
    );
    (session as any).toolHandlers.set("blocked_tool", handler);
    const requested = {
        type: "external_tool.requested",
        data: {
            requestId: "request-duplicate",
            sessionId: "session-1",
            toolCallId: "tool-call-duplicate",
            toolName: "blocked_tool",
            arguments: {},
        },
    };

    (session as any)._handleBroadcastEvent(requested);
    (session as any)._handleBroadcastEvent(requested);
    await vi.waitFor(() => expect(handler).toHaveBeenCalledTimes(1));

    (session as any)._handleBroadcastEvent({
        type: "external_tool.completed",
        data: { requestId: "request-duplicate" },
    });
});
