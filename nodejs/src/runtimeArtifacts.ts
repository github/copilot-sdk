import { createHash } from "node:crypto";
import {
    chmodSync,
    copyFileSync,
    existsSync,
    lstatSync,
    mkdirSync,
    mkdtempSync,
    readdirSync,
    renameSync,
    rmSync,
    statSync,
    writeFileSync,
} from "node:fs";
import { homedir } from "node:os";
import { dirname, join, relative, sep } from "node:path";
import { x as extractTar } from "tar";
import { COPILOT_CLI_HASHES, COPILOT_CLI_VERSION } from "./cliVersion.js";

export interface RuntimeArtifactSources {
    packageRoot: string;
    platform: string;
}

export interface EnsureRuntimeBundleOptions {
    cacheRoot?: string;
    environment?: NodeJS.ProcessEnv;
    fetch?: typeof globalThis.fetch;
    platform?: string;
}

const runtimeDownloads = new Map<string, Promise<string>>();

const EXCLUDED_TOP_LEVEL = new Set([
    "app.js",
    "assets",
    "changelog.json",
    "copilot",
    "copilot.exe",
    "copilot-sdk",
    "foundry-local-sdk",
    "index.js",
    "LICENSE.md",
    "napi-oop-runtime",
    "npm-loader.js",
    "package.json",
    "preloads",
    "pvrecorder",
    "queries",
    "README.md",
    "sdk",
    "sea-loader.js",
    "webview",
]);

interface RuntimeAsset {
    source: string;
    relativePath: string;
}

function validateFile(path: string, label: string): void {
    if (!existsSync(path)) {
        throw new Error(`${label} not found at ${path}.`);
    }
    if (statSync(path).size === 0) {
        throw new Error(`${label} at ${path} is empty.`);
    }
}

function validateRuntimeBundle(wrapper: string, runtimeNode: string): void {
    validateFile(wrapper, "Copilot runtime wrapper");
    validateFile(runtimeNode, "Copilot runtime.node");
}

function sanitizeCacheSegment(value: string): string {
    return value.replace(/[^a-zA-Z0-9._-]/g, "_");
}

function isExcluded(relativePath: string): boolean {
    const parts = relativePath.split(sep);
    const topLevel = parts[0];
    const fileName = parts.at(-1) ?? "";
    return (
        EXCLUDED_TOP_LEVEL.has(topLevel) ||
        /^tree-sitter.*\.wasm$/.test(topLevel) ||
        /^voice-.*\.js$/.test(topLevel) ||
        fileName === "cli-native.node" ||
        parts.includes("mediaremote-adapter") ||
        fileName.startsWith("copilot-runtime-bin")
    );
}

function collectRuntimeAssets(sources: RuntimeArtifactSources): RuntimeAsset[] {
    const assets: RuntimeAsset[] = [];
    const visit = (directory: string): void => {
        for (const entry of readdirSync(directory, { withFileTypes: true })) {
            const source = join(directory, entry.name);
            const sourceRelative = relative(sources.packageRoot, source);
            if (isExcluded(sourceRelative)) {
                continue;
            }
            if (entry.isDirectory()) {
                visit(source);
                continue;
            }
            if (!entry.isFile() && !entry.isSymbolicLink()) {
                continue;
            }

            const parts = sourceRelative.split(sep);
            let relativePath = sourceRelative;
            if (parts[0] === "prebuilds") {
                if (parts[1] !== sources.platform || parts.length < 3) {
                    continue;
                }
                relativePath = parts.slice(2).join(sep);
            }
            assets.push({ source, relativePath });
        }
    };
    visit(sources.packageRoot);
    return assets.sort((left, right) => left.relativePath.localeCompare(right.relativePath));
}

function sourceFingerprint(assets: RuntimeAsset[]): string {
    const hash = createHash("sha256");
    for (const asset of assets) {
        const stat = lstatSync(asset.source);
        hash.update(asset.relativePath).update("\0");
        hash.update(`${stat.size}:${stat.mtimeMs}`).update("\0");
    }
    return hash.digest("hex").slice(0, 20);
}

function makeExecutable(path: string): void {
    if (process.platform === "win32") {
        return;
    }
    const mode = statSync(path).mode;
    if ((mode & 0o111) === 0) {
        chmodSync(path, mode | 0o111);
    }
}

export function defaultRuntimeCacheRoot(
    platform = process.platform,
    home = homedir(),
    environment: NodeJS.ProcessEnv = process.env
): string {
    const cacheDirectory =
        platform === "win32"
            ? (environment.LOCALAPPDATA ?? join(home, "AppData", "Local"))
            : platform === "darwin"
              ? join(home, "Library", "Caches")
              : (environment.XDG_CACHE_HOME ?? join(home, ".cache"));
    return join(cacheDirectory, "github-copilot-sdk", "runtime");
}

export function materializeRuntimeBundle(
    sources: RuntimeArtifactSources,
    cacheRoot = defaultRuntimeCacheRoot(),
    cacheKey = `${sources.platform}-${sourceFingerprint(collectRuntimeAssets(sources))}`
): string {
    const assets = collectRuntimeAssets(sources);
    const wrapperName = sources.platform.startsWith("win32")
        ? "copilot-runtime.exe"
        : "copilot-runtime";
    const sourceWrapper = assets.find((asset) => asset.relativePath === wrapperName)?.source;
    const sourceRuntimeNode = assets.find((asset) => asset.relativePath === "runtime.node")?.source;
    validateRuntimeBundle(sourceWrapper ?? "", sourceRuntimeNode ?? "");

    const installDir = join(cacheRoot, cacheKey);
    const installedWrapper = join(installDir, wrapperName);
    const installedRuntimeNode = join(installDir, "runtime.node");
    if (existsSync(installDir)) {
        validateRuntimeBundle(installedWrapper, installedRuntimeNode);
        makeExecutable(installedWrapper);
        return installedWrapper;
    }

    mkdirSync(cacheRoot, { recursive: true });
    const stagingDir = mkdtempSync(join(cacheRoot, ".runtime-"));
    try {
        for (const asset of assets) {
            const destination = join(stagingDir, asset.relativePath);
            mkdirSync(dirname(destination), { recursive: true });
            copyFileSync(asset.source, destination);
        }
        const stagedWrapper = join(stagingDir, wrapperName);
        makeExecutable(stagedWrapper);
        renameSync(stagingDir, installDir);
    } catch (error) {
        if (!existsSync(installDir)) {
            throw error;
        }
        validateRuntimeBundle(installedWrapper, installedRuntimeNode);
    } finally {
        rmSync(stagingDir, { recursive: true, force: true });
    }

    return installedWrapper;
}

function isMusl(): boolean {
    if (process.platform !== "linux") {
        return false;
    }
    const report = process.report?.getReport() as
        | { header?: { glibcVersionRuntime?: string } }
        | undefined;
    return report?.header?.glibcVersionRuntime === undefined;
}

export function getRuntimePlatform(
    platform = process.platform,
    arch = process.arch,
    musl = isMusl()
): string {
    if (arch !== "x64" && arch !== "arm64") {
        throw new Error(`Unsupported Copilot CLI architecture: ${arch}.`);
    }
    if (platform === "linux") {
        return `${musl ? "linuxmusl" : "linux"}-${arch}`;
    }
    if (platform === "darwin" || platform === "win32") {
        return `${platform}-${arch}`;
    }
    throw new Error(`Unsupported Copilot CLI platform: ${platform}-${arch}.`);
}

export function getRuntimeReleaseAssetName(version: string, platform: string): string {
    return `github-copilot-${version}-${platform}.tgz`;
}

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

function checksumForAsset(checksums: string, assetName: string): string {
    for (const line of checksums.split(/\r?\n/)) {
        const [digest, name] = line.trim().split(/\s+/, 2);
        if (name?.replace(/^\*/, "") === assetName && /^[a-f0-9]{64}$/i.test(digest)) {
            return digest.toLowerCase();
        }
    }
    throw new Error(`SHA256SUMS.txt does not contain ${assetName}.`);
}

export async function ensureRuntimeBundle(
    version: string,
    options: EnsureRuntimeBundleOptions = {}
): Promise<string> {
    const platform = options.platform ?? getRuntimePlatform();
    const cacheRoot = options.cacheRoot ?? defaultRuntimeCacheRoot();
    const baseUrl = (
        (options.environment ?? process.env).COPILOT_CLI_DOWNLOAD_BASE_URL ??
        "https://github.com/github/copilot-cli/releases/download"
    ).replace(/\/+$/, "");
    const downloadKey = `${cacheRoot}\0${version}\0${platform}\0${baseUrl}`;
    if (!options.fetch) {
        const existing = runtimeDownloads.get(downloadKey);
        if (existing) {
            return existing;
        }
        const download = ensureRuntimeBundleUncached(version, options);
        runtimeDownloads.set(downloadKey, download);
        try {
            return await download;
        } finally {
            runtimeDownloads.delete(downloadKey);
        }
    }
    return ensureRuntimeBundleUncached(version, options);
}

async function ensureRuntimeBundleUncached(
    version: string,
    options: EnsureRuntimeBundleOptions
): Promise<string> {
    const platform = options.platform ?? getRuntimePlatform();
    const cacheRoot = options.cacheRoot ?? defaultRuntimeCacheRoot();
    const versionRoot = join(cacheRoot, sanitizeCacheSegment(version));
    const wrapperName = platform.startsWith("win32") ? "copilot-runtime.exe" : "copilot-runtime";
    const installedWrapper = join(versionRoot, platform, wrapperName);
    const installedRuntimeNode = join(versionRoot, platform, "runtime.node");
    if (existsSync(installedWrapper) && existsSync(installedRuntimeNode)) {
        validateRuntimeBundle(installedWrapper, installedRuntimeNode);
        makeExecutable(installedWrapper);
        return installedWrapper;
    }

    const packageRoot = await ensureCopilotPackage(version, options);
    return materializeRuntimeBundle({ packageRoot, platform }, versionRoot, platform);
}

export async function ensureCopilotPackage(
    version: string,
    options: EnsureRuntimeBundleOptions = {}
): Promise<string> {
    const environment = options.environment ?? process.env;
    const platform = options.platform ?? getRuntimePlatform();
    const cacheRoot = options.cacheRoot ?? defaultRuntimeCacheRoot();
    const versionRoot = join(cacheRoot, sanitizeCacheSegment(version));
    const cachedPackageRoot = join(versionRoot, "packages", platform);
    const cachedRuntimeNode = join(cachedPackageRoot, "prebuilds", platform, "runtime.node");
    if (existsSync(cachedRuntimeNode)) {
        validateFile(cachedRuntimeNode, "Copilot runtime.node");
        return cachedPackageRoot;
    }

    const fetcher = options.fetch ?? globalThis.fetch;
    if (!fetcher) {
        throw new Error("This Node.js runtime does not provide fetch().");
    }
    mkdirSync(cacheRoot, { recursive: true });
    const baseUrl = (
        environment.COPILOT_CLI_DOWNLOAD_BASE_URL ??
        "https://github.com/github/copilot-cli/releases/download"
    ).replace(/\/+$/, "");
    const releaseUrl = `${baseUrl}/v${version}`;
    const assetName = getRuntimeReleaseAssetName(version, platform);
    const pinnedChecksum =
        version === COPILOT_CLI_VERSION ? COPILOT_CLI_HASHES[platform] : undefined;
    const [checksumsResponse, assetResponse] = await Promise.all([
        pinnedChecksum
            ? Promise.resolve(undefined)
            : fetchWithRetry(fetcher, `${releaseUrl}/SHA256SUMS.txt`),
        fetchWithRetry(fetcher, `${releaseUrl}/${assetName}`),
    ]);
    const archive = Buffer.from(await assetResponse.arrayBuffer());
    const expectedChecksum =
        pinnedChecksum ?? checksumForAsset(await checksumsResponse!.text(), assetName);
    const actualChecksum = createHash("sha256").update(archive).digest("hex");
    if (actualChecksum !== expectedChecksum) {
        throw new Error(
            `Checksum mismatch for ${assetName}: expected ${expectedChecksum}, got ${actualChecksum}.`
        );
    }

    const stagingRoot = mkdtempSync(join(cacheRoot, ".download-"));
    const archivePath = join(stagingRoot, assetName);
    const packageRoot = join(stagingRoot, "package");
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
            if (!existsSync(cachedRuntimeNode)) {
                throw error;
            }
        }
        return cachedPackageRoot;
    } finally {
        rmSync(stagingRoot, { recursive: true, force: true });
    }
}
