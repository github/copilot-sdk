/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { appendFileSync, writeFileSync } from "node:fs";

const sdkModule =
    process.env.APP_EXTENSION_SDK_MODULE ?? "@github/copilot-sdk/private/app-extension";
const { defineAppExtension } = await import(sdkModule);

const record = (path, value) => {
    if (path) {
        writeFileSync(path, value);
    }
};

try {
    await defineAppExtension(async (host) => {
        const contributionId = (point) => {
            const declaration = host.contributions.find(
                (candidate) => candidate.contributionPoint === point
            );
            if (!declaration) {
                throw new Error(`Missing ${point} contribution`);
            }
            return declaration.contributionId;
        };
        const badges = await host.sessionBadges.register({
            onSnapshot: async (snapshot, identity) => {
                if (process.env.APP_EXTENSION_SNAPSHOT_FILE) {
                    appendFileSync(
                        process.env.APP_EXTENSION_SNAPSHOT_FILE,
                        `${JSON.stringify({ snapshot, identity })}\n`
                    );
                }
                if (snapshot.sessions.length > 0 && process.env.APP_EXTENSION_BADGE_UPDATES) {
                    await badges.setBadges(JSON.parse(process.env.APP_EXTENSION_BADGE_UPDATES));
                    await badges.setPresentations(
                        JSON.parse(process.env.APP_EXTENSION_PRESENTATION_UPDATES)
                    );
                    record(process.env.APP_EXTENSION_BATCH_SENT_FILE, "sent");
                }
            },
            onAction: ({ target, draft }) => ({
                prompt: `# Pull Request Creation\nCreate a fake${draft ? " draft" : ""} pull request for ${target.branch ?? target.workspaceId}.`,
                requiredTool: "create_ado_pull_request",
            }),
        });
        const canvas = await host.canvases.register({
            contributionId: contributionId("canvases"),
            onOpen: ({ instanceId, input, context }) => ({
                state: { instanceId, input, context },
                title: "Repository overview",
                status: "Ready",
                actions: [
                    {
                        name: "create",
                        label: "Create pull request",
                        input: { draft: false },
                        variant: "primary",
                    },
                ],
            }),
            onAction: ({ actionName, input }) => ({ actionName, input }),
        });
        const forge = await host.forgeProviders.register({
            contributionId: contributionId("forgeProvider"),
            operations: {
                getPullRequest: async ({ accountId, input, signal }) => {
                    const response = await host.mediatedFetch.request({
                        contributionId: contributionId("forgeProvider"),
                        accountId,
                        operation: "getPullRequest",
                        method: "GET",
                        path: `/repos/github/copilot-sdk/pulls/${input.number}`,
                        headers: { Accept: "application/json" },
                        signal,
                    });
                    return {
                        status: response.status,
                        body: response.body,
                        truncated: response.truncated,
                    };
                },
            },
        });
        record(
            process.env.APP_EXTENSION_READY_FILE,
            JSON.stringify({
                principal: host.principal,
                hostKeys: Object.keys(host).sort(),
                contributions: {
                    badges: Object.keys(badges).sort(),
                    canvas: Object.keys(canvas).sort(),
                    forge: Object.keys(forge).sort(),
                },
            })
        );
    });
} catch (error) {
    record(
        process.env.APP_EXTENSION_ERROR_FILE,
        error instanceof Error ? (error.stack ?? error.message) : String(error)
    );
}
