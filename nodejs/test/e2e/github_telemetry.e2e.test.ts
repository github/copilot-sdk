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

    it("keeps telemetry ownership across disconnect and same-id recreation", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const firstIdentity = await session.rpc.telemetry.getEngagementId();
        try {
            await session.setModel("gpt-4.1");
            await waitForCondition(
                () =>
                    received.some(
                        (notification) =>
                            notification.sessionId === session.sessionId &&
                            notification.event.kind === "session_model_change" &&
                            notification.event.properties?.engagement_id ===
                                firstIdentity.engagementId
                    ),
                { timeoutMessage: "The original pipeline did not forward its model change." }
            );
        } finally {
            await session.disconnect();
        }

        // Native race tests control delayed enrichment. This boundary test proves
        // the close/recreate path and forwarding transport retain the same ownership.
        const resumeOffset = received.length;
        const resumed = await client.createSession({
            sessionId: session.sessionId,
            model: "gpt-4.1",
            onPermissionRequest: approveAll,
        });
        try {
            const secondIdentity = await resumed.rpc.telemetry.getEngagementId();
            expect(firstIdentity.engagementId).toBeTruthy();
            expect(secondIdentity.engagementId).toBeTruthy();
            expect(secondIdentity.engagementId).not.toBe(firstIdentity.engagementId);
            await resumed.setModel("gpt-5.4");
            await waitForCondition(
                () =>
                    received
                        .slice(resumeOffset)
                        .some(
                            (notification) =>
                                notification.sessionId === resumed.sessionId &&
                                notification.event.kind === "session_model_change" &&
                                notification.event.properties?.engagement_id ===
                                    secondIdentity.engagementId
                        ),
                { timeoutMessage: "The replacement pipeline did not forward its model change." }
            );
            const afterResume = received
                .slice(resumeOffset)
                .filter((notification) => notification.sessionId === resumed.sessionId);
            expect(
                afterResume.some(
                    (notification) =>
                        notification.event.properties?.engagement_id === firstIdentity.engagementId
                )
            ).toBe(false);
        } finally {
            await resumed.disconnect();
        }
    });
});
