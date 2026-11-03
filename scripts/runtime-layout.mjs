/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { realpathSync, statSync } from "node:fs";
import { join, resolve } from "node:path";

const RUNTIME_LAYOUT_MARKER = "script/sea-build.ts";

export function findRuntimeRoot(sdkRoot) {
    const runtimeRoot = resolve(sdkRoot, "../..");
    try {
        if (
            realpathSync(sdkRoot) !== realpathSync(join(runtimeRoot, "src/sdk")) ||
            !statSync(join(runtimeRoot, RUNTIME_LAYOUT_MARKER)).isFile()
        ) {
            return undefined;
        }
        return runtimeRoot;
    } catch {
        return undefined;
    }
}

export function getRuntimeCliPaths(runtimeRoot, environment = process.env) {
    const target = environment.COPILOT_RUNTIME_TARGET ?? `${process.platform}-${process.arch}`;
    const wrapperName = process.platform === "win32" ? "copilot-runtime.exe" : "copilot-runtime";
    return {
        cliPath:
            environment.COPILOT_CLI_PATH ??
            (runtimeRoot ? join(runtimeRoot, "dist-cli", "prebuilds", target, wrapperName) : undefined),
        legacyCliPath:
            environment.COPILOT_LEGACY_CLI_PATH ??
            (runtimeRoot ? join(runtimeRoot, "dist-cli", "app.js") : undefined),
    };
}
