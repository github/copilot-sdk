/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { appendFileSync, writeFileSync } from "node:fs";

const sdkModule = process.env.EXTENSION_SDK_MODULE ?? "@github/copilot-sdk/extension";
const { joinAppSessionBadges } = await import(sdkModule);

const record = (path, value) => {
    if (path) {
        writeFileSync(path, value);
    }
};

try {
    const badges = await joinAppSessionBadges();
    badges.onSnapshot((snapshot) => {
        if (process.env.EXTENSION_SNAPSHOT_FILE) {
            appendFileSync(process.env.EXTENSION_SNAPSHOT_FILE, `${JSON.stringify(snapshot)}\n`);
        }
    });
    if (process.env.EXTENSION_INVALID_BATCH_FILE) {
        try {
            await badges.setBadges([
                {
                    workspaceId: "duplicate-workspace",
                    sessionId: "duplicate-session",
                    badge: { state: "open" },
                },
                {
                    workspaceId: "duplicate-workspace",
                    sessionId: "duplicate-session",
                    badge: null,
                },
            ]);
        } catch (error) {
            record(
                process.env.EXTENSION_INVALID_BATCH_FILE,
                error instanceof Error ? error.message : String(error)
            );
        }
    }
    if (process.env.EXTENSION_BADGE_UPDATES) {
        await badges.setBadges(JSON.parse(process.env.EXTENSION_BADGE_UPDATES));
    }
    record(process.env.EXTENSION_READY_FILE, "ready");
} catch (error) {
    record(
        process.env.EXTENSION_ERROR_FILE,
        error instanceof Error ? (error.stack ?? error.message) : String(error)
    );
}
