import { describe, expect, it } from "vitest";
import type { QueuePendingItems } from "../src/generated/rpc.js";
import type { UserMessageData } from "../src/generated/session-events.js";

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
});
