/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe.each([
    ["tenant.ghe.example", "fallback.ghe.example", "https://tenant.ghe.example"],
    ["", "fallback.ghe.example", "https://fallback.ghe.example"],
    ["", "", "https://github.com"],
])(
    "auth host with COPILOT_GH_HOST=%s and GH_HOST=%s",
    async (copilotHost, ghHost, expectedHost) => {
        const { copilotClient: client } = await createSdkTestContext({
            copilotClientOptions: {
                env: {
                    COPILOT_GH_HOST: copilotHost,
                    GH_HOST: ghHost,
                },
            },
        });

        it("reports the selected authentication host", async () => {
            await client.start();
            const status = await client.getAuthStatus();
            expect(status.isAuthenticated).toBe(true);
            expect(status.host).toBe(expectedHost);
        });
    }
);
