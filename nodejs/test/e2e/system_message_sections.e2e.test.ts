/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("System message sections", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should_use_replaced_identity_section_in_response", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    identity: {
                        action: "replace",
                        content:
                            "You are a helpful gardening assistant called Botanica. You only answer questions about plants and gardening.",
                    },
                },
            },
        });

        const response = await session.sendAndWait({ prompt: "Who are you?" });

        expect(response).not.toBeNull();
        const content = response!.data.content.toLowerCase();
        expect(
            content.includes("botanica") || content.includes("garden") || content.includes("plant"),
            `Expected response to reflect the replaced identity section, but got: ${response!.data.content}`
        ).toBe(true);

        await session.disconnect();
    });

    it("should_use_replaced_preamble_section_in_response", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    preamble: {
                        action: "replace",
                        content:
                            "You are a helpful gardening assistant called Botanica. You only answer questions about plants and gardening.",
                    },
                },
            },
        });

        const response = await session.sendAndWait({ prompt: "Who are you?" });

        expect(response).not.toBeNull();
        const content = response!.data.content.toLowerCase();
        expect(
            content.includes("botanica") || content.includes("garden") || content.includes("plant"),
            `Expected response to reflect the replaced preamble section, but got: ${response!.data.content}`
        ).toBe(true);

        await session.disconnect();
    });
});
