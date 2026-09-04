/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFileSync } from "node:fs";

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
        record(process.env.EXTENSION_SNAPSHOT_FILE, JSON.stringify(snapshot));
    });
    record(process.env.EXTENSION_READY_FILE, "ready");
} catch (error) {
    record(
        process.env.EXTENSION_ERROR_FILE,
        error instanceof Error ? (error.stack ?? error.message) : String(error)
    );
}
