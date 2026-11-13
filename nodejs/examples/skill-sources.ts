/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    CopilotClient,
    SkillsInstallSourceRequest,
    SkillsInstallSourceResult,
    SkillsRemoveSourceRequest,
    SkillsRemoveSourceOutcome,
} from "@github/copilot-sdk";

/** Install direct HTTPS/file sources or register discovery directories without catalog plans. */
export async function installSkillSources(
    client: CopilotClient,
    sources: readonly string[],
    destination: Omit<SkillsInstallSourceRequest, "source">
): Promise<SkillsInstallSourceResult[]> {
    let skillDirectories = [...destination.skillDirectories];
    const results: SkillsInstallSourceResult[] = [];
    for (const source of sources) {
        const installed = await client.rpc.skills.installSource({
            ...destination,
            skillDirectories,
            source,
        });
        skillDirectories = installed.skillDirectories;
        results.push(installed);
        console.log(`${installed.kind}: ${installed.path}`);
    }
    return results;
}

/** Unregister a directory or remove a direct file with the caller's discovery inventory. */
export async function removeSkillSource(
    client: CopilotClient,
    request: SkillsRemoveSourceRequest
): Promise<SkillsRemoveSourceOutcome> {
    const outcome = await client.rpc.skills.removeSource(request);
    console.log(
        outcome.kind === "removed"
            ? `${outcome.result.kind}: removed ${outcome.result.path}`
            : "needs-inventory: no files changed"
    );
    return outcome;
}
