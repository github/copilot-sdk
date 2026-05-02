/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Error Resilience", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should throw when sending to disconnected session", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        await session.disconnect();

        await expect(session.sendAndWait({ prompt: "Hello" })).rejects.toThrow();
    });

    it("should throw when getting messages from disconnected session", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        await session.disconnect();

        await expect(session.getMessages()).rejects.toThrow();
    });

    it("should handle double abort without error", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        // First abort should be fine
        await session.abort();
        // Second abort should not throw
        await session.abort();

        // Session should still be disconnectable
        await session.disconnect();
    });

    it("should throw when resuming non-existent session", async () => {
        await expect(
            client.resumeSession("non-existent-session-id-12345", {
                onPermissionRequest: approveAll,
            })
        ).rejects.toThrow();
    });
});
