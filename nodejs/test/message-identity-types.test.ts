import { describe, expect, expectTypeOf, it } from "vitest";
import type { QueuePendingItems, SendMessageItem, SendRequest } from "../src/generated/rpc.js";
import type { ToolExecutionStartData, UserMessageData } from "../src/generated/session-events.js";

describe("generated message identity types", () => {
    it("keeps admission correlation optional, string-valued, and independent of messageId", () => {
        expectTypeOf<SendRequest["clientCorrelationId"]>().toEqualTypeOf<string | undefined>();
        expectTypeOf<SendMessageItem["clientCorrelationId"]>().toEqualTypeOf<string | undefined>();
        expectTypeOf<QueuePendingItems["clientCorrelationId"]>().toEqualTypeOf<
            string | undefined
        >();
        expectTypeOf<UserMessageData["clientCorrelationId"]>().toEqualTypeOf<string | undefined>();

        for (const clientCorrelationId of [
            undefined,
            "01234567-89ab-4cde-8f01-23456789abcd",
            "01234567-89AB-4CDE-8F01-23456789ABCD",
            "not-a-uuid",
            "",
        ]) {
            const values: Array<
                SendRequest | SendMessageItem | QueuePendingItems | UserMessageData
            > = [
                { prompt: "hello", clientCorrelationId },
                {
                    id: "queue-1",
                    messageId: "canonical-1",
                    kind: "message",
                    displayText: "hello",
                    agentMode: "interactive",
                    clientCorrelationId,
                },
                {
                    content: "hello",
                    messageId: "canonical-1",
                    interactionId: "agent-loop-1",
                    clientCorrelationId,
                },
            ];
            for (const value of values) {
                const decoded = JSON.parse(JSON.stringify(value));
                expect(decoded.clientCorrelationId).toBe(clientCorrelationId);
                expect(Object.hasOwn(decoded, "clientCorrelationId")).toBe(
                    clientCorrelationId !== undefined
                );
                expect(decoded.messageId).toBe("messageId" in value ? value.messageId : undefined);
                const future = JSON.parse(
                    JSON.stringify({ ...value, futureField: { enabled: true } })
                );
                expect(future.clientCorrelationId).toBe(clientCorrelationId);
                expect(future.futureField).toEqual({ enabled: true });
            }
        }
    });

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
