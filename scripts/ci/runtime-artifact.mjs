/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/** Normalizes target-specific runtime artifacts, schemas, and SDK test environment variables. */

import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const SDK_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

/**
 * @typedef {object} RuntimeEnvironment
 * @property {string} COPILOT_CLI_PATH
 * @property {string} COPILOT_LEGACY_CLI_PATH
 * @property {string} COPILOT_SKIP_CLI_DOWNLOAD
 * @property {string} COPILOT_RUNTIME_BINARY_PATH
 * @property {string} COPILOT_RUNTIME_LIBRARY_PATH
 * @property {string} COPILOT_EXTENSION_SDK_PATH
 * @property {string} COPILOT_CLI_RELEASE_TARBALL
 * @property {string} COPILOT_CLI_RELEASE_SHA256
 * @property {string} BUNDLED_CLI_CACHE_DIR
 */

/**
 * @param {{
 *   runtimeRoot?: string,
 *   sdkRoot?: string,
 *   target?: string,
 *   outputDirectory?: string,
 *   environmentFile?: string
 * }} [options]
 * @returns {RuntimeEnvironment}
 */
export function prepareRuntimeArtifact(options = {}) {
    const target = options.target ?? process.env.COPILOT_RUNTIME_TARGET ?? `${process.platform}-${process.arch}`;
    const {
        runtimeRoot = defaultRuntimeRoot(),
        sdkRoot = SDK_ROOT,
        outputDirectory = process.env.COPILOT_RUNTIME_OUTPUT_DIRECTORY ??
            path.join(process.env.RUNNER_TEMP ?? os.tmpdir(), "copilot-sdk-runtime", target),
        environmentFile = process.env.GITHUB_ENV,
    } = options;
    const isWindowsTarget = target.startsWith("win32-");
    const executableName = isWindowsTarget ? "copilot.exe" : "copilot";
    const wrapperName = isWindowsTarget ? "copilot-runtime.exe" : "copilot-runtime";
    const cliDirectory = path.resolve(runtimeRoot, "dist-cli");
    const sourceLegacyCli = path.join(cliDirectory, "app.js");
    const sourceExecutable = path.resolve(runtimeRoot, "dist-bin", target, executableName);
    const sourceRuntime = path.join(cliDirectory, "prebuilds", target, "runtime.node");
    const sourceWrapper = path.join(cliDirectory, "prebuilds", target, wrapperName);

    assertFile(sourceExecutable, "CLI executable");
    assertFile(sourceLegacyCli, "legacy CLI entry point");
    assertFile(sourceRuntime, "runtime native library");
    assertFile(sourceWrapper, "runtime wrapper");

    const packageDirectory = path.join(outputDirectory, "package");
    fs.rmSync(outputDirectory, { recursive: true, force: true });
    fs.cpSync(cliDirectory, packageDirectory, { recursive: true });

    const packagedExecutable = path.join(packageDirectory, executableName);
    const packagedLegacyCli = path.join(packageDirectory, "app.js");
    const packagedRuntime = path.join(packageDirectory, "prebuilds", target, "runtime.node");
    const packagedWrapper = path.join(packageDirectory, "prebuilds", target, wrapperName);
    const packagedExtensionSdk = path.join(packageDirectory, "copilot-sdk");
    fs.copyFileSync(sourceExecutable, packagedExecutable);
    if (!isWindowsTarget) {
        fs.chmodSync(packagedExecutable, 0o755);
        fs.chmodSync(packagedWrapper, 0o755);
    }
    assertFile(path.join(packagedExtensionSdk, "index.js"), "extension SDK entry point");
    assertFile(path.join(packagedExtensionSdk, "extension.js"), "extension SDK extension entry point");

    const outOfProcessArchive = path.join(
        outputDirectory,
        isWindowsTarget ? `copilot-${target}.zip` : `copilot-${target}.tar.gz`,
    );
    const inProcessArchive = path.join(outputDirectory, `github-copilot-0.0.0-sdk-ci-${target}.tgz`);

    if (isWindowsTarget) {
        run("tar.exe", ["-a", "-cf", path.basename(outOfProcessArchive), "-C", "package", executableName], {
            cwd: outputDirectory,
        });
    } else {
        run("tar", ["-czf", path.basename(outOfProcessArchive), "-C", "package", executableName], {
            cwd: outputDirectory,
        });
    }

    run(
        process.platform === "win32" ? "tar.exe" : "tar",
        [
            "--exclude",
            `package/${executableName}`,
            "--exclude",
            "package/app.js",
            "-czf",
            path.basename(inProcessArchive),
            "package",
        ],
        { cwd: outputDirectory },
    );

    const rustCacheDirectory = stageRustArchives({
        target,
        isWindowsTarget,
        outOfProcessArchive,
        inProcessArchive,
    });

    const values = {
        COPILOT_CLI_PATH: packagedWrapper,
        COPILOT_LEGACY_CLI_PATH: packagedLegacyCli,
        COPILOT_SKIP_CLI_DOWNLOAD: "1",
        COPILOT_RUNTIME_BINARY_PATH: packagedExecutable,
        COPILOT_RUNTIME_LIBRARY_PATH: packagedRuntime,
        COPILOT_EXTENSION_SDK_PATH: packagedExtensionSdk,
        COPILOT_CLI_RELEASE_TARBALL: inProcessArchive,
        COPILOT_CLI_RELEASE_SHA256: sha256Hex(inProcessArchive),
        BUNDLED_CLI_CACHE_DIR: rustCacheDirectory,
    };

    if (environmentFile) {
        fs.appendFileSync(
            environmentFile,
            `${Object.entries(values)
                .map(([name, value]) => `${name}=${value}`)
                .join("\n")}\n`,
        );
    }

    return values;
}

function defaultRuntimeRoot() {
    const runtimeRoot = path.resolve(SDK_ROOT, "../..");
    if (path.resolve(runtimeRoot, "src/sdk") === SDK_ROOT) {
        return runtimeRoot;
    }
    return SDK_ROOT;
}

/**
 * @param {{ runtimeRoot?: string, schemaDirectory?: string, environmentFile?: string }} [options]
 */
export function stageRuntimeSchemas(options = {}) {
    const schemaDirectory = path.resolve(
        options.schemaDirectory ?? path.join(options.runtimeRoot ?? defaultRuntimeRoot(), "generated"),
    );
    const environmentFile = options.environmentFile ?? process.env.GITHUB_ENV;
    const schemaNames = ["session-events.schema.json", "api.schema.json"];

    for (const schemaName of schemaNames) {
        const schemaPath = path.join(schemaDirectory, schemaName);
        assertFile(schemaPath, "runtime schema");
        try {
            JSON.parse(fs.readFileSync(schemaPath, "utf8"));
        } catch (error) {
            throw new Error(`Invalid runtime schema at ${schemaPath}`, { cause: error });
        }
    }

    if (environmentFile) {
        fs.appendFileSync(
            environmentFile,
            `COPILOT_CLI_SCHEMA_DIR=${schemaDirectory}\nCOPILOT_RUNTIME_SOURCE=checkout\n`,
        );
    }
    return schemaDirectory;
}

/**
 * @param {{
 *   target: string,
 *   isWindowsTarget: boolean,
 *   outOfProcessArchive: string,
 *   inProcessArchive: string
 * }} options
 */
function stageRustArchives(options) {
    const { target, isWindowsTarget, outOfProcessArchive, inProcessArchive } = options;
    const version = "0.0.0-sdk-ci";
    const cacheDirectory = path.join(path.dirname(inProcessArchive), "rust-cache");
    const assetName = isWindowsTarget ? `copilot-${target}.zip` : `copilot-${target}.tar.gz`;
    const packageArchiveName = `github-copilot-${version}-${target}.tgz`;

    fs.mkdirSync(cacheDirectory, { recursive: true });
    fs.copyFileSync(outOfProcessArchive, path.join(cacheDirectory, `v${version}-${assetName}`));
    fs.copyFileSync(inProcessArchive, path.join(cacheDirectory, `v${version}-${packageArchiveName}`));

    return cacheDirectory;
}

/** @param {string} filePath */
function sha256Hex(filePath) {
    return createHash("sha256").update(fs.readFileSync(filePath)).digest("hex");
}

/** @param {string} filePath @param {string} description */
function assertFile(filePath, description) {
    if (!fs.statSync(filePath, { throwIfNoEntry: false })?.isFile()) {
        throw new Error(`${description} not found at ${filePath}`);
    }
}

/** @param {string} command @param {string[]} args @param {{ cwd?: string }} [options] */
function run(command, args, options = {}) {
    const result = spawnSync(command, args, { cwd: options.cwd, stdio: "inherit" });
    if (result.error) throw result.error;
    if (result.status !== 0) {
        throw new Error(`${command} exited with status ${result.status}`);
    }
}

function usage() {
    process.stderr.write("Usage: node scripts/ci/runtime-artifact.mjs <prepare|restore|stage-schemas>\n");
}

if (
    process.argv[1] &&
    fs.realpathSync(fileURLToPath(import.meta.url)) === fs.realpathSync(path.resolve(process.argv[1]))
) {
    if (!["prepare", "restore", "stage-schemas"].includes(process.argv[2]) || process.argv.length !== 3) {
        usage();
        process.exitCode = 2;
    } else if (process.argv[2] === "prepare") {
        prepareRuntimeArtifact();
    } else if (process.argv[2] === "restore") {
        restoreRuntimeArtifact();
    } else {
        stageRuntimeSchemas();
    }
}

export function restoreRuntimeArtifact(options = {}) {
    const runtimeRoot = options.runtimeRoot ?? defaultRuntimeRoot();
    if (process.platform === "win32") {
        return;
    }
    for (const directory of [
        path.join(runtimeRoot, "dist-cli/ripgrep/bin"),
        path.join(runtimeRoot, "dist-cli/tgrep/bin"),
        path.join(runtimeRoot, "dist-cli/prebuilds"),
    ]) {
        if (!fs.statSync(directory, { throwIfNoEntry: false })?.isDirectory()) {
            continue;
        }
        for (const file of fs.globSync("**/*", { cwd: directory })) {
            const filePath = path.join(directory, file);
            if (fs.statSync(filePath).isFile()) {
                fs.chmodSync(filePath, 0o755);
            }
        }
    }
}
