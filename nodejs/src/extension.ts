/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CopilotClient } from "./client.js";
import type { CopilotSession } from "./session.js";
import type { ResumeSessionConfig } from "./types.js";

/**
 * Joins the current foreground session.
 *
 * @param config - Configuration to add to the session
 * @returns A promise that resolves with the joined session
 *
 * @example
 * ```typescript
 * import { approveAll } from "@github/copilot-sdk";
 * import { joinSession } from "@github/copilot-sdk/extension";
 *
 * const session = await joinSession({
 *   onPermissionRequest: approveAll,
 *   tools: [myTool],
 * });
 * ```
 */
export async function joinSession(config: ResumeSessionConfig): Promise<CopilotSession> {
    const sessionId = process.env.SESSION_ID;
    if (!sessionId) {
        throw new Error(
            "joinSession() is intended for extensions running as child processes of the Copilot CLI."
        );
    }

    const client = new CopilotClient({ isChildProcess: true });
    return client.resumeSession(sessionId, {
        ...config,
        disableResume: config.disableResume ?? true,
    });
}
