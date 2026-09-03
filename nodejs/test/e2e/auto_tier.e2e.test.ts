/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

/**
 * The runtime stages an Auto routing preference instead of applying it immediately: a
 * request is "unclaimed" until a later turn using the `auto` model mints a usable model
 * and token pair. These tests observe that staged state through `model.getCurrent`, so
 * they assert what the runtime actually recorded rather than what the SDK serialized.
 */
describe("Auto tier switching", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should stage and reset auto tier preference", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "auto",
        });

        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBeUndefined();

        const staged = await session.setAutoTier("efficiency");
        expect(staged.status).toBe("pending");
        expect(staged.pendingAutoTier).toBe("efficiency");
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBe("efficiency");

        // A second request replaces the first and reports the one it displaced.
        const superseded = await session.setAutoTier("intelligence");
        expect(superseded.status).toBe("pending");
        expect(superseded.pendingAutoTier).toBe("intelligence");
        expect(superseded.supersededAutoTier).toBe("efficiency");
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBe("intelligence");

        // Passing null returns the session to provider-default routing. The status is
        // `unchanged` because provider-default was already the committed preference;
        // the request's effect is cancelling the staged one.
        const reset = await session.setAutoTier(null);
        expect(reset.status).toBe("unchanged");
        expect(reset.supersededAutoTier).toBe("intelligence");
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBeUndefined();

        await session.disconnect();
    });

    it("should preserve auto tier when set model omits it", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "auto",
        });

        await session.setAutoTier("balance");
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBe("balance");

        // Omitting the option leaves the staged preference alone.
        await session.setModel("auto");
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBe("balance");

        // Supplying a tier replaces it.
        await session.setModel("auto", { autoTier: "intelligence" });
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBe("intelligence");

        // Supplying null clears it. Omission, a value, and null are three distinct
        // outcomes, which is why the option cannot collapse to a plain optional field.
        await session.setModel("auto", { autoTier: null });
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBeUndefined();

        await session.disconnect();
    });
});
