/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Scenario testing persistence", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should retry from existing history with empty sendmessages", async () => {
        const session = await client.createSession({
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
        });

        try {
            const initial = await session.sendAndWait({
                prompt: "Reply with exactly EMPTY_BATCH_CONTEXT_READY.",
            });
            expect(initial?.data.content).toBe("EMPTY_BATCH_CONTEXT_READY");

            const result = await session.rpc.sendMessages({ messages: [], wait: true });
            expect(result.messageIds).toEqual([]);

            const events = await session.getEvents();
            const finalAssistantMessage = [...events]
                .reverse()
                .find((event) => event.type === "assistant.message");
            expect(finalAssistantMessage?.data.content).toBe("EMPTY_BATCH_RETRY_DONE");
        } finally {
            await session.disconnect();
        }
    });
});
