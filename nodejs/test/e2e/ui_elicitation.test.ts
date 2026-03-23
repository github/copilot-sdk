/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("UI Elicitation", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("elicitation methods throw in headless mode", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
        });

        // The SDK spawns the CLI headless - no TUI means no elicitation support.
        expect(session.capabilities.ui?.elicitation).toBeFalsy();
        await expect(session.ui.confirm("test")).rejects.toThrow(/not supported/);
    });
});
