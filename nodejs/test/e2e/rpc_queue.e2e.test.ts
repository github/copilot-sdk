/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { describe, expect, it } from "vitest";
import { approveAll, type SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

describe("Session queue RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    async function expectQueueEmpty(session: Awaited<ReturnType<typeof client.createSession>>) {
        const pending = await session.rpc.queue.pendingItems();
        expect(pending.items).toEqual([]);
        expect(pending.steeringMessages).toEqual([]);
    }

    function isPendingCommand(
        item: { kind: string; displayText: string },
        command: string
    ): boolean {
        return (
            item.kind === "command" &&
            (item.displayText === command || item.displayText.includes(command.replace(/^\//, "")))
        );
    }

    it("fresh queue is empty and empty mutations are no-ops", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await expectQueueEmpty(session);

            expect((await session.rpc.queue.removeMostRecent()).removed).toBe(false);
            await expectQueueEmpty(session);

            await session.rpc.queue.clear();
            await expectQueueEmpty(session);

            expect((await session.rpc.queue.removeMostRecent()).removed).toBe(false);
            await expectQueueEmpty(session);
        } finally {
            await session.disconnect();
        }
    });

    it("pendingItems reports queued command and remove and clear update queue", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        let firstEvent: Extract<SessionEvent, { type: "command.queued" }> | undefined;
        let respondedToFirst = false;
        const interest = await session.rpc.eventLog.registerInterest({
            eventType: "command.queued",
        });
        try {
            const firstCommand = `/sdk-queue-first-${randomUUID()}`;
            const secondCommand = `/sdk-queue-second-${randomUUID()}`;
            const thirdCommand = `/sdk-queue-third-${randomUUID()}`;
            const firstQueued = new Promise<Extract<SessionEvent, { type: "command.queued" }>>(
                (resolve) => {
                    session.on((event) => {
                        if (
                            event.type === "command.queued" &&
                            event.data.command === firstCommand
                        ) {
                            resolve(event);
                        }
                    });
                }
            );

            expect((await session.rpc.commands.enqueue({ command: firstCommand })).queued).toBe(
                true
            );
            firstEvent = await firstQueued;

            expect((await session.rpc.commands.enqueue({ command: secondCommand })).queued).toBe(
                true
            );
            await waitForCondition(
                async () =>
                    (await session.rpc.queue.pendingItems()).items.some((item) =>
                        isPendingCommand(item, secondCommand)
                    ),
                { timeoutMessage: `Timed out waiting for ${secondCommand} in queue.` }
            );

            expect((await session.rpc.queue.removeMostRecent()).removed).toBe(true);
            await waitForCondition(
                async () =>
                    !(await session.rpc.queue.pendingItems()).items.some((item) =>
                        isPendingCommand(item, secondCommand)
                    ),
                { timeoutMessage: `Timed out waiting for ${secondCommand} to leave queue.` }
            );

            expect((await session.rpc.commands.enqueue({ command: thirdCommand })).queued).toBe(
                true
            );
            await waitForCondition(
                async () =>
                    (await session.rpc.queue.pendingItems()).items.some((item) =>
                        isPendingCommand(item, thirdCommand)
                    ),
                { timeoutMessage: `Timed out waiting for ${thirdCommand} in queue.` }
            );

            await session.rpc.queue.clear();
            await waitForCondition(
                async () =>
                    !(await session.rpc.queue.pendingItems()).items.some((item) =>
                        isPendingCommand(item, thirdCommand)
                    ),
                { timeoutMessage: `Timed out waiting for ${thirdCommand} to leave queue.` }
            );

            const completed = await session.rpc.commands.respondToQueuedCommand({
                requestId: firstEvent.data.requestId,
                result: { handled: true, stopProcessingQueue: true },
            });
            respondedToFirst = completed.success;
            expect(completed.success).toBe(true);

            await waitForCondition(
                async () => {
                    const pending = await session.rpc.queue.pendingItems();
                    return pending.items.length === 0 && pending.steeringMessages.length === 0;
                },
                { timeoutMessage: "Timed out waiting for queue to empty." }
            );
        } finally {
            if (!respondedToFirst && firstEvent) {
                await session.rpc.commands.respondToQueuedCommand({
                    requestId: firstEvent.data.requestId,
                    result: { handled: true, stopProcessingQueue: true },
                });
            }
            await session.rpc.queue.clear();
            await session.rpc.eventLog.releaseInterest({ handle: interest.handle });
            await session.disconnect();
        }
    });
});
