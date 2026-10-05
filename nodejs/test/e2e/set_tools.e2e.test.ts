/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool } from "../../src/index.js";
import type { ParsedHttpExchange } from "../../../test/harness/replayingCapiProxy";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

// These scenarios, prompts, and tool results are shared with the other SDKs'
// set_tools E2E tests, which replay the same snapshots.
const FRUIT_PROMPT = "Use lookup_fruit to find the fruit for code 42.";
const FRUIT_AND_VEGETABLE_PROMPT =
    "Use lookup_fruit to find the fruit for code 42 again, and use lookup_vegetable to find the vegetable for code 7.";
const VEGETABLE_PROMPT = "Use lookup_vegetable to find the vegetable for code 7.";

function lookupFruit(fruit: string, calls: number[] = []) {
    return defineTool("lookup_fruit", {
        description: "Looks up the fruit for a numeric code",
        parameters: z.object({ code: z.number().int().describe("Fruit code") }),
        handler: ({ code }) => {
            calls.push(code);
            return fruit;
        },
    });
}

function lookupVegetable(calls: number[] = []) {
    return defineTool("lookup_vegetable", {
        description: "Looks up the vegetable for a numeric code",
        parameters: z.object({ code: z.number().int().describe("Vegetable code") }),
        handler: ({ code }) => {
            calls.push(code);
            return "carrot";
        },
    });
}

function offeredTools(exchange: ParsedHttpExchange): string[] {
    return (exchange.request.tools ?? []).map((tool) =>
        "function" in tool ? tool.function.name : ""
    );
}

/** Whether the exchange's request carries `prompt` as a user message. */
function includesPrompt(exchange: ParsedHttpExchange, prompt: string): boolean {
    return exchange.request.messages.some(
        (message) =>
            message.role === "user" &&
            (typeof message.content === "string"
                ? message.content.includes(prompt)
                : JSON.stringify(message.content).includes(prompt))
    );
}

describe("Live tool replacement", async () => {
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext();

    it("replaces tools on a created session", async () => {
        const originalLookups: number[] = [];
        const replacementLookups: number[] = [];
        const vegetableLookups: number[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                lookupFruit("apple", originalLookups),
                defineTool("retired_lookup", {
                    description: "Looks up a retired value",
                    handler: () => "retired",
                }),
            ],
        });

        const first = await session.sendAndWait({ prompt: FRUIT_PROMPT });
        expect(first?.data.content).toContain("apple");

        await session.setTools([
            lookupFruit("dragonfruit", replacementLookups),
            lookupVegetable(vegetableLookups),
        ]);

        const second = await session.sendAndWait({ prompt: FRUIT_AND_VEGETABLE_PROMPT });
        expect(second?.data.content).toContain("dragonfruit");
        expect(second?.data.content).toContain("carrot");
        expect(originalLookups).toEqual([42]);
        expect(replacementLookups).toEqual([42]);
        expect(vegetableLookups).toEqual([7]);

        // Model requests after the replacement offer exactly the new tool set.
        const exchanges = await openAiEndpoint.getExchanges();
        const replacedFrom = exchanges.findIndex((exchange) =>
            includesPrompt(exchange, FRUIT_AND_VEGETABLE_PROMPT)
        );
        expect(replacedFrom).toBeGreaterThan(0);
        for (const exchange of exchanges.slice(0, replacedFrom)) {
            expect(offeredTools(exchange)).toEqual(
                expect.arrayContaining(["lookup_fruit", "retired_lookup"])
            );
            expect(offeredTools(exchange)).not.toContain("lookup_vegetable");
        }
        for (const exchange of exchanges.slice(replacedFrom)) {
            expect(offeredTools(exchange)).toEqual(
                expect.arrayContaining(["lookup_fruit", "lookup_vegetable"])
            );
            expect(offeredTools(exchange)).not.toContain("retired_lookup");
        }

        await session.disconnect();
    });

    it("replaces tools on a resumed session", async () => {
        const createdLookups: number[] = [];
        const created = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [lookupFruit("apple", createdLookups)],
        });
        const sessionId = created.sessionId;
        const first = await created.sendAndWait({ prompt: FRUIT_PROMPT });
        expect(first?.data.content).toContain("apple");
        expect(createdLookups).toEqual([42]);
        await created.disconnect();

        const fruitLookups: number[] = [];
        const vegetableLookups: number[] = [];
        const resumed = await client.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            tools: [lookupFruit("apple", fruitLookups)],
        });
        await resumed.setTools([lookupVegetable(vegetableLookups)]);

        const answer = await resumed.sendAndWait({ prompt: VEGETABLE_PROMPT });
        expect(answer?.data.content).toContain("carrot");
        expect(vegetableLookups).toEqual([7]);
        expect(fruitLookups).toEqual([]);

        const exchanges = await openAiEndpoint.getExchanges();
        const replacedFrom = exchanges.findIndex((exchange) =>
            includesPrompt(exchange, VEGETABLE_PROMPT)
        );
        expect(replacedFrom).toBeGreaterThan(0);
        for (const exchange of exchanges.slice(replacedFrom)) {
            expect(offeredTools(exchange)).toContain("lookup_vegetable");
            expect(offeredTools(exchange)).not.toContain("lookup_fruit");
        }

        await resumed.disconnect();
    });

    it("keeps the previous tools when a replacement is rejected", async () => {
        const originalLookups: number[] = [];
        const replacementLookups: number[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [lookupFruit("apple", originalLookups)],
        });

        await expect(
            session.setTools([
                lookupFruit("dragonfruit", replacementLookups),
                defineTool("invalid.tool", {
                    description: "Has a name the runtime rejects",
                    handler: () => "never",
                }),
            ])
        ).rejects.toThrow();

        const answer = await session.sendAndWait({ prompt: FRUIT_PROMPT });
        expect(answer?.data.content).toContain("apple");
        expect(originalLookups).toEqual([42]);
        expect(replacementLookups).toEqual([]);

        await session.disconnect();
    });

    it("removes all client tools with an empty set", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [lookupFruit("apple")],
        });

        await session.setTools([]);

        const answer = await session.sendAndWait({ prompt: "Reply with exactly OK." });
        expect(answer?.data.content).toContain("OK");

        // The client's tools are gone, while the runtime's built-in tools remain.
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThan(0);
        for (const exchange of exchanges) {
            expect(offeredTools(exchange)).not.toContain("lookup_fruit");
            expect(offeredTools(exchange)).toContain("view");
        }

        await session.disconnect();
    });
});
