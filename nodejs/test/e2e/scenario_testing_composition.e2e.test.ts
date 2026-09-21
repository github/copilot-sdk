/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import type { SessionEvent } from "../../src/index.js";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Scenario testing composition", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should not emit redundant model change when resuming same model", async () => {
        const original = await client.createSession({
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
        });
        const sessionId = original.sessionId;

        try {
            const response = await original.sendAndWait({
                prompt: "Reply with exactly SCENARIO_SAME_MODEL_HISTORY_READY.",
            });
            expect(response?.data.content).toBe("SCENARIO_SAME_MODEL_HISTORY_READY");
        } finally {
            await original.disconnect();
        }

        const resumeEvents: SessionEvent[] = [];
        const resumed = await client.resumeSession(sessionId, {
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
            onEvent: (event) => resumeEvents.push(event),
        });
        try {
            expect(resumeEvents.some((event) => event.type === "session.model_change")).toBe(false);
            expect((await resumed.rpc.model.getCurrent()).modelId).toBe("claude-sonnet-5");
        } finally {
            await resumed.disconnect();
        }
    });
});
