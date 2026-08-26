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
} from "node:fs";
import { homedir } from "node:os";
import { dirname, join, relative, sep } from "node:path";

export interface RuntimeArtifactSources {
    packageRoot: string;
    platform: string;
}

const EXCLUDED_TOP_LEVEL = new Set([
    "app.js",
    "assets",
    "changelog.json",
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
    cacheRoot = defaultRuntimeCacheRoot()
): string {
    const assets = collectRuntimeAssets(sources);
    const wrapperName = process.platform === "win32" ? "copilot-runtime.exe" : "copilot-runtime";
    const sourceWrapper = assets.find((asset) => asset.relativePath === wrapperName)?.source;
    const sourceRuntimeNode = assets.find((asset) => asset.relativePath === "runtime.node")?.source;
    validateRuntimeBundle(sourceWrapper ?? "", sourceRuntimeNode ?? "");

    const installDir = join(cacheRoot, `${sources.platform}-${sourceFingerprint(assets)}`);
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
