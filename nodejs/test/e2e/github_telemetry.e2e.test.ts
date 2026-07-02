/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, GitHubTelemetryNotification } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

// Experimental: exercises the end-to-end GitHub (hydro) telemetry forwarding
// path. The runtime forwards per-session telemetry to opted-in connections via
// the `gitHubTelemetry.event` JSON-RPC *notification*; the SDK opts in
// automatically whenever an `onGitHubTelemetry` handler is registered. Creating
// a session emits an early `session.start` hydro event, so no model round-trip
// (and therefore no recorded CAPI exchange) is needed to observe forwarding.
describe("GitHub telemetry forwarding", async () => {
    const received: GitHubTelemetryNotification[] = [];

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            onGitHubTelemetry: (notification) => {
                received.push(notification);
            },
        },
    });

    it(
        "forwards gitHubTelemetry.event notifications from a live session",
        { timeout: 60_000 },
        async () => {
            received.length = 0;

            const session = await client.createSession({
                onPermissionRequest: approveAll,
            });

            // The CLI forwards telemetry over the JSON-RPC connection
            // asynchronously, so wait until at least one event arrives or we
            // time out.
            await waitForCondition(() => received.length > 0, {
                timeoutMs: 30_000,
                timeoutMessage: "Timed out waiting for a gitHubTelemetry.event notification.",
            });

            expect(received.length).toBeGreaterThan(0);

            const notification = received[0];
            expect(typeof notification.sessionId).toBe("string");
            expect(notification.sessionId.length).toBeGreaterThan(0);
            expect(typeof notification.restricted).toBe("boolean");
            expect(notification.event).toBeDefined();
            expect(typeof notification.event.kind).toBe("string");

            await session.disconnect();
        }
    );
});
