/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
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
            const tail = await session.rpc.eventLog.tail();
            await session.log("Ephemeral event after tail", { ephemeral: true });
            const request = {
                cursor: tail.cursor,
                max: 10,
                waitMs: 0,
                includeEphemeral: false,
            };
            const read = await session.rpc.eventLog.read(request);

            expect(tail.cursor.trim()).toBeTruthy();
            expect(read.cursorStatus).toBe("ok");
            expect(read.events).toEqual([]);
            expect(read.hasMore).toBe(false);

            await session.rpc.plan.update({ content: "# Durable event after tail" });
            await client.rpc.sessions.save({ sessionId: session.sessionId });
            const durableRead = await session.rpc.eventLog.read(request);
            expect(durableRead.cursorStatus).toBe("ok");
            expect(durableRead.events).toContainEqual(
                expect.objectContaining({
                    type: "session.plan_changed",
                    data: expect.objectContaining({ operation: "create" }),
                })
            );
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

            await session.rpc.plan.update({ content: "# Unrelated event during a filtered read" });
            let read = await readTask;
            expect(read.events).toEqual([]);
            await session.rpc.name.set({ name: expectedTitle });

            // An unrelated event can wake a filtered read with an empty page.
            await waitForCondition(
                async () => {
                    expect(read.cursorStatus).toBe("ok");
                    expect(
                        read.events.every((event) => event.type === "session.title_changed")
                    ).toBe(true);
                    if (
                        read.events.some(
                            (event) =>
                                event.type === "session.title_changed" &&
                                event.data.title === expectedTitle
                        )
                    ) {
                        return true;
                    }
                    read = await session.rpc.eventLog.read({
                        cursor: read.cursor,
                        max: 10,
                        waitMs: 5_000,
                        types: ["session.title_changed"],
                    });
                    return false;
                },
                { timeoutMessage: "Timed out waiting for the filtered title changed event." }
            );

            expect(read.events.length).toBeGreaterThan(0);
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
