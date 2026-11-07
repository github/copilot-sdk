/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotClient, CopilotSession } from "../src/index.js";

/** Read configuration without starting a server, then warm up one session's server set. */
export async function inspectLanguageServers(
    client: CopilotClient,
    session: CopilotSession,
    workingDirectory: string
) {
    const global = await client.rpc.lsp.list({ workingDirectory });
    const scoped = await session.rpc.lsp.list({ force: true });
    const warmup = await session.rpc.lsp.start({});
    return {
        globalServers: global.configs.map((server) => server.id),
        sessionServers: scoped.configs.map((server) => server.id),
        warmServers: warmup.servers,
        errors: warmup.errors,
    };
}
