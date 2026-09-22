// Resolves the runtime executable used by Node.js SDK development and tests. Nested runtime
// checkouts require an existing same-checkout artifact; standalone SDK checkouts acquire the pinned release.

import fs from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { getRuntimePlatform, materializeRuntimeBundle } from "../src/runtimeArtifacts.js";
import { COPILOT_CLI_VERSION } from "../src/cliVersion.js";
import { ensureCopilotPackage } from "./releaseArtifacts.js";
import { findRuntimeRoot, getRuntimeCliPaths } from "../../scripts/runtime-layout.mjs";

const sdkRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../..");

export async function resolvePreparedRuntimePath({
    acquirePackage = ensureCopilotPackage,
    environment = process.env,
    option,
    root = sdkRoot,
}: {
    acquirePackage?: typeof ensureCopilotPackage;
    environment?: NodeJS.ProcessEnv;
    option?: string;
    root?: string;
} = {}): Promise<string> {
    if (option !== undefined && option !== "--print-path" && option !== "--print-legacy-path") {
        throw new Error(`Unknown option: ${option}`);
    }

    const runtimeRoot = findRuntimeRoot(root);
    const runtimeSource = environment.COPILOT_RUNTIME_SOURCE ?? (runtimeRoot ? "checkout" : undefined);
    if (runtimeSource === "checkout") {
        const paths = getRuntimeCliPaths(runtimeRoot, environment);
        const selectedPath = option === "--print-legacy-path" ? paths.legacyCliPath : paths.cliPath;
        if (!selectedPath || !fs.statSync(selectedPath, { throwIfNoEntry: false })?.isFile()) {
            const variable = option === "--print-legacy-path" ? "COPILOT_LEGACY_CLI_PATH" : "COPILOT_CLI_PATH";
            throw new Error(
                `${variable} must select an existing same-checkout runtime artifact; run pnpm run build:cli first`,
            );
        }
        return resolve(selectedPath);
    }

    const platform = getRuntimePlatform();
    const packageRoot = await acquirePackage(COPILOT_CLI_VERSION, { platform });
    return option === "--print-legacy-path"
        ? join(packageRoot, "app.js")
        : materializeRuntimeBundle({ packageRoot, platform });
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    process.stdout.write(`${await resolvePreparedRuntimePath({ option: process.argv[2] })}\n`);
}
