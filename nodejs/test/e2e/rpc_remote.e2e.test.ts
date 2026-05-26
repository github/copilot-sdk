/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

describe("Session remote RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    async function expectImplemented(
        action: () => Promise<unknown>,
        method: string
    ): Promise<unknown> {
        try {
            return await action();
        } catch (err: unknown) {
            const text = err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
            expect(text.toLowerCase()).not.toContain(`unhandled method ${method.toLowerCase()}`);
            return undefined;
        }
    }

    it("should treat remote off as no-op or implemented error", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const result = (await expectImplemented(
                () => session.rpc.remote.enable({ mode: "off" }),
                "session.remote.enable"
            )) as Awaited<ReturnType<typeof session.rpc.remote.enable>> | undefined;

            if (result) {
                expect(result.remoteSteerable).toBe(false);
                expect(result.url ?? "").toBe("");
            }
        } finally {
            await session.disconnect();
        }
    });

    it("should treat remote disable as no-op or implemented error", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await expectImplemented(() => session.rpc.remote.disable(), "session.remote.disable");
        } finally {
            await session.disconnect();
        }
    });

    it("should notify steerable changed event and persist flag", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.rpc.remote.notifySteerableChanged({ remoteSteerable: true });
            await waitForCondition(
                async () =>
                    (await session.getEvents()).some(
                        (event) =>
                            event.type === "session.remote_steerable_changed" &&
                            event.data.remoteSteerable === true
                    ),
                { timeoutMessage: "Timed out waiting for remote steerable=true event." }
            );
            expect(
                (
                    await client.rpc.sessions.getPersistedRemoteSteerable({
                        sessionId: session.sessionId,
                    })
                ).remoteSteerable
            ).toBe(true);

            await session.rpc.remote.notifySteerableChanged({ remoteSteerable: false });
            await waitForCondition(
                async () =>
                    (await session.getEvents()).some(
                        (event) =>
                            event.type === "session.remote_steerable_changed" &&
                            event.data.remoteSteerable === false
                    ),
                { timeoutMessage: "Timed out waiting for remote steerable=false event." }
            );
            expect(
                (
                    await client.rpc.sessions.getPersistedRemoteSteerable({
                        sessionId: session.sessionId,
                    })
                ).remoteSteerable
            ).toBe(false);
        } finally {
            await session.disconnect();
        }
    });
});
