/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotSession } from "../src/index.js";

/** Configure one SDK-owned session, then observe its existing policy barriers. */
export async function configureSessionPolicy(
    session: CopilotSession,
    config: Parameters<CopilotSession["rpc"]["featureFlags"]["configure"]>[0],
    update: Parameters<CopilotSession["rpc"]["featureFlags"]["update"]>[0]
): Promise<string> {
    const configured = await session.rpc.featureFlags.configure(config);
    const changed = await session.rpc.featureFlags.update(update);
    const current = await session.rpc.featureFlags.read({
        flags: ["TOOL_SEARCH_DISABLED", "CATALOG_SEARCH"],
    });
    const before = await session.rpc.managedSettings.getCurrentSnapshot();
    await session.rpc.managedSettings.awaitPublished();
    await session.rpc.managedSettings.awaitApplied();
    const after = await session.rpc.managedSettings.getCurrentSnapshot();
    return JSON.stringify({
        sameService: configured.serviceId === changed.serviceId,
        revision: current.snapshot.revision,
        flags: current.flags,
        before,
        after,
    });
}

export async function inspectSessionPolicy(session: CopilotSession, cwd: string) {
    const permissions = await session.rpc.permissions.getAllowAll();
    const egress = await session.rpc.sandbox.getRemoteMcpEgress({ url: "https://example.com" });
    const context = await session.rpc.metadata.getWorkingDirectoryContext({ cwd });
    const additionalDirectories = await session.rpc.permissions.additionalDirectories.list();
    const result = { permissions, egress, context, additionalDirectories };
    console.log("Session policy:", JSON.stringify(result));
    return result;
}
