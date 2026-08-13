/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotClientInfo } from "./types.js";

/**
 * Environment variable carrying the client identity to the runtime.
 *
 * Applications declare {@link CopilotClientInfo} instead of setting this
 * directly: the variable is an implementation detail of how the identity
 * reaches the runtime, and passing it through the environment is what lets a
 * typed option work against any runtime version, with no protocol change.
 */
export const COPILOT_CLIENT_INFO_ENV_VAR = "COPILOT_CLIENT_INFO";

const LOWERCASE_ENV_VAR = COPILOT_CLIENT_INFO_ENV_VAR.toLowerCase();

const CLIENT_INFO_FIELDS = ["editorName", "editorVersion", "extensionName", "extensionVersion"] as const;

/**
 * Serialize {@link CopilotClientInfo} into `env`, replacing any value already
 * there.
 *
 * A caller-supplied or inherited value is always removed first. Otherwise the
 * typed option would be bypassable by writing the raw string, and an inherited
 * one describes whatever process launched this one rather than this client.
 *
 * Only defined string fields are serialized, so an identity that declares
 * nothing usable leaves no variable behind and the runtime keeps its default
 * attribution.
 */
export function applyClientInfoEnv(
    env: Record<string, string | undefined>,
    clientInfo: CopilotClientInfo | undefined
): void {
    for (const key of Object.keys(env)) {
        // Windows resolves environment variables case-insensitively while this
        // is a plain case-sensitive object, so a differently cased spelling
        // would otherwise survive alongside the canonical key and could win the
        // de-duplication that happens when the child process is spawned.
        // Elsewhere it is a genuinely separate variable, so it is left alone.
        if (key === COPILOT_CLIENT_INFO_ENV_VAR || (process.platform === "win32" && key.toLowerCase() === LOWERCASE_ENV_VAR)) {
            delete env[key];
        }
    }
    if (!clientInfo) {
        return;
    }
    const fields: Record<string, string> = {};
    for (const field of CLIENT_INFO_FIELDS) {
        const value = clientInfo[field];
        if (typeof value === "string" && value.length > 0) {
            fields[field] = value;
        }
    }
    if (Object.keys(fields).length > 0) {
        env[COPILOT_CLIENT_INFO_ENV_VAR] = JSON.stringify(fields);
    }
}
