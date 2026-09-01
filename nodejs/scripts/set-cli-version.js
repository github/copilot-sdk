import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const [version, mode] = process.argv.slice(2);
if (!version || !/^\d+\.\d+\.\d+(?:-[0-9A-Za-z._-]+)?$/.test(version)) {
    throw new Error("Usage: set-cli-version.js <semver> [--npm-package]");
}
if (mode !== undefined && mode !== "--npm-package") {
    throw new Error(`Unknown option: ${mode}`);
}

const nodeRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
const runtimePlatforms = [
    "darwin-arm64",
    "darwin-x64",
    "linux-arm64",
    "linux-x64",
    "linuxmusl-arm64",
    "linuxmusl-x64",
    "win32-arm64",
    "win32-x64",
];
const cliAssets = [
    "copilot-darwin-arm64.tar.gz",
    "copilot-darwin-x64.tar.gz",
    "copilot-linux-arm64.tar.gz",
    "copilot-linux-x64.tar.gz",
    "copilot-win32-arm64.zip",
    "copilot-win32-x64.zip",
];
const useNpmPackage = mode === "--npm-package";
let runtimeHashes = {};
let cliHashes = {};
if (!useNpmPackage) {
    const checksumsUrl = `https://github.com/github/copilot-cli/releases/download/v${version}/SHA256SUMS.txt`;
    const response = await fetch(checksumsUrl);
    if (!response.ok) {
        throw new Error(
            `Failed to download ${checksumsUrl}: ${response.status} ${response.statusText}`
        );
    }
    const checksums = new Map(
        (await response.text())
            .split(/\r?\n/)
            .map((line) => line.trim().split(/\s+/, 2))
            .filter(([hash, name]) => /^[a-fA-F0-9]{64}$/.test(hash) && name)
            .map(([hash, name]) => [name.replace(/^\*/, ""), hash.toLowerCase()])
    );
    const hashForAsset = (assetName) => {
        const hash = checksums.get(assetName);
        if (!hash) {
            throw new Error(`SHA256SUMS.txt does not contain ${assetName}`);
        }
        return hash;
    };
    runtimeHashes = Object.fromEntries(
        runtimePlatforms.map((platform) => [
            platform,
            hashForAsset(`github-copilot-${version}-${platform}.tgz`),
        ])
    );
    cliHashes = Object.fromEntries(
        cliAssets.map((assetName) => [assetName, hashForAsset(assetName)])
    );
}
const packagePath = join(nodeRoot, "package.json");
const packageJson = JSON.parse(readFileSync(packagePath, "utf8"));
packageJson.copilotCliVersion = version;
writeFileSync(packagePath, `${JSON.stringify(packageJson, null, 4)}\n`);

const manifest = {
    version,
    source: useNpmPackage ? "npm-package" : "github-release",
    runtimeHashes,
    cliHashes,
};
writeFileSync(join(nodeRoot, "copilot-cli.json"), `${JSON.stringify(manifest, null, 4)}\n`);

const sourcePath = join(nodeRoot, "src", "cliVersion.ts");
writeFileSync(
    sourcePath,
    [
        `export const COPILOT_CLI_VERSION = ${JSON.stringify(version)};`,
        "",
        `export const COPILOT_CLI_USE_NPM_PACKAGE = ${useNpmPackage};`,
        "",
        `export const COPILOT_CLI_HASHES: Readonly<Record<string, string>> = ${JSON.stringify(runtimeHashes, null, 4)};`,
        "",
    ].join("\n")
);
