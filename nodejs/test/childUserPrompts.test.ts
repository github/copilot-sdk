/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { childUserPrompts } from "./e2e/harness/childUserPrompts.js";

const context = "start hook context\n\nRead the requested file.";

describe("subagent request observation", () => {
    it.each([
        ["/chat/completions", { messages: [{ role: "user", content: context }] }],
        ["/messages", { messages: [{ role: "user", content: [{ type: "text", text: context }] }] }],
        ["/responses", { input: context }],
        [
            "/responses",
            {
                input: [
                    {
                        type: "message",
                        role: "user",
                        content: [{ type: "input_text", text: context }],
                    },
                ],
            },
        ],
    ])("observes the child's user input on %s", (endpoint, body) => {
        expect(
            childUserPrompts(`http://localhost${endpoint}`, JSON.stringify(body), {
                parentAgentId: "parent",
            })
        ).toEqual([context]);
    });

    it.each(["/chat/completions", "/messages", "/responses"])(
        "does not mistake a parent request for child context on %s",
        (endpoint) => {
            expect(
                childUserPrompts(
                    `http://localhost${endpoint}`,
                    JSON.stringify({
                        messages: [{ role: "user", content: context }],
                        input: [{ type: "message", role: "user", content: context }],
                    }),
                    {}
                )
            ).toEqual([]);
        }
    );

    it.each(["/chat/completions", "/messages", "/responses"])(
        "does not find hook context in system, developer, assistant, or tool content on %s",
        (endpoint) => {
            const messages = ["system", "developer", "assistant", "tool"].map((role) => ({
                role,
                content: context,
            }));
            expect(
                childUserPrompts(
                    `http://localhost${endpoint}`,
                    JSON.stringify({
                        instructions: context,
                        system: context,
                        messages,
                        input: [
                            ...messages
                                .filter((message) => message.role !== "tool")
                                .map((message) => ({ type: "message", ...message })),
                            { type: "function_call_output", call_id: "tool", output: context },
                        ],
                    }),
                    { parentAgentId: "parent" }
                )
            ).toEqual([]);
        }
    );

    it.each([
        { kind: "string", content: context },
        { kind: "blocks", content: [{ type: "input_text", text: context }] },
    ])("ignores Responses developer-only $kind input", ({ content }) => {
        expect(
            childUserPrompts(
                "http://localhost/responses",
                JSON.stringify({
                    input: [{ type: "message", role: "developer", content }],
                }),
                { parentAgentId: "parent" }
            )
        ).toEqual([]);
    });

    it("extracts only the original user input from mixed Responses roles", () => {
        const userPrompt = "Read the requested file.";
        expect(
            childUserPrompts(
                "http://localhost/responses",
                JSON.stringify({
                    input: [
                        { type: "message", role: "developer", content: context },
                        {
                            type: "message",
                            role: "user",
                            content: [{ type: "input_text", text: userPrompt }],
                        },
                    ],
                }),
                { parentAgentId: "parent" }
            )
        ).toEqual([userPrompt]);
    });

    it.each(["/chat/completions", "/messages", "/responses"])(
        "keeps missing child context missing on %s",
        (endpoint) => {
            expect(
                childUserPrompts(`http://localhost${endpoint}`, "{}", {
                    parentAgentId: "parent",
                })
            ).toEqual([]);
        }
    );
});
