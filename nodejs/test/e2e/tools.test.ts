/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "fs/promises";
import { join } from "path";
import { assert, describe, expect, it } from "vitest";
import { z } from "zod";
import { defineTool } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Custom tools", async () => {
    const { copilotClient: client, openAiEndpoint, workDir } = await createSdkTestContext();

    it("invokes built-in tools", async () => {
        await writeFile(join(workDir, "README.md"), "# ELIZA, the only chatbot you'll ever need");

        const session = await client.createSession();
        const assistantMessage = await session.sendAndWait({
            prompt: "What's the first line of README.md in this directory?",
        });
        expect(assistantMessage?.data.content).toContain("ELIZA");
    });

    it("invokes custom tool", async () => {
        const session = await client.createSession({
            tools: [
                defineTool("encrypt_string", {
                    description: "Encrypts a string",
                    parameters: z.object({
                        input: z.string().describe("String to encrypt"),
                    }),
                    handler: ({ input }) => input.toUpperCase(),
                }),
            ],
        });

        const assistantMessage = await session.sendAndWait({
            prompt: "Use encrypt_string to encrypt this string: Hello",
        });
        expect(assistantMessage?.data.content).toContain("HELLO");
    });

    it("handles tool calling errors", async () => {
        const session = await client.createSession({
            tools: [
                defineTool("get_user_location", {
                    description: "Gets the user's location",
                    handler: () => {
                        throw new Error("Melbourne");
                    },
                }),
            ],
        });

        const answer = await session.sendAndWait({
            prompt: "What is my location? If you can't find out, just say 'unknown'.",
        });

        // Check the underlying traffic
        const traffic = await openAiEndpoint.getExchanges();
        const lastConversation = traffic[traffic.length - 1];

        const toolCalls = lastConversation.request.messages.flatMap((m) =>
            m.role === "assistant" ? m.tool_calls : []
        );
        expect(toolCalls.length).toBe(1);
        const toolCall = toolCalls[0]!;
        assert(toolCall.type === "function");
        expect(toolCall.function.name).toBe("get_user_location");

        const toolResults = lastConversation.request.messages.filter((m) => m.role === "tool");
        expect(toolResults.length).toBe(1);
        const toolResult = toolResults[0]!;
        expect(toolResult.tool_call_id).toBe(toolCall.id);
        expect(toolResult.content).not.toContain("Melbourne");

        // Importantly, we're checking that the assistant does not see the
        // exception information as if it was the tool's output.
        expect(answer?.data.content).not.toContain("Melbourne");
        expect(answer?.data.content?.toLowerCase()).toContain("unknown");
    });

    it("can receive and return complex types", async () => {
        const session = await client.createSession({
            tools: [
                defineTool("db_query", {
                    description: "Performs a database query",
                    parameters: z.object({
                        query: z.object({
                            table: z.string(),
                            ids: z.array(z.number()),
                            sortAscending: z.boolean(),
                        }),
                    }),
                    handler: ({ query }, invocation) => {
                        expect(query.table).toBe("cities");
                        expect(query.ids).toEqual([12, 19]);
                        expect(query.sortAscending).toBe(true);
                        expect(invocation.sessionId).toBe(session.sessionId);

                        return [
                            { countryId: 19, cityName: "Passos", population: 135460 },
                            { countryId: 12, cityName: "San Lorenzo", population: 204356 },
                        ];
                    },
                }),
            ],
        });

        const assistantMessage = await session.sendAndWait({
            prompt:
                "Perform a DB query for the 'cities' table using IDs 12 and 19, sorting ascending. " +
                "Reply only with lines of the form: [cityname] [population]",
        });

        const responseContent = assistantMessage?.data.content!;
        expect(assistantMessage).not.toBeNull();
        expect(responseContent).not.toBe("");
        expect(responseContent).toContain("Passos");
        expect(responseContent).toContain("San Lorenzo");
        expect(responseContent.replace(/,/g, "")).toContain("135460");
        expect(responseContent.replace(/,/g, "")).toContain("204356");
    });
});
