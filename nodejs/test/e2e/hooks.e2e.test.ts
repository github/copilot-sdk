/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { SessionHooks } from "../../src/types.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const UNSUPPORTED_SDK_HOOKS_MESSAGE = "SDK hook callbacks are no longer supported";

describe("Session hooks", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    async function expectUnsupportedHooks(hooks: SessionHooks) {
        await expect(
            client.createSession({
                onPermissionRequest: approveAll,
                hooks,
            })
        ).rejects.toThrow(UNSUPPORTED_SDK_HOOKS_MESSAGE);
    }

    const hookCases: Array<[string, SessionHooks]> = [
        ["preToolUse", { onPreToolUse: () => ({ permissionDecision: "allow" }) }],
        ["postToolUse", { onPostToolUse: () => undefined }],
        ["preToolUse denial", { onPreToolUse: () => ({ permissionDecision: "deny" }) }],
        [
            "combined preToolUse and postToolUse",
            {
                onPreToolUse: () => ({ permissionDecision: "allow" }),
                onPostToolUse: () => undefined,
            },
        ],
    ];

    it.each(hookCases)("rejects SDK callback hook %s", async (_name, hooks) => {
        await expectUnsupportedHooks(hooks);
    });
});
