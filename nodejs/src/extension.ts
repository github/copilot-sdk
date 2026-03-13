/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CopilotClient } from "./client.js";
import type { CopilotSession } from "./session.js";
import type { PermissionHandler, PermissionRequestResult, ResumeSessionConfig } from "./types.js";

const defaultJoinSessionPermissionHandler: PermissionHandler = (): PermissionRequestResult => ({
    kind: "no-result",
});

export type JoinSessionConfig = Omit<ResumeSessionConfig, "onPermissionRequest"> & {
    onPermissionRequest?: PermissionHandler;
};

/**
 * Joins the current foreground session.
 *
 * @param config - Configuration to add to the session
 * @returns A promise that resolves with the joined session
 *
 * @example
 * ```typescript
 * import { joinSession } from "@github/copilot-sdk/extension";
 *
 * const session = await joinSession({ tools: [myTool] });
 * ```
 */
export async function joinSession(config: JoinSessionConfig = {}): Promise<CopilotSession> {
    const sessionId = process.env.SESSION_ID;
    if (!sessionId) {
        throw new Error(
            "joinSession() is intended for extensions running as child processes of the Copilot CLI."
        );
    }

    const client = new CopilotClient({ isChildProcess: true });
    return client.resumeSession(sessionId, {
        ...config,
        onPermissionRequest: config.onPermissionRequest ?? defaultJoinSessionPermissionHandler,
        disableResume: config.disableResume ?? true,
    });
}
