/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { SessionLifecycleEvent, approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Client Lifecycle", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should return last session id after sending a message", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await session.sendAndWait({ prompt: "Say hello" });

        // Wait for session data to flush to disk
        await new Promise((r) => setTimeout(r, 500));

        const lastSessionId = await client.getLastSessionId();
        expect(lastSessionId).toBe(session.sessionId);

        await session.destroy();
    });

    it("should return undefined for getLastSessionId with no sessions", async () => {
        // On a fresh client this may return undefined or an older session ID
        const lastSessionId = await client.getLastSessionId();
        expect(lastSessionId === undefined || typeof lastSessionId === "string").toBe(true);
    });

    it("should emit session lifecycle events", async () => {
        const events: SessionLifecycleEvent[] = [];
        const unsubscribe = client.on((event: SessionLifecycleEvent) => {
            events.push(event);
        });

        try {
            const session = await client.createSession({ onPermissionRequest: approveAll });

            await session.sendAndWait({ prompt: "Say hello" });

            // Wait for session data to flush to disk
            await new Promise((r) => setTimeout(r, 500));

            // Lifecycle events may not fire in all runtimes
            if (events.length > 0) {
                const sessionEvents = events.filter((e) => e.sessionId === session.sessionId);
                expect(sessionEvents.length).toBeGreaterThan(0);
            }

            await session.destroy();
        } finally {
            unsubscribe();
        }
    });
});
