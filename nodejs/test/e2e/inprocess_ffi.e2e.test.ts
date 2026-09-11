/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { setTimeout as delay } from "node:timers/promises";
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

    it("keeps the event loop responsive when force-stopping pending outbound traffic", async () => {
        const baseDirectory = await mkdtemp(join(tmpdir(), "copilot-ffi-stop-"));
        const client = new CopilotClient({
            connection: RuntimeConnection.forInProcess(),
            mode: "empty",
            useLoggedInUser: false,
            baseDirectory,
        });
        try {
            await client.start();
            const pending = Array.from({ length: 200 }, () => client.ping("x").catch(() => {}));
            await delay(1);

            const start = performance.now();
            const timer = delay(10).then(() => performance.now() - start);
            await client.forceStop();
            const timerDelay = await timer;
            await Promise.all(pending);

            // Native close waits up to five seconds if it blocks callback delivery.
            expect(timerDelay).toBeLessThan(2000);
        } finally {
            await client.forceStop();
            await rm(baseDirectory, { recursive: true, force: true });
        }
    });
});
