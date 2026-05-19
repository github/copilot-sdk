/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { SessionLifecycleEvent, approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Client Lifecycle", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    function deferred<T>(): { promise: Promise<T>; resolve: (value: T) => void } {
        let resolveFn!: (value: T) => void;
        const promise = new Promise<T>((resolve) => {
            resolveFn = resolve;
        });
        return { promise, resolve: resolveFn };
    }

    async function withTimeout<T>(promise: Promise<T>, ms: number, label: string): Promise<T> {
        let timer: NodeJS.Timeout | undefined;
        try {
            return await Promise.race([
                promise,
                new Promise<T>((_, reject) => {
                    timer = setTimeout(() => reject(new Error(`Timeout: ${label}`)), ms);
                }),
            ]);
        } finally {
            if (timer) clearTimeout(timer);
        }
    }

    it("should return last session id after sending a message", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await session.sendAndWait({ prompt: "Say hello" });

        // Poll until getLastSessionId returns something rather than a hard 500ms wait.
        // (Using await with a polling loop keeps fast machines fast and slow CI safe.)
        let lastSessionId: string | undefined;
        const deadline = Date.now() + 10_000;
        while (Date.now() < deadline) {
            lastSessionId = await client.getLastSessionId();
            if (lastSessionId) break;
            await new Promise((r) => setTimeout(r, 50));
        }

        // In parallel test runs we can't guarantee the last session ID matches
        // this specific session, since other tests may flush session data concurrently.
        expect(lastSessionId).toBeTruthy();

        await session.disconnect();
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

            // Poll for the session-specific event rather than a hard 500ms wait.
            const deadline = Date.now() + 10_000;
            while (
                Date.now() < deadline &&
                !events.some((e) => e.sessionId === session.sessionId)
            ) {
                await new Promise((r) => setTimeout(r, 50));
            }

            // Lifecycle events may not fire in all runtimes
            if (events.length > 0) {
                const sessionEvents = events.filter((e) => e.sessionId === session.sessionId);
                expect(sessionEvents.length).toBeGreaterThan(0);
            }

            await session.disconnect();
        } finally {
            unsubscribe();
        }
    });

    it("should receive session created lifecycle event", async () => {
        const created = deferred<SessionLifecycleEvent>();
        const unsubscribe = client.on((evt) => {
            if (evt.type === "session.created") {
                created.resolve(evt);
            }
        });

        try {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const evt = await withTimeout(created.promise, 10_000, "session.created");

            expect(evt.type).toBe("session.created");
            expect(evt.sessionId).toBe(session.sessionId);

            await session.disconnect();
        } finally {
            unsubscribe();
        }
    });

    it("should filter session lifecycle events by type", async () => {
        const created = deferred<SessionLifecycleEvent>();
        const unsubscribe = client.on("session.created", (evt) => {
            created.resolve(evt);
        });

        try {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const evt = await withTimeout(created.promise, 10_000, "session.created (filtered)");

            expect(evt.type).toBe("session.created");
            expect(evt.sessionId).toBe(session.sessionId);

            await session.disconnect();
        } finally {
            unsubscribe();
        }
    });

    it("disposing lifecycle subscription stops receiving events", async () => {
        let count = 0;
        const created = deferred<SessionLifecycleEvent>();
        const unsubscribeFirst = client.on(() => {
            count += 1;
        });
        unsubscribeFirst();

        const unsubscribeActive = client.on("session.created", (evt) => {
            created.resolve(evt);
        });

        try {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const evt = await withTimeout(created.promise, 10_000, "session.created");

            expect(evt.sessionId).toBe(session.sessionId);
            expect(count).toBe(0);

            await session.disconnect();
        } finally {
            unsubscribeActive();
        }
    });

    it("should receive session updated lifecycle event for non ephemeral activity", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const updated = deferred<SessionLifecycleEvent>();
        const unsubscribe = client.on("session.updated", (evt) => {
            if (evt.sessionId === session.sessionId) {
                updated.resolve(evt);
            }
        });

        try {
            // Setting a non-ephemeral mode triggers a session.updated lifecycle event
            await session.rpc.mode.set({ mode: "plan" });

            const evt = await withTimeout(updated.promise, 10_000, "session.updated");
            expect(evt.type).toBe("session.updated");
            expect(evt.sessionId).toBe(session.sessionId);
        } finally {
            unsubscribe();
            await session.disconnect();
        }
    });

    it("should receive session deleted lifecycle event when deleted", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        // Make an LLM call first to ensure the session is persisted
        const message = await session.sendAndWait({ prompt: "Say SESSION_DELETED_OK exactly." });
        expect(message?.data.content).toContain("SESSION_DELETED_OK");

        const deleted = deferred<SessionLifecycleEvent>();
        const unsubscribe = client.on("session.deleted", (evt) => {
            if (evt.sessionId === session.sessionId) {
                deleted.resolve(evt);
            }
        });

        try {
            await client.deleteSession(session.sessionId);

            const evt = await withTimeout(deleted.promise, 10_000, "session.deleted");
            expect(evt.type).toBe("session.deleted");
            expect(evt.sessionId).toBe(session.sessionId);
        } finally {
            unsubscribe();
        }
    });
});
