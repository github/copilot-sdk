/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotSession } from "../src/index.js";

type IdeOptions = Extract<
    NonNullable<Parameters<CopilotSession["rpc"]["mcp"]["startServer"]>[0]["config"]>,
    { type: "local-http" }
>;

// The endpoint and workspace paths refer to the runtime host, not a remote SDK client.
export async function connectToIdeExample(session: CopilotSession, options: IdeOptions) {
    const unsubscribe = session.on("ide.selection_changed", (event) => {
        console.log("IDE selection:", JSON.stringify(event.data.selection));
    });
    try {
        await session.rpc.mcp.startServer({ serverName: "ide", config: options });
        const state = await session.rpc.mcp.ide.state();
        console.log("Connected IDE:", state.connectedIde?.ideName);
        const result = await session.rpc.mcp.callTool({
            serverName: "ide",
            toolName: "get_selection",
            arguments: {},
        });
        console.log("IDE tool result:", JSON.stringify(result));
        return { state, unsubscribe };
    } catch (error) {
        unsubscribe();
        throw error;
    }

}

export async function disconnectFromIdeExample(session: CopilotSession) {
    await session.rpc.mcp.stopServer({ serverName: "ide", transport: "local-http" });
    const state = await session.rpc.mcp.ide.state();
    console.log("Connected IDE:", state.connectedIde?.ideName ?? "none");
    return state;
}
