/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { SessionHooks } from "../../src/types.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const UNSUPPORTED_SDK_HOOKS_MESSAGE = "SDK hook callbacks are no longer supported";

describe("Subagent hooks", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("rejects SDK callback hooks for sub-agent hook propagation", async () => {
        const hooks: SessionHooks = {
            onPreToolUse: () => ({ permissionDecision: "allow" }),
            onPostToolUse: () => undefined,
        };

        await expect(
            client.createSession({
                onPermissionRequest: approveAll,
                hooks,
            })
        ).rejects.toThrow(UNSUPPORTED_SDK_HOOKS_MESSAGE);
    });
});
