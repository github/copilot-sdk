/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import { ParsedHttpExchange } from "../../../test/harness/replayingCapiProxy.js";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("System message transform", async () => {
    const { copilotClient: client, openAiEndpoint, workDir } = await createSdkTestContext();

    it("should invoke transform callbacks with section content", async () => {
        const transformedSections: Record<string, string> = {};

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    identity: {
                        action: (content: string) => {
                            transformedSections["identity"] = content;
                            // Pass through unchanged
                            return content;
                        },
                    },
                    tone: {
                        action: (content: string) => {
                            transformedSections["tone"] = content;
                            return content;
                        },
                    },
                },
            },
        });

        await writeFile(join(workDir, "test.txt"), "Hello transform!");

        await session.sendAndWait({
            prompt: "Read the contents of test.txt and tell me what it says",
        });

        // Transform callbacks should have been invoked with real section content
        expect(Object.keys(transformedSections).length).toBe(2);
        expect(transformedSections["identity"]).toBeDefined();
        expect(transformedSections["identity"]!.length).toBeGreaterThan(0);
        expect(transformedSections["tone"]).toBeDefined();
        expect(transformedSections["tone"]!.length).toBeGreaterThan(0);

        await session.disconnect();
    });

    it("should apply transform modifications to section content", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    identity: {
                        action: (content: string) => {
                            return content + "\nTRANSFORM_MARKER";
                        },
                    },
                },
            },
        });

        await writeFile(join(workDir, "hello.txt"), "Hello!");

        await session.sendAndWait({
            prompt: "Read the contents of hello.txt",
        });

        // Verify the transform result was actually applied to the system message
        const traffic = await openAiEndpoint.getExchanges();
        const systemMessage = getSystemMessage(traffic[0]);
        expect(systemMessage).toContain("TRANSFORM_MARKER");

        await session.disconnect();
    });

    it("should work with static overrides and transforms together", async () => {
        const transformedSections: Record<string, string> = {};

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    // Static override
                    safety: { action: "remove" },
                    // Transform
                    identity: {
                        action: (content: string) => {
                            transformedSections["identity"] = content;
                            return content;
                        },
                    },
                },
            },
        });

        await writeFile(join(workDir, "combo.txt"), "Combo test!");

        await session.sendAndWait({
            prompt: "Read the contents of combo.txt and tell me what it says",
        });

        // Transform should have been invoked
        expect(transformedSections["identity"]).toBeDefined();
        expect(transformedSections["identity"]!.length).toBeGreaterThan(0);

        await session.disconnect();
    });
});

function getSystemMessage(exchange: ParsedHttpExchange): string | undefined {
    const systemMessage = exchange.request.messages.find((m) => m.role === "system") as
        | { role: "system"; content: string }
        | undefined;
    return systemMessage?.content;
}
