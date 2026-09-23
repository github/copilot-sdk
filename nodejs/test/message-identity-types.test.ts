import { describe, expect, it } from "vitest";
import type { QueuePendingItems } from "../src/generated/rpc.js";
import type { ToolExecutionStartData, UserMessageData } from "../src/generated/session-events.js";

describe("generated message identity types", () => {
    it("exposes optional camelCase message IDs", () => {
        const queueItemWithIdentity: QueuePendingItems = {
            id: "queue-1",
            messageId: "message-1",
            kind: "message",
            displayText: "hello",
            agentMode: "interactive",
        };
        const queueItemFromOlderRuntime: QueuePendingItems = {
            id: "queue-2",
            kind: "command",
            displayText: "/help",
            agentMode: "interactive",
        };
        const userMessageWithIdentity: UserMessageData = {
            content: "hello",
            messageId: "message-1",
        };
        const userMessageFromOlderRuntime: UserMessageData = {
            content: "hello",
        };

        expect(queueItemWithIdentity.messageId).toBe("message-1");
        expect(queueItemFromOlderRuntime.messageId).toBeUndefined();
        expect(userMessageWithIdentity.messageId).toBe("message-1");
        expect(userMessageFromOlderRuntime.messageId).toBeUndefined();
    });

    it("exposes optional early tool context without interpreting it", () => {
        const contexts: Array<Pick<ToolExecutionStartData, "traceparent" | "tracestate">> = [
            {},
            {
                traceparent: "00-11111111111111111111111111111111-2222222222222222-01",
                tracestate: "vendor=value",
            },
            { traceparent: "00-11111111111111111111111111111111-2222222222222222-00" },
            { traceparent: "invalid", tracestate: "invalid" },
            { traceparent: "" },
        ];
        for (const context of contexts) {
            const data: ToolExecutionStartData = {
                toolCallId: "tool-call-a",
                toolName: "client-tool",
                ...context,
            };
            expect(data.traceparent).toBe(context.traceparent);
            expect(data.tracestate).toBe(context.tracestate);
            expect(JSON.parse(JSON.stringify(data))).toEqual({
                toolCallId: "tool-call-a",
                toolName: "client-tool",
                ...context,
            });
        }
    });
});
