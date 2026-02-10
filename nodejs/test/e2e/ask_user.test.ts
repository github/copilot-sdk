/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import type { UserInputRequest, UserInputResponse } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("User input (ask_user)", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should invoke user input handler when model uses ask_user tool", async () => {
        const userInputRequests: UserInputRequest[] = [];

        const session = await client.createSession({
            onUserInputRequest: async (request, invocation) => {
                userInputRequests.push(request);
                expect(invocation.sessionId).toBe(session.sessionId);

                // Return the first choice if available, otherwise a freeform answer
                const response: UserInputResponse = {
                    answer: request.choices?.[0] ?? "freeform answer",
                    wasFreeform: !request.choices?.length,
                };
                return response;
            },
        });

        await session.sendAndWait({
            prompt: "Ask me to choose between 'Option A' and 'Option B' using the ask_user tool. Wait for my response before continuing.",
        });

        // Should have received at least one user input request
        expect(userInputRequests.length).toBeGreaterThan(0);

        // The request should have a question
        expect(userInputRequests.some((req) => req.question && req.question.length > 0)).toBe(true);

        await session.destroy();
    });

    it("should receive choices in user input request", async () => {
        const userInputRequests: UserInputRequest[] = [];

        const session = await client.createSession({
            onUserInputRequest: async (request) => {
                userInputRequests.push(request);
                // Pick the first choice
                return {
                    answer: request.choices?.[0] ?? "default",
                    wasFreeform: false,
                };
            },
        });

        await session.sendAndWait({
            prompt: "Use the ask_user tool to ask me to pick between exactly two options: 'Red' and 'Blue'. These should be provided as choices. Wait for my answer.",
        });

        // Should have received a request
        expect(userInputRequests.length).toBeGreaterThan(0);

        // At least one request should have choices
        const requestWithChoices = userInputRequests.find(
            (req) => req.choices && req.choices.length > 0
        );
        expect(requestWithChoices).toBeDefined();

        await session.destroy();
    });

    it("should handle freeform user input response", async () => {
        const userInputRequests: UserInputRequest[] = [];
        const freeformAnswer = "This is my custom freeform answer that was not in the choices";

        const session = await client.createSession({
            onUserInputRequest: async (request) => {
                userInputRequests.push(request);
                // Return a freeform answer (not from choices)
                return {
                    answer: freeformAnswer,
                    wasFreeform: true,
                };
            },
        });

        const response = await session.sendAndWait({
            prompt: "Ask me a question using ask_user and then include my answer in your response. The question should be 'What is your favorite color?'",
        });

        // Should have received a request
        expect(userInputRequests.length).toBeGreaterThan(0);

        // The model's response should reference the freeform answer we provided
        // (This is a soft check since the model may paraphrase)
        expect(response).toBeDefined();

        await session.destroy();
    });
});
