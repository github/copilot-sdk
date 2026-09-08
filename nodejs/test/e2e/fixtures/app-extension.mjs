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
                    record(process.env.APP_EXTENSION_BATCH_SENT_FILE, "sent");
                }
            },
        });
        record(
            process.env.APP_EXTENSION_READY_FILE,
            JSON.stringify({
                principal: host.principal,
                hostKeys: Object.keys(host).sort(),
                contributionKeys: Object.keys(badges).sort(),
            })
        );
    });
} catch (error) {
    record(
        process.env.APP_EXTENSION_ERROR_FILE,
        error instanceof Error ? (error.stack ?? error.message) : String(error)
    );
}
