/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext, isInProcessTransport } from "./harness/sdkTestContext.js";

describe("UI ephemeral query RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    // TODO(cli-1.0.81-2): CLI 1.0.81-6 still fails session.ui.ephemeralQuery against the
    // recorded snapshot on macOS ("Failed to get response from the AI model").
    it.skipIf(isInProcessTransport || process.platform === "darwin")(
        "should answer ephemeral query",
        { timeout: 120_000 },
        async () => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            try {
                const result = await session.rpc.ui.ephemeralQuery({
                    question: "In one word, what is the primary color of a clear daytime sky?",
                });

                expect(result).toBeDefined();
                expect(result.answer.trim()).toBeTruthy();
                expect(result.answer.toLowerCase()).toContain("blue");
            } finally {
                await session.disconnect();
            }
        }
    );
});
