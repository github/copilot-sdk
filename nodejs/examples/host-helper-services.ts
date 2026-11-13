/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    CopilotClient,
    SessionsHasEventsRequest,
    SessionsHasEventsResult,
    ShellBuildInteractiveEnvironmentRequest,
    ShellBuildInteractiveEnvironmentResult,
} from "../src/index.js";

/** Check a saved session and prepare a shell environment without exposing host credentials. */
export async function inspectHostHelperServices(
    client: CopilotClient,
    sessionId: string
): Promise<string> {
    const { exists }: SessionsHasEventsResult = await client.rpc.sessions.hasEvents({
        sessionId,
    } satisfies SessionsHasEventsRequest);
    const { environment }: ShellBuildInteractiveEnvironmentResult =
        await client.rpc.shell.buildInteractiveEnvironment({
            hostEnvironment: {
                PATH: process.env.PATH ?? "",
                GITHUB_TOKEN: "example-secret-that-must-not-reach-the-shell",
                COPILOT_SUPERVISED: "1",
            },
            additionalBlockedNames: ["COPILOT_SUPERVISED"],
            caseInsensitive: process.platform === "win32",
        } satisfies ShellBuildInteractiveEnvironmentRequest);
    return JSON.stringify(
        {
            hasEvents: exists,
            shellVariableNames: Object.keys(environment).sort(),
            gitConfig: environment.GIT_CONFIG_VALUE_0,
        },
        null,
        2
    );
}
