/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { createInternalServerRpc } from "../src/generated/rpc.js";

// Internal host example; these methods are not part of the public SDK API.
export async function editUserSettings(
    settings: ReturnType<typeof createInternalServerRpc>["user"]["settings"],
    configDir: string
) {
    const validation = await settings.validate({
        topKey: "voice",
        value: { enabled: true },
    });
    if (!validation.ok) {
        throw new Error(JSON.stringify(validation.issues));
    }
    await settings.update({
        key: "voice",
        value: { enabled: true },
        configDir,
    });
    return settings.update({
        key: "voice",
        value: { selectedModel: null },
        configDir,
    });
}
