/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import {
    createWriteStream,
    existsSync,
    mkdirSync,
    readdirSync,
    rmSync,
    chmodSync,
    copyFileSync,
} from "node:fs";
import { rename, rm } from "node:fs/promises";
import { homedir, platform, arch, tmpdir } from "node:os";
import { join, dirname, resolve } from "node:path";
import { spawn } from "node:child_process";
import * as semver from "semver";
import { CopilotClient } from "./client.js";
import { PREFERRED_CLI_VERSION } from "./generatedOnBuild/versions.js";

export { PREFERRED_CLI_VERSION };

export interface AcquisitionOptions {
    /**
     * Directory where CLI versions will be downloaded and stored.
     * Should be app-specific (e.g., `~/.myapp/copilot-cli/`).
     */
    downloadDir: string;

    /**
     * Minimum CLI version required.
     * Format: "major.minor.patch" (e.g., "0.0.402"). No "v" prefix.
     */
    minVersion?: string;

    /**
     * Callback for download progress updates.
     */
    onProgress?: (progress: AcquisitionProgress) => void;
}

export interface AcquisitionProgress {
    bytesDownloaded: number;
    totalBytes: number;
}

const RELEASES_BASE_URL = "https://github.com/github/copilot-cli/releases/download";

function getPlatformAsset(): { platform: string; arch: string; ext: string } {
    const p = platform();
    const a = arch();

    let platformName: string;
    let archName: string;
    let ext: string;

    switch (p) {
        case "win32":
            platformName = "win32";
            ext = "zip";
            break;
        case "darwin":
            platformName = "darwin";
            ext = "tar.gz";
            break;
        case "linux":
            platformName = "linux";
            ext = "tar.gz";
            break;
        default:
            throw new Error(`Unsupported platform: ${p}`);
    }

    switch (a) {
        case "x64":
            archName = "x64";
            break;
        case "arm64":
            archName = "arm64";
            break;
        default:
            throw new Error(`Unsupported architecture: ${a}`);
    }

    return { platform: platformName, arch: archName, ext };
}

function getDownloadUrl(version: string): string {
    const { platform: p, arch: a, ext } = getPlatformAsset();
    return `${RELEASES_BASE_URL}/v${version}/copilot-${p}-${a}.${ext}`;
}

function getCliExecutablePath(versionDir: string): string {
    const execName = platform() === "win32" ? "copilot.exe" : "copilot";
    return join(versionDir, execName);
}

async function checkProtocolCompatibility(cliPath: string): Promise<boolean> {
    const client = new CopilotClient({
        cliPath,
        autoStart: false,
        logLevel: "none",
    });

    try {
        await client.start();
        return true;
    } catch {
        return false;
    } finally {
        try {
            await client.forceStop();
        } catch {
            // Ignore cleanup errors
        }
    }
}

function listExistingVersions(downloadDir: string): string[] {
    if (!existsSync(downloadDir)) {
        return [];
    }

    const versions: string[] = [];
    const entries = readdirSync(downloadDir, { withFileTypes: true });

    for (const entry of entries) {
        if (entry.isDirectory() && entry.name.startsWith("copilot-")) {
            const version = entry.name.replace("copilot-", "");
            if (semver.valid(version)) {
                const execPath = getCliExecutablePath(join(downloadDir, entry.name));
                if (existsSync(execPath)) {
                    versions.push(version);
                }
            }
        }
    }

    return versions.sort((a, b) => semver.compare(b, a));
}

async function findSuitableVersion(
    downloadDir: string,
    minVersion: string | undefined
): Promise<string | null> {
    const versions = listExistingVersions(downloadDir);

    for (const version of versions) {
        if (minVersion && semver.lt(version, minVersion)) {
            continue;
        }

        // Skip protocol check if version matches PREFERRED_CLI_VERSION (known compatible)
        if (version === PREFERRED_CLI_VERSION) {
            return version;
        }

        const cliPath = getCliExecutablePath(join(downloadDir, `copilot-${version}`));
        if (await checkProtocolCompatibility(cliPath)) {
            return version;
        }
    }

    return null;
}

async function downloadCli(
    version: string,
    downloadDir: string,
    onProgress?: (progress: AcquisitionProgress) => void
): Promise<string> {
    const url = getDownloadUrl(version);
    const versionDir = join(downloadDir, `copilot-${version}`);
    const tempDir = join(
        tmpdir(),
        `copilot-download-${Date.now()}-${Math.random().toString(36).slice(2)}`
    );

    mkdirSync(downloadDir, { recursive: true });
    mkdirSync(tempDir, { recursive: true });

    try {
        const response = await fetch(url, { redirect: "follow" });

        if (!response.ok) {
            throw new Error(
                `Failed to download CLI v${version}: ${response.status} ${response.statusText}`
            );
        }

        const contentLength = parseInt(response.headers.get("content-length") || "0", 10);
        const { ext } = getPlatformAsset();
        const archivePath = join(tempDir, `copilot.${ext}`);

        const reader = response.body?.getReader();
        if (!reader) {
            throw new Error("Failed to get response body reader");
        }

        const writeStream = createWriteStream(archivePath);
        let bytesDownloaded = 0;

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            writeStream.write(value);
            bytesDownloaded += value.length;

            onProgress?.({
                bytesDownloaded,
                totalBytes: contentLength,
            });
        }

        await new Promise<void>((resolve, reject) => {
            writeStream.close((err: Error | null | undefined) => {
                if (err) reject(err);
                else resolve();
            });
        });

        const extractDir = join(tempDir, "extracted");
        mkdirSync(extractDir, { recursive: true });

        await extractArchive(archivePath, extractDir);

        const execName = platform() === "win32" ? "copilot.exe" : "copilot";
        const extractedExec = findExecutable(extractDir, execName);

        if (!extractedExec) {
            throw new Error(`Could not find ${execName} in downloaded archive`);
        }

        mkdirSync(dirname(versionDir), { recursive: true });
        const tempVersionDir = join(tempDir, `copilot-${version}`);
        mkdirSync(tempVersionDir, { recursive: true });

        const destExec = join(tempVersionDir, execName);
        copyFileSync(extractedExec, destExec);

        if (platform() !== "win32") {
            chmodSync(destExec, 0o755);
        }

        if (existsSync(versionDir)) {
            await rm(versionDir, { recursive: true, force: true });
        }
        await rename(tempVersionDir, versionDir);

        return getCliExecutablePath(versionDir);
    } finally {
        try {
            rmSync(tempDir, { recursive: true, force: true });
        } catch {
            // Ignore cleanup errors
        }
    }
}

// tar is built-in on Windows 10+, macOS, and Linux and handles .zip and .tar.gz
async function extractArchive(archivePath: string, destDir: string): Promise<void> {
    return new Promise((resolve, reject) => {
        const proc = spawn("tar", ["-xf", archivePath, "-C", destDir], { stdio: "pipe" });

        let stderr = "";
        proc.stderr?.on("data", (data) => {
            stderr += data.toString();
        });

        proc.on("error", reject);
        proc.on("exit", (code) => {
            if (code === 0) resolve();
            else {
                const msg = stderr ? `: ${stderr.trim()}` : "";
                reject(
                    new Error(
                        `Archive extraction failed for ${archivePath} (exit code ${code})${msg}`
                    )
                );
            }
        });
    });
}

function findExecutable(dir: string, execName: string): string | null {
    const entries = readdirSync(dir, { withFileTypes: true });

    for (const entry of entries) {
        const fullPath = join(dir, entry.name);
        if (entry.isDirectory()) {
            const found = findExecutable(fullPath, execName);
            if (found) return found;
        } else if (entry.name === execName) {
            return fullPath;
        }
    }

    return null;
}

function cleanupOldVersions(downloadDir: string, keepVersion: string): void {
    if (!existsSync(downloadDir)) return;

    for (const entry of readdirSync(downloadDir, { withFileTypes: true })) {
        if (entry.isDirectory() && entry.name.startsWith("copilot-")) {
            const version = entry.name.replace("copilot-", "");
            if (version !== keepVersion) {
                try {
                    rmSync(join(downloadDir, entry.name), { recursive: true, force: true });
                } catch {
                    // Skip if locked
                }
            }
        }
    }
}

function validateOptions(options: AcquisitionOptions): void {
    const normalizedDir = resolve(options.downloadDir);
    const forbiddenDir = resolve(homedir(), ".copilot");
    if (normalizedDir === forbiddenDir) {
        throw new Error(
            `Cannot use '${options.downloadDir}' as downloadDir. ` +
                "This directory is reserved for the global Copilot CLI installation. " +
                "Please use an app-specific directory (e.g., '~/.myapp/copilot-cli/')."
        );
    }

    if (options.minVersion && !semver.valid(options.minVersion)) {
        throw new Error(
            `Invalid minVersion format: '${options.minVersion}'. ` +
                "Expected format: 'major.minor.patch' (e.g., '0.0.402')"
        );
    }
}

export async function acquireCli(options: AcquisitionOptions): Promise<string> {
    validateOptions(options);

    const { downloadDir, minVersion, onProgress } = options;

    const effectiveVersion =
        minVersion && semver.gt(minVersion, PREFERRED_CLI_VERSION)
            ? minVersion
            : PREFERRED_CLI_VERSION;

    const suitableVersion = await findSuitableVersion(downloadDir, minVersion);

    if (suitableVersion) {
        cleanupOldVersions(downloadDir, suitableVersion);
        return getCliExecutablePath(join(downloadDir, `copilot-${suitableVersion}`));
    }

    const cliPath = await downloadCli(effectiveVersion, downloadDir, onProgress);
    cleanupOldVersions(downloadDir, effectiveVersion);

    return cliPath;
}
