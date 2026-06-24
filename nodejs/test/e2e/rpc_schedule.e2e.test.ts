/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Session schedule RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should list no schedules for fresh session", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const result = await session.rpc.schedule.list();
            expect(result.entries).toEqual([]);
        } finally {
            await session.disconnect();
        }
    });

    it("should return undefined entry when stopping unknown schedule", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const result = await session.rpc.schedule.stop({ id: Number.MAX_SAFE_INTEGER });
            expect(result.entry).toBeUndefined();
            expect((await session.rpc.schedule.list()).entries).toEqual([]);
        } finally {
            await session.disconnect();
        }
    });
});
