/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { SessionHooks } from "../../src/types.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const UNSUPPORTED_SDK_HOOKS_MESSAGE = "SDK hook callbacks are no longer supported";

describe("Extended session hooks", async () => {
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
        ["userPromptSubmitted", { onUserPromptSubmitted: () => undefined }],
        ["sessionStart", { onSessionStart: () => undefined }],
        ["sessionEnd", { onSessionEnd: () => undefined }],
        ["errorOccurred", { onErrorOccurred: () => undefined }],
        ["preToolUse output", { onPreToolUse: () => ({ permissionDecision: "allow" }) }],
        ["postToolUse output", { onPostToolUse: () => ({ suppressOutput: false }) }],
        [
            "postToolUseFailure output",
            {
                onPostToolUse: () => undefined,
                onPostToolUseFailure: () => ({ additionalContext: "not used" }),
            },
        ],
    ];

    it.each(hookCases)("rejects SDK callback hook %s", async (_name, hooks) => {
        await expectUnsupportedHooks(hooks);
    });
});
