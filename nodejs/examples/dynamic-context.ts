/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotSession } from "../src/index.js";
import type { SessionDynamicContextConfig } from "../src/generated/rpc.js";

/** Configure a context board for an SDK host's background memory agent. */
export async function configureContextBoard(
    session: CopilotSession,
    config: SessionDynamicContextConfig
): Promise<string> {
    const before = await session.rpc.dynamicContext.getConfig();
    await session.rpc.dynamicContext.setConfig(config);
    const after = await session.rpc.dynamicContext.getConfig();
    return JSON.stringify({ before: before.config, after: after.config }, null, 2);
}
