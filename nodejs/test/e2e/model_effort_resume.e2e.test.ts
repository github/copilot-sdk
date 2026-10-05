/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "node:fs/promises";
import { join } from "node:path";
import { describe, expect, it, onTestFinished } from "vitest";
import { approveAll, RuntimeConnection } from "../../src/index.js";
import type { NamedProviderConfig, ProviderModelConfig } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Model-bound effort across runtime restart", async () => {
    const { createClient, openAiEndpoint, env } = await createSdkTestContext();

    it.each([
        { override: undefined, replacement: "switch", plan: false },
        { override: undefined, replacement: "policy", plan: false },
        { override: "rpc", replacement: "policy", plan: false },
        { override: "options", replacement: "switch", plan: false },
        { override: "options", replacement: "options", plan: false },
        { override: "combined", replacement: "switch", plan: false },
        { override: "model-only", replacement: "options", plan: false },
        { override: undefined, replacement: "switch", plan: true },
        { override: "rpc", replacement: "switch", plan: true },
    ] as const)(
        "restores ownership with $override effort override, $replacement replacement, and plan=$plan",
        async ({ override, replacement, plan }) => {
            const providers: NamedProviderConfig[] = [
                {
                    name: "effort-resume",
                    type: "openai",
                    wireApi: "completions",
                    baseUrl: openAiEndpoint.url,
                },
            ];
            const models: ProviderModelConfig[] = [
                { id: "bound", provider: "effort-resume", modelId: "gpt-5.4-mini" },
                { id: "replacement", provider: "effort-resume", modelId: "gpt-5.4-mini" },
            ];
            const customAgents = [
                {
                    name: "cold-resume-agent",
                    prompt: "Respond concisely.",
                    model: "effort-resume/bound:defaultReasoningEffort=high",
                },
            ];
            // Pin child-process transport: recreating an in-process client is not a cold restart.
            const originalClient = createClient({ connection: RuntimeConnection.forStdio() });
            onTestFinished(async () => {
                expect(await originalClient.stop()).toHaveLength(0);
            });
            const fixtureSession = await originalClient.createSession({
                onPermissionRequest: approveAll,
                model: "effort-resume/bound",
                providers,
                models,
                customAgents,
            });
            // Create workspace metadata without a model turn; this does not enable journal persistence.
            await fixtureSession.rpc.name.set({
                name: `effort-resume-${fixtureSession.sessionId}`,
            });
            await fixtureSession.rpc.agent.select({ name: "cold-resume-agent" });
            await fixtureSession.rpc.model.switchTo({
                modelId: "effort-resume/bound",
                reasoningEffort: "high",
                source: "agent",
            });
            const fixtureEvents = await fixtureSession.getEvents();
            expect(fixtureEvents[0]?.type).toBe("session.start");
            const transported = fixtureEvents
                .filter((event) => event.type === "session.model_change")
                .at(-1);
            expect(transported?.data.reasoningEffort).toBe("high");
            expect(transported?.data.source).toBe("sdk");
            expect(transported?.data.reasoningEffortModel).toBeUndefined();
            await originalClient.rpc.sessions.save({ sessionId: fixtureSession.sessionId });
            await originalClient.rpc.sessions.close({ sessionId: fixtureSession.sessionId });
            expect(await originalClient.stop()).toHaveLength(0);

            // SDK callers cannot claim agent provenance, and a no-turn session has no journal.
            // Seed its emitted history and agent receipt only after the writer stops.
            const eventsPath = join(
                env.COPILOT_HOME,
                "session-state",
                fixtureSession.sessionId,
                "events.jsonl"
            );
            const authored = fixtureEvents
                .filter((event) => event.type === "session.model_change")
                .at(-1);
            if (!authored) {
                throw new Error("The runtime did not emit the model switch fixture");
            }
            expect(authored.data.reasoningEffort).toBe("high");
            authored.data.source = "agent";
            authored.data.reasoningEffortModel = "effort-resume/bound";
            await writeFile(
                eventsPath,
                `${fixtureEvents.map((event) => JSON.stringify(event)).join("\n")}\n`
            );

            const boundClient = createClient({ connection: RuntimeConnection.forStdio() });
            onTestFinished(async () => {
                expect(await boundClient.stop()).toHaveLength(0);
            });
            const session = await boundClient.resumeSession(fixtureSession.sessionId, {
                onPermissionRequest: approveAll,
                providers,
                models,
                customAgents,
            });
            const bound = (await session.getEvents())
                .filter((event) => event.type === "session.model_change")
                .at(-1);
            expect(bound?.data.reasoningEffortModel).toBe("effort-resume/bound");
            if (override === "rpc") {
                await session.rpc.model.setReasoningEffort({ reasoningEffort: "high" });
            } else if (override === "options") {
                await session.rpc.options.update({ reasoningEffort: "high" });
            } else if (override === "combined") {
                await session.rpc.options.update({
                    model: "effort-resume/replacement",
                    reasoningEffort: "high",
                });
            } else if (override === "model-only") {
                await session.rpc.options.update({ model: "effort-resume/bound" });
                expect((await session.rpc.model.getCurrent()).reasoningEffort).toBe("high");
                const unchanged = (await session.getEvents())
                    .filter((event) => event.type === "session.model_change")
                    .at(-1);
                expect(unchanged?.data.reasoningEffortModel).toBe("effort-resume/bound");
                await session.rpc.options.update({ model: "effort-resume/replacement" });
            }
            const updatedModel =
                override === "combined" || override === "model-only"
                    ? "effort-resume/replacement"
                    : "effort-resume/bound";
            const expectedEffort = override === "model-only" ? undefined : "high";
            const updatedControls = await session.rpc.model.getCurrent();
            expect(updatedControls.modelId).toBe(updatedModel);
            expect(updatedControls.reasoningEffort).toBe(expectedEffort);
            const beforeRestart = (await session.getEvents())
                .filter((event) => event.type === "session.model_change")
                .at(-1);
            expect(beforeRestart?.data.reasoningEffort).toBe(expectedEffort ?? null);
            expect(beforeRestart?.data.newModel).toBe(updatedModel);
            expect(beforeRestart?.data.reasoningEffortModel).toBe(
                override === undefined ? "effort-resume/bound" : undefined
            );
            const planSelection = {
                planModelConfigured: true,
                planModel: "effort-resume/replacement",
                planReasoningEffort: "low",
            };
            if (plan) {
                await session.rpc.mode.set({ mode: "plan", ...planSelection });
                expect(await session.rpc.mode.get()).toBe("plan");
                expect(await session.rpc.model.getCurrent()).toMatchObject({
                    modelId: "effort-resume/replacement",
                    reasoningEffort: "low",
                });
            }
            await boundClient.rpc.sessions.save({ sessionId: session.sessionId });
            await boundClient.rpc.sessions.close({ sessionId: session.sessionId });
            expect(await boundClient.stop()).toHaveLength(0);

            const resumedClient = createClient({ connection: RuntimeConnection.forStdio() });
            onTestFinished(async () => {
                expect(await resumedClient.stop()).toHaveLength(0);
            });
            const resumed = await resumedClient.resumeSession(session.sessionId, {
                onPermissionRequest: approveAll,
                providers,
                models,
                customAgents,
            });
            if (plan) {
                expect(await resumed.rpc.mode.get()).toBe("plan");
                await resumed.rpc.mode.set({ mode: "interactive", ...planSelection });
                expect(await resumed.rpc.mode.get()).toBe("interactive");
            }
            const restoredControls = await resumed.rpc.model.getCurrent();
            expect(restoredControls.modelId).toBe(updatedModel);
            expect(restoredControls.reasoningEffort).toBe(expectedEffort);
            if (override === "model-only") {
                await resumed.rpc.options.update({ model: "effort-resume/bound" });
                const revertedControls = await resumed.rpc.model.getCurrent();
                expect(revertedControls.modelId).toBe("effort-resume/bound");
                expect(revertedControls.reasoningEffort).toBeUndefined();
            }
            if (replacement === "policy") {
                await resumed.rpc.model.setAllowedModels({
                    allowedModels: ["effort-resume/replacement"],
                });
            } else if (replacement === "options") {
                await resumed.rpc.options.update({ model: "effort-resume/replacement" });
            } else {
                await resumed.rpc.model.switchTo({ modelId: "effort-resume/replacement" });
            }
            const replacementEffort =
                override === undefined || override === "model-only" ? undefined : "high";
            const replacedControls = await resumed.rpc.model.getCurrent();
            expect(replacedControls.modelId).toBe("effort-resume/replacement");
            expect(replacedControls.reasoningEffort).toBe(replacementEffort);
            const replaced = (await resumed.getEvents())
                .filter((event) => event.type === "session.model_change")
                .at(-1);
            expect(replaced?.data.newModel).toBe("effort-resume/replacement");
            expect(replaced?.data.reasoningEffort).toBe(replacementEffort ?? null);
            expect(replaced?.data.reasoningEffortModel).toBeUndefined();
            await resumedClient.rpc.sessions.close({ sessionId: resumed.sessionId });
            expect(await resumedClient.stop()).toHaveLength(0);
            expect(
                (await openAiEndpoint.getRequests()).filter((request) =>
                    /\/(?:completions|responses|messages)(?:\?|$)/.test(request.url)
                )
            ).toHaveLength(0);
        }
    );
});
