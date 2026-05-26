/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { describe, expect, it } from "vitest";
import { approveAll, type SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

describe("Session event log RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should read persisted events from the beginning", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.rpc.plan.update({ content: "# Event log E2E plan\n- persisted event" });

            let read: Awaited<ReturnType<typeof session.rpc.eventLog.read>> | undefined;
            await waitForCondition(
                async () => {
                    read = await session.rpc.eventLog.read({ max: 100, waitMs: 0 });
                    return read.events.some(
                        (event) =>
                            event.type === "session.plan_changed" &&
                            event.data.operation === "create" &&
                            event.ephemeral !== true
                    );
                },
                {
                    timeoutMessage:
                        "Timed out waiting for session.eventLog.read to return the persisted session.plan_changed event.",
                }
            );

            expect(read).toBeDefined();
            expect(read!.cursorStatus).toBe("ok");
            expect(read!.cursor.trim()).toBeTruthy();
            expect(read!.events).toContainEqual(
                expect.objectContaining({
                    type: "session.plan_changed",
                    data: expect.objectContaining({ operation: "create" }),
                })
            );
        } finally {
            await session.disconnect();
        }
    });

    it("should return tail cursor and read empty when no new events", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            let tail: Awaited<ReturnType<typeof session.rpc.eventLog.tail>> | undefined;
            let read: Awaited<ReturnType<typeof session.rpc.eventLog.read>> | undefined;
            await waitForCondition(
                async () => {
                    tail = await session.rpc.eventLog.tail();
                    read = await session.rpc.eventLog.read({
                        cursor: tail.cursor,
                        max: 10,
                        waitMs: 0,
                    });
                    return read.cursorStatus === "ok" && read.events.length === 0;
                },
                {
                    timeoutMessage:
                        "Timed out waiting for a stable event-log tail cursor with no immediately available events.",
                }
            );

            expect(tail!.cursor.trim()).toBeTruthy();
            expect(read!.events).toEqual([]);
            expect(read!.hasMore).toBe(false);
        } finally {
            await session.disconnect();
        }
    });

    it("should register and release event interest idempotently", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const registered = await session.rpc.eventLog.registerInterest({
                eventType: "session.title_changed",
            });
            expect(registered.handle.trim()).toBeTruthy();

            const released = await session.rpc.eventLog.releaseInterest({
                handle: registered.handle,
            });
            expect(released.success).toBe(true);

            const releasedAgain = await session.rpc.eventLog.releaseInterest({
                handle: registered.handle,
            });
            expect(releasedAgain.success).toBe(true);
        } finally {
            await session.disconnect();
        }
    });

    it("should long-poll with types filter for title changed event", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const expectedTitle = `EventLogTitle-${randomUUID()}`;
            const tail = await session.rpc.eventLog.tail();
            const readTask = session.rpc.eventLog.read({
                cursor: tail.cursor,
                max: 10,
                waitMs: 5_000,
                types: ["session.title_changed"],
            });

            await session.rpc.name.set({ name: expectedTitle });
            const read = await readTask;

            expect(read.cursorStatus).toBe("ok");
            expect(read.events.length).toBeGreaterThan(0);
            expect(
                read.events.every((event: SessionEvent) => event.type === "session.title_changed")
            ).toBe(true);
            expect(read.events).toContainEqual(
                expect.objectContaining({
                    type: "session.title_changed",
                    data: expect.objectContaining({ title: expectedTitle }),
                })
            );
        } finally {
            await session.disconnect();
        }
    });
});
