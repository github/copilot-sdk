/* eslint-disable @typescript-eslint/no-explicit-any */
import { expect, it, vi } from "vitest";
import { CopilotSession } from "../src/session.js";
import type { ToolInvocation } from "../src/types.js";

it("names the sub-agent and request behind a tool invocation", async () => {
    const sendRequest = vi.fn().mockResolvedValue(undefined);
    const session = new CopilotSession("session-1", { sendRequest } as never);
    const invocations: ToolInvocation[] = [];
    (session as any).toolHandlers.set("calc", async (_args: unknown, context: ToolInvocation) => {
        invocations.push(context);
        return "ok";
    });

    (session as any)._handleBroadcastEvent({
        type: "external_tool.requested",
        agentId: "task-agent-1",
        data: {
            requestId: "req-sub-agent",
            sessionId: "session-1",
            toolCallId: "tc-sub-agent",
            toolName: "calc",
            arguments: {},
        },
    });
    (session as any)._handleBroadcastEvent({
        type: "external_tool.requested",
        data: {
            requestId: "req-root",
            sessionId: "session-1",
            toolCallId: "tc-root",
            toolName: "calc",
            arguments: {},
        },
    });
    (session as any)._handleBroadcastEvent({
        type: "external_tool.requested",
        agentId: "",
        data: {
            requestId: "req-blank-agent",
            sessionId: "session-1",
            toolCallId: "tc-blank-agent",
            toolName: "calc",
            arguments: {},
        },
    });
    await vi.waitFor(() => expect(invocations).toHaveLength(3));

    const [subAgentCall, rootCall, blankAgentCall] = invocations;
    expect(subAgentCall).toMatchObject({
        agentId: "task-agent-1",
        requestId: "req-sub-agent",
        toolCallId: "tc-sub-agent",
    });
    expect(rootCall.agentId).toBeUndefined();
    expect(rootCall.requestId).toBe("req-root");
    expect(blankAgentCall.agentId).toBeUndefined();
    expect(blankAgentCall.requestId).toBe("req-blank-agent");
});
