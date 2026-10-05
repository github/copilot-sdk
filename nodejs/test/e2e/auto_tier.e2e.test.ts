/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { getNextEventOfType } from "./harness/sdkTestHelper.js";

/**
 * The runtime stages an Auto routing preference instead of applying it immediately: a
 * request is "unclaimed" until a later turn using the `auto` model mints a usable model
 * and token pair. These tests observe that staged state through `model.getCurrent`, so
 * they assert what the runtime actually recorded rather than what the SDK serialized.
 */
describe("Auto tier switching", async () => {
    const { copilotClient: client, createClient } = await createSdkTestContext();

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
        const superseded = await session.setAutoTier("fast");
        expect(superseded.status).toBe("pending");
        expect(superseded.pendingAutoTier).toBe("fast");
        expect(superseded.supersededAutoTier).toBe("efficiency");
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBe("fast");

        const replacedFast = await session.setAutoTier("intelligence");
        expect(replacedFast.status).toBe("pending");
        expect(replacedFast.pendingAutoTier).toBe("intelligence");
        expect(replacedFast.supersededAutoTier).toBe("fast");
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
        await session.setModel("auto", { autoTier: "fast" });
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBe("fast");

        // Supplying null clears it. Omission, a value, and null are three distinct
        // outcomes, which is why the option cannot collapse to a plain optional field.
        await session.setModel("auto", { autoTier: null });
        expect((await session.rpc.model.getCurrent()).pendingAutoTier).toBeUndefined();

        await session.disconnect();
    });

    it("should restore and override fast auto tier on cold resume", async () => {
        const fastSession = await client.createSession({
            onPermissionRequest: approveAll,
            model: "auto",
            capi: { autoTier: "fast", enableWebSocketResponses: false },
        });
        const tierlessSession = await client.createSession({
            onPermissionRequest: approveAll,
            model: "auto",
            capi: { enableWebSocketResponses: false },
        });
        const fastSessionId = fastSession.sessionId;
        const tierlessSessionId = tierlessSession.sessionId;

        await fastSession.sendAndWait({
            prompt: "Reply with exactly AUTO_TIER_COLD_RESUME_READY.",
        });
        await tierlessSession.sendAndWait({
            prompt: "Reply with exactly AUTO_TIER_TIERLESS_READY.",
        });
        expect((await fastSession.rpc.model.getCurrent()).autoTier).toBe("fast");
        expect((await tierlessSession.rpc.model.getCurrent()).autoTier).toBeUndefined();

        await fastSession.disconnect();
        await tierlessSession.disconnect();
        await client.stop();

        const restoredClient = createClient();
        const restoredFast = await restoredClient.resumeSession(fastSessionId, {
            onPermissionRequest: approveAll,
        });
        const restoredTierless = await restoredClient.resumeSession(tierlessSessionId, {
            onPermissionRequest: approveAll,
        });
        expect((await restoredFast.rpc.model.getCurrent()).autoTier).toBe("fast");
        expect((await restoredTierless.rpc.model.getCurrent()).autoTier).toBeUndefined();
        await restoredFast.disconnect();
        await restoredTierless.disconnect();
        await restoredClient.stop();

        const overrideClient = createClient();
        const overridden = await overrideClient.resumeSession(fastSessionId, {
            onPermissionRequest: approveAll,
            model: "auto",
            capi: { autoTier: "balance", enableWebSocketResponses: false },
        });
        expect((await overridden.rpc.model.getCurrent()).autoTier).toBe("balance");
        await overridden.disconnect();
        await overrideClient.stop();
    }, 120_000);

    it("should commit fast auto tier after successful turn", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "auto",
            capi: { autoTier: "efficiency", enableWebSocketResponses: false },
        });

        const modelChangePromise = getNextEventOfType(session, "session.model_change");
        const staged = await session.setAutoTier("fast");
        expect(staged.status).toBe("pending");
        expect(staged.effectiveAutoTier).toBe("efficiency");
        expect(staged.pendingAutoTier).toBe("fast");

        const beforeTurn = await session.rpc.model.getCurrent();
        expect(beforeTurn.autoTier).toBe("efficiency");
        expect(beforeTurn.pendingAutoTier).toBe("fast");

        await session.sendAndWait({
            prompt: "Reply with exactly AUTO_TIER_FAST_COMMITTED.",
        });

        const modelChange = await modelChangePromise;
        expect(modelChange.data.previousModel).toBe("auto");
        expect(modelChange.data.newModel).toBe("auto");
        expect(modelChange.data.previousAutoTier).toBe("efficiency");
        expect(modelChange.data.autoTier).toBe("fast");

        const committed = await session.rpc.model.getCurrent();
        expect(committed.autoTier).toBe("fast");
        expect(committed.pendingAutoTier).toBeUndefined();
        expect(committed.activatingAutoTier).toBeUndefined();

        await session.disconnect();
    }, 120_000);

    it("should preserve effective tier when fast activation fails", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "auto",
            capi: { autoTier: "efficiency", enableWebSocketResponses: false },
        });
        const sessionId = session.sessionId;

        await session.sendAndWait({
            prompt: "Reply with exactly AUTO_TIER_INITIAL_READY.",
        });

        let fastCommitted = false;
        const unsubscribeModelChange = session.on("session.model_change", (event) => {
            fastCommitted ||= event.data.autoTier === "fast";
        });
        const failurePromise = getNextEventOfType(session, "session.auto_tier_switch_failed");

        const staged = await session.setAutoTier("fast");
        expect(staged.status).toBe("pending");
        expect(staged.effectiveAutoTier).toBe("efficiency");
        expect(staged.pendingAutoTier).toBe("fast");

        await session.sendAndWait({
            prompt: "Reply with exactly AUTO_TIER_FAILURE_RECOVERED.",
        });

        const failure = await failurePromise;
        expect(failure.ephemeral).toBe(true);
        expect(failure.data.effectiveAutoTier).toBe("efficiency");
        expect(failure.data.requestedAutoTier).toBe("fast");
        expect(failure.data.reason).toBe("request_failed");
        expect(fastCommitted).toBe(false);

        const current = await session.rpc.model.getCurrent();
        expect(current.autoTier).toBe("efficiency");
        expect(current.pendingAutoTier).toBeUndefined();
        expect(current.activatingAutoTier).toBeUndefined();

        unsubscribeModelChange();
        await session.disconnect();
        await client.stop();

        const resumedClient = createClient();
        const resumed = await resumedClient.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
        });
        const resumedCurrent = await resumed.rpc.model.getCurrent();
        expect(resumedCurrent.autoTier).toBe("efficiency");
        expect(resumedCurrent.pendingAutoTier).toBeUndefined();
        expect(resumedCurrent.activatingAutoTier).toBeUndefined();

        const persisted = await resumed.rpc.eventLog.read({ max: 100, waitMs: 0 });
        expect(
            persisted.events.some((event) => event.type === "session.auto_tier_switch_failed")
        ).toBe(false);
        await resumed.disconnect();
        await resumedClient.stop();
    }, 120_000);
});
