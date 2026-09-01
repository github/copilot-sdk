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
} from "node:fs";
import { createHash } from "node:crypto";
import { createRequire } from "node:module";
import { homedir } from "node:os";
import { dirname, join, relative, sep } from "node:path";
import { COPILOT_CLI_USE_NPM_PACKAGE } from "./cliVersion.js";

export interface RuntimeArtifactSources {
    packageRoot: string;
    platform: string;
}

export interface EnsureRuntimeBundleOptions {
    cacheRoot?: string;
    packageSearchPaths?: string[];
    platform?: string;
}

const require = createRequire(typeof __filename === "string" ? __filename : import.meta.url);
export const RUNTIME_PLATFORMS = [
    "darwin-arm64",
    "darwin-x64",
    "linux-arm64",
    "linux-x64",
    "linuxmusl-arm64",
    "linuxmusl-x64",
    "win32-arm64",
    "win32-x64",
] as const;

const EXCLUDED_TOP_LEVEL = new Set([
    "app.js",
    "assets",
    "changelog.json",
    "copilot",
    "copilot.exe",
    "foundry-local-sdk",
    "index.js",
    "LICENSE.md",
    "napi-oop-runtime",
    "npm-loader.js",
    "package.json",
    "pvrecorder",
    "queries",
    "README.md",
    "sea-loader.js",
    "webview",
]);

interface RuntimeAsset {
    source: string;
    relativePath: string;
}

export function validateFile(path: string, label: string): void {
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

export function getRuntimePackageName(platform: string): string {
    return `@github/copilot-sdk-${platform}`;
}

export function resolvePackageRoot(
    packageName: string,
    searchPaths = require.resolve.paths(packageName) ?? []
): string | undefined {
    return searchPaths
        .map((base) => join(base, ...packageName.split("/")))
        .find((candidate) => existsSync(join(candidate, "package.json")));
}

export async function ensureRuntimeBundle(
    version: string,
    options: EnsureRuntimeBundleOptions = {}
): Promise<string> {
    const platform = options.platform ?? getRuntimePlatform();
    // lgtm[js/trivial-conditional] This generated constant is true for internal canary builds.
    if (COPILOT_CLI_USE_NPM_PACKAGE) {
        const packageName = `@github/copilot-${platform}`;
        const packageRoot = resolvePackageRoot(packageName, options.packageSearchPaths);
        if (!packageRoot) {
            throw new Error(`Could not resolve ${packageName} for Copilot CLI ${version}.`);
        }
        validateFile(
            join(packageRoot, "prebuilds", platform, "runtime.node"),
            "Copilot runtime.node"
        );
        return materializeRuntimeBundle(
            { packageRoot, platform },
            options.cacheRoot,
            `${version}-${platform}`
        );
    }

    const packageName = getRuntimePackageName(platform);
    const packageRoot = resolvePackageRoot(packageName, options.packageSearchPaths);
    if (!packageRoot) {
        throw new Error(
            `Could not resolve ${packageName}. Reinstall @github/copilot-sdk so its platform package is installed.`
        );
    }
    const wrapperName = platform.startsWith("win32") ? "copilot-runtime.exe" : "copilot-runtime";
    const wrapper = join(packageRoot, wrapperName);
    try {
        validateRuntimeBundle(wrapper, join(packageRoot, "runtime.node"));
    } catch (error) {
        throw new Error(
            `${packageName} is missing required Copilot CLI runtime files. Reinstall @github/copilot-sdk.`,
            { cause: error }
        );
    }
    makeExecutable(wrapper);
    return wrapper;
}
