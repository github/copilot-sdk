/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { CopilotClient, RuntimeConnection } from "../../src/index.js";

describe("In-process FFI transport", () => {
    // Smoke test that the in-process FFI transport starts and completes a round-trip.
    // Resolution of the in-process transport from COPILOT_SDK_DEFAULT_CONNECTION is
    // exercised by the full E2E suite running under the `inprocess` CI matrix cell,
    // not a dedicated test.
    it("should start and connect over in-process FFI", async () => {
        // In-process FFI hosting loads runtime.node directly from the bundled runtime.
        // If it is unavailable, start() throws and the test fails hard.
        const client = new CopilotClient({ connection: RuntimeConnection.forInProcess() });
        await client.start();

        const pong = await client.ping("ffi message");
        expect(pong.message).toBe("pong: ffi message");
        expect(Date.parse(pong.timestamp)).not.toBeNaN();

        expect(await client.stop()).toHaveLength(0); // No errors on stop
    });
});
