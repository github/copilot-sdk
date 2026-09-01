import { createHash } from "node:crypto";
import { existsSync, mkdirSync, mkdtempSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { x as extractTar } from "tar";
import {
    defaultRuntimeCacheRoot,
    getRuntimePlatform,
    getRuntimeReleaseAssetName,
    resolvePackageRoot,
    validateFile,
} from "../src/runtimeArtifacts.js";
import { COPILOT_CLI_USE_NPM_PACKAGE, COPILOT_CLI_VERSION } from "../src/cliVersion.js";

export interface EnsureCopilotPackageOptions {
    cacheRoot?: string;
    environment?: NodeJS.ProcessEnv;
    fetch?: typeof globalThis.fetch;
    platform?: string;
}

const packageDownloads = new Map<string, Promise<string>>();
const checksumDownloads = new Map<string, Promise<Map<string, string>>>();

async function fetchWithRetry(fetcher: typeof globalThis.fetch, url: string): Promise<Response> {
    let lastError: unknown;
    for (let attempt = 0; attempt < 3; attempt++) {
        try {
            const response = await fetcher(url);
            if (response.ok) {
                return response;
            }
            await response.body?.cancel();
            lastError = new Error(`${response.status} ${response.statusText}`);
            if (
                response.status >= 400 &&
                response.status < 500 &&
                response.status !== 408 &&
                response.status !== 429
            ) {
                break;
            }
        } catch (error) {
            lastError = error;
        }
        if (attempt < 2) {
            await new Promise((resolve) => setTimeout(resolve, 2 ** attempt * 1000));
        }
    }
    throw new Error(`Failed to download ${url}: ${String(lastError)}`);
}

async function getReleaseChecksum(
    version: string,
    assetName: string,
    baseUrl: string,
    fetcher: typeof globalThis.fetch
): Promise<string | undefined> {
    const key = `${baseUrl}\0${version}`;
    let checksums = checksumDownloads.get(key);
    if (!checksums) {
        checksums = downloadReleaseChecksums(version, baseUrl, fetcher);
        checksumDownloads.set(key, checksums);
        try {
            return (await checksums).get(assetName);
        } catch (error) {
            checksumDownloads.delete(key);
            throw error;
        }
    }
    return (await checksums).get(assetName);
}

async function downloadReleaseChecksums(
    version: string,
    baseUrl: string,
    fetcher: typeof globalThis.fetch
): Promise<Map<string, string>> {
    const response = await fetchWithRetry(fetcher, `${baseUrl}/v${version}/SHA256SUMS.txt`);
    const checksums = new Map<string, string>();
    for (const line of (await response.text()).split(/\r?\n/)) {
        const [hash, name] = line.trim().split(/\s+/, 2);
        if (/^[a-fA-F0-9]{64}$/.test(hash) && name) {
            checksums.set(name.replace(/^\*/, ""), hash.toLowerCase());
        }
    }
    return checksums;
}

export async function ensureCopilotPackage(
    version = COPILOT_CLI_VERSION,
    options: EnsureCopilotPackageOptions = {}
): Promise<string> {
    const platform = options.platform ?? getRuntimePlatform();
    // lgtm[js/trivial-conditional] This generated constant is true for internal canary builds.
    if (version === COPILOT_CLI_VERSION && COPILOT_CLI_USE_NPM_PACKAGE) {
        const packageName = `@github/copilot-${platform}`;
        const packageRoot = resolvePackageRoot(packageName);
        if (!packageRoot) {
            throw new Error(`Could not resolve ${packageName} for Copilot CLI ${version}.`);
        }
        validateFile(
            join(packageRoot, "prebuilds", platform, "runtime.node"),
            "Copilot runtime.node"
        );
        return packageRoot;
    }

    const cacheRoot = options.cacheRoot ?? defaultRuntimeCacheRoot();
    const cachedPackageRoot = join(cacheRoot, version, "packages", platform);
    const cachedRuntimeNode = join(cachedPackageRoot, "prebuilds", platform, "runtime.node");
    if (existsSync(cachedRuntimeNode)) {
        validateFile(cachedRuntimeNode, "Copilot runtime.node");
        return cachedPackageRoot;
    }

    const baseUrl = (
        (options.environment ?? process.env).COPILOT_CLI_DOWNLOAD_BASE_URL ??
        "https://github.com/github/copilot-cli/releases/download"
    ).replace(/\/+$/, "");
    const key = `${cacheRoot}\0${version}\0${platform}\0${baseUrl}`;
    if (!options.fetch) {
        const existing = packageDownloads.get(key);
        if (existing) {
            return existing;
        }
        const download = downloadCopilotPackage(
            version,
            platform,
            cacheRoot,
            baseUrl,
            globalThis.fetch
        );
        packageDownloads.set(key, download);
        try {
            return await download;
        } finally {
            packageDownloads.delete(key);
        }
    }
    return downloadCopilotPackage(version, platform, cacheRoot, baseUrl, options.fetch);
}

async function downloadCopilotPackage(
    version: string,
    platform: string,
    cacheRoot: string,
    baseUrl: string,
    fetcher: typeof globalThis.fetch
): Promise<string> {
    if (!fetcher) {
        throw new Error("This Node.js runtime does not provide fetch().");
    }
    const assetName = getRuntimeReleaseAssetName(version, platform);
    const expectedChecksum = await getReleaseChecksum(version, assetName, baseUrl, fetcher);
    if (!expectedChecksum) {
        throw new Error(`SHA256SUMS.txt does not contain ${assetName}.`);
    }
    const response = await fetchWithRetry(fetcher, `${baseUrl}/v${version}/${assetName}`);
    const archive = Buffer.from(await response.arrayBuffer());
    const actualChecksum = createHash("sha256").update(archive).digest("hex");
    if (actualChecksum !== expectedChecksum) {
        throw new Error(
            `Checksum mismatch for ${assetName}: expected ${expectedChecksum}, got ${actualChecksum}.`
        );
    }

    mkdirSync(cacheRoot, { recursive: true });
    const stagingRoot = mkdtempSync(join(cacheRoot, ".download-"));
    const archivePath = join(stagingRoot, assetName);
    const packageRoot = join(stagingRoot, "package");
    const cachedPackageRoot = join(cacheRoot, version, "packages", platform);
    writeFileSync(archivePath, archive);
    try {
        await extractTar({
            cwd: stagingRoot,
            file: archivePath,
            gzip: true,
            preservePaths: false,
            strict: true,
        });
        validateFile(
            join(packageRoot, "prebuilds", platform, "runtime.node"),
            "Copilot runtime.node"
        );
        mkdirSync(dirname(cachedPackageRoot), { recursive: true });
        try {
            renameSync(packageRoot, cachedPackageRoot);
        } catch (error) {
            if (!existsSync(cachedPackageRoot)) {
                throw error;
            }
        }
        return cachedPackageRoot;
    } finally {
        rmSync(stagingRoot, { recursive: true, force: true });
    }
}
