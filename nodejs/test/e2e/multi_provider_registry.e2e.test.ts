/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type {
    CustomAgentConfig,
    NamedProviderConfig,
    ProviderModelConfig,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";
import type { ParsedHttpExchange } from "../../../test/harness/replayingCapiProxy";

/**
 * End-to-end coverage for the experimental multi-provider BYOK registry
 * (`providers` / `models` on the session config). Validates that several named
 * providers, several models per provider, and custom agents bound to those
 * provider-qualified models can coexist in one session, be launched, and route
 * inference to the configured provider with the configured wire model and
 * headers.
 */
describe("Multi-provider BYOK registry", async () => {
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext();

    async function waitForExchanges(minimumCount = 1): Promise<ParsedHttpExchange[]> {
        await retry(
            `capture ${minimumCount} chat completion request(s)`,
            async () => {
                const exchanges = await openAiEndpoint.getExchanges();
                expect(exchanges.length).toBeGreaterThanOrEqual(minimumCount);
            },
            1_200
        );
        return openAiEndpoint.getExchanges();
    }

    function getHeader(exchange: ParsedHttpExchange, name: string): string | undefined {
        const headers = exchange.requestHeaders ?? {};
        const key = Object.keys(headers).find((k) => k.toLowerCase() === name.toLowerCase());
        if (key === undefined) {
            return undefined;
        }
        const value = headers[key];
        return Array.isArray(value) ? value[0] : value;
    }

    // A heterogeneous registry: two providers of different types, with multiple
    // models each. Provider-qualified selection ids are alpha/sonnet,
    // alpha/haiku, beta/opus, beta/haiku.
    const registryProviders: NamedProviderConfig[] = [
        {
            name: "alpha",
            type: "openai",
            wireApi: "completions",
            baseUrl: "https://alpha.example.test/v1",
            apiKey: "alpha-secret",
            headers: { "X-Provider": "alpha" },
        },
        {
            name: "beta",
            type: "anthropic",
            baseUrl: "https://beta.example.test",
            bearerToken: "beta-bearer",
            headers: { "X-Provider": "beta" },
        },
    ];
    const registryModels: ProviderModelConfig[] = [
        { id: "sonnet", provider: "alpha", wireModel: "byok-gpt-4o", maxPromptTokens: 111111 },
        { id: "haiku", provider: "alpha", wireModel: "byok-gpt-4o-mini" },
        { id: "opus", provider: "beta", wireModel: "byok-claude-3-opus" },
        { id: "haiku", provider: "beta", wireModel: "byok-claude-3-haiku" },
    ];
    const registryAgents: CustomAgentConfig[] = [
        { name: "orchestrator", displayName: "Orchestrator", description: "Top-level planner.", prompt: "Plan and delegate.", model: "alpha/sonnet" },
        { name: "researcher", displayName: "Researcher", description: "Deep research subagent.", prompt: "Research thoroughly.", model: "beta/opus" },
        { name: "fast-helper", displayName: "Fast Helper", description: "Quick subagent.", prompt: "Answer quickly.", model: "alpha/haiku" },
        { name: "summarizer", displayName: "Summarizer", description: "Summarizing subagent.", prompt: "Summarize.", model: "beta/haiku" },
    ];

    it("should register multiple providers with custom agents bound to their models", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            providers: registryProviders,
            models: registryModels,
            customAgents: registryAgents,
        });

        try {
            const { agents } = await session.rpc.agent.list();

            // All four custom agents coexist in a single session.
            expect(agents.length).toBe(4);

            // Each agent is bound to its configured provider-qualified BYOK model.
            const byName = new Map(agents.map((a) => [a.name, a]));
            expect(byName.get("orchestrator")?.model).toBe("alpha/sonnet");
            expect(byName.get("researcher")?.model).toBe("beta/opus");
            expect(byName.get("fast-helper")?.model).toBe("alpha/haiku");
            expect(byName.get("summarizer")?.model).toBe("beta/haiku");

            // Models from BOTH providers are represented, proving the two
            // providers and their models coexist within the same session.
            const boundModels = agents.map((a) => a.model ?? "");
            expect(boundModels.some((m) => m.startsWith("alpha/"))).toBe(true);
            expect(boundModels.some((m) => m.startsWith("beta/"))).toBe(true);
        } finally {
            await session.disconnect();
        }
    });

    async function assertRouting(
        selectionId: string,
        expectedWireModel: string,
        expectedProviderHeader: string
    ): Promise<void> {
        // Two OpenAI-compatible providers, both pointed at the replay proxy so
        // their /chat/completions traffic is captured. They are distinguished on
        // the wire by their per-provider X-Provider header. "alpha" carries two
        // models (multiple models per provider); "delta" carries one.
        const providers: NamedProviderConfig[] = [
            {
                name: "alpha",
                type: "openai",
                wireApi: "completions",
                baseUrl: openAiEndpoint.url,
                apiKey: "alpha-secret",
                headers: { "X-Provider": "alpha" },
            },
            {
                name: "delta",
                type: "openai",
                wireApi: "completions",
                baseUrl: openAiEndpoint.url,
                apiKey: "delta-secret",
                headers: { "X-Provider": "delta" },
            },
        ];
        const models: ProviderModelConfig[] = [
            { id: "sonnet", provider: "alpha", wireModel: "byok-gpt-4o" },
            { id: "haiku", provider: "alpha", wireModel: "byok-gpt-4o-mini" },
            { id: "turbo", provider: "delta", wireModel: "byok-gpt-4-turbo" },
        ];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: selectionId,
            providers,
            models,
        });

        try {
            await session.sendAndWait({ prompt: "What is 5+5?" });
            const exchanges = await waitForExchanges();
            expect(exchanges.length).toBe(1);
            const exchange = exchanges[0];

            // The wire model sent to the provider is the selected model's
            // wireModel, not its provider-qualified selection id.
            expect(exchange.request.model).toBe(expectedWireModel);

            // The request carried the owning provider's custom header, proving
            // the turn was dispatched against the correct provider connection.
            expect(getHeader(exchange, "X-Provider")).toBe(expectedProviderHeader);

            // The provider's API key was applied as an Authorization header.
            expect(getHeader(exchange, "Authorization")).toBeTruthy();
        } finally {
            try {
                await session.disconnect();
            } catch {
                // disconnect may fail since the BYOK provider URL is fake
            }
        }
    }

    it("should route alpha sonnet turn to its provider and wire model", async () => {
        await assertRouting("alpha/sonnet", "byok-gpt-4o", "alpha");
    });

    it("should route alpha haiku turn to its provider and wire model", async () => {
        await assertRouting("alpha/haiku", "byok-gpt-4o-mini", "alpha");
    });

    it("should route delta turbo turn to its provider and wire model", async () => {
        await assertRouting("delta/turbo", "byok-gpt-4-turbo", "delta");
    });
});
