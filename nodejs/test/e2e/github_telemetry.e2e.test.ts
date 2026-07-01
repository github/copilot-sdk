/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, GitHubTelemetryNotification } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

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

            // Telemetry forwarding is asynchronous and in-process; poll until the
            // runtime has forwarded at least one event or we time out.
            const deadline = Date.now() + 30_000;
            while (received.length === 0 && Date.now() < deadline) {
                await new Promise((resolve) => setTimeout(resolve, 100));
            }

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
