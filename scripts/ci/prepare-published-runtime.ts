/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/** Downloads verified published CLI artifacts and stages the shared CI runtime layout. */

import { spawnSync } from "node:child_process";
import { chmodSync, cpSync, mkdirSync, mkdtempSync, realpathSync, rmSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import { COPILOT_CLI_VERSION } from "../../nodejs/src/cliVersion.js";
import { downloadVerifiedReleaseAsset, ensureCopilotPackage } from "../../nodejs/scripts/releaseArtifacts.js";

const SDK_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "../..");

export async function preparePublishedRuntime({
    target,
    runtimeRoot = defaultRuntimeRoot(),
    acquirePackage = (platform: string) => ensureCopilotPackage(COPILOT_CLI_VERSION, { platform }),
    acquireExecutable = acquirePublishedExecutable,
}: {
    target: string;
    runtimeRoot?: string;
    acquirePackage?: (target: string) => Promise<string>;
    acquireExecutable?: (target: string) => Promise<{ path: string; cleanup?: () => void }>;
}) {
    const packageRoot = await acquirePackage(target);
    const wrapperName = target.startsWith("win32-") ? "copilot-runtime.exe" : "copilot-runtime";
    const executableName = target.startsWith("win32-") ? "copilot.exe" : "copilot";
    const wrapper = join(packageRoot, "prebuilds", target, wrapperName);
    if (!statSync(wrapper, { throwIfNoEntry: false })?.isFile()) {
        throw new Error(`Published runtime wrapper not found at ${wrapper}`);
    }

    const acquiredExecutable = await acquireExecutable(target);
    try {
        const distCli = join(runtimeRoot, "dist-cli");
        const distBin = join(runtimeRoot, "dist-bin", target);
        rmSync(distCli, { recursive: true, force: true });
        rmSync(distBin, { recursive: true, force: true });
        cpSync(packageRoot, distCli, { recursive: true });
        mkdirSync(distBin, { recursive: true });
        const executable = join(distBin, executableName);
        cpSync(acquiredExecutable.path, executable);
        if (!target.startsWith("win32-")) {
            chmodSync(executable, 0o755);
        }
    } finally {
        acquiredExecutable.cleanup?.();
    }
}

async function acquirePublishedExecutable(target: string) {
    const isWindows = target.startsWith("win32-");
    const executableName = isWindows ? "copilot.exe" : "copilot";
    const assetName = `copilot-${target}.${isWindows ? "zip" : "tar.gz"}`;
    const archive = await downloadVerifiedReleaseAsset(COPILOT_CLI_VERSION, assetName);
    const stagingRoot = mkdtempSync(join(tmpdir(), "copilot-sdk-cli-"));
    const archivePath = join(stagingRoot, assetName);
    writeFileSync(archivePath, archive);

    const command = isWindows ? "unzip" : "tar";
    const args = isWindows ? ["-q", archivePath, "-d", stagingRoot] : ["-xzf", archivePath, "-C", stagingRoot];
    const result = spawnSync(command, args, { encoding: "utf8" });
    if (result.error || result.status !== 0) {
        rmSync(stagingRoot, { recursive: true, force: true });
        throw result.error ?? new Error(`${command} failed: ${result.stderr}`);
    }
    const executable = join(stagingRoot, executableName);
    if (!statSync(executable, { throwIfNoEntry: false })?.isFile()) {
        rmSync(stagingRoot, { recursive: true, force: true });
        throw new Error(`Published CLI executable not found in ${assetName}`);
    }
    return {
        path: executable,
        cleanup: () => rmSync(stagingRoot, { recursive: true, force: true }),
    };
}

function defaultRuntimeRoot() {
    if (process.env.GITHUB_WORKSPACE) {
        return resolve(process.env.GITHUB_WORKSPACE);
    }
    const nestedRuntimeRoot = resolve(SDK_ROOT, "../..");
    return resolve(nestedRuntimeRoot, "src/sdk") === SDK_ROOT ? nestedRuntimeRoot : SDK_ROOT;
}

if (process.argv[1] && realpathSync(fileURLToPath(import.meta.url)) === realpathSync(resolve(process.argv[1]))) {
    const target = process.argv[2];
    if (!target || process.argv.length !== 3) {
        throw new Error("Usage: prepare-published-runtime.ts <target>");
    }
    preparePublishedRuntime({ target }).catch((error: unknown) => {
        console.error(error);
        process.exitCode = 1;
    });
}
