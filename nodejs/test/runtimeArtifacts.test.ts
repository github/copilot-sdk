import { existsSync, mkdtempSync, mkdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { createHash } from "node:crypto";
import { spawnSync } from "node:child_process";
import { c as createTar } from "tar";
import { afterEach, describe, expect, it, vi } from "vitest";

import {
    defaultRuntimeCacheRoot,
    ensureRuntimeBundle,
    getRuntimePackageName,
    getRuntimePlatform,
    getRuntimeReleaseAssetName,
    materializeRuntimeBundle,
} from "../src/runtimeArtifacts.js";
import { COPILOT_CLI_USE_NPM_PACKAGE, COPILOT_CLI_VERSION } from "../src/cliVersion.js";
import { ensureCopilotPackage } from "../scripts/releaseArtifacts.js";

describe("defaultRuntimeCacheRoot", () => {
    it.each([
        [
            "darwin",
            "/home/test",
            {},
            join("/home/test", "Library", "Caches", "github-copilot-sdk", "runtime"),
        ],
        ["linux", "/home/test", {}, join("/home/test", ".cache", "github-copilot-sdk", "runtime")],
        [
            "linux",
            "/home/test",
            { XDG_CACHE_HOME: "/cache" },
            join("/cache", "github-copilot-sdk", "runtime"),
        ],
        [
            "win32",
            "C:\\Users\\test",
            { LOCALAPPDATA: "C:\\Users\\test\\AppData\\Local" },
            join("C:\\Users\\test\\AppData\\Local", "github-copilot-sdk", "runtime"),
        ],
    ])("uses the %s user cache directory", (platform, home, environment, expected) => {
        expect(defaultRuntimeCacheRoot(platform, home, environment)).toBe(expected);
    });
});

describe("release runtime selection", () => {
    it("keeps the compiled CLI version aligned with package metadata", () => {
        const packageJson = JSON.parse(
            readFileSync(join(import.meta.dirname, "../package.json"), "utf8")
        );
        expect(COPILOT_CLI_VERSION).toBe(packageJson.copilotCliVersion);
        // lgtm[js/trivial-conditional] This generated constant is true for internal canary builds.
        if (COPILOT_CLI_USE_NPM_PACKAGE) {
            expect(packageJson.dependencies["@github/copilot"]).toBe(COPILOT_CLI_VERSION);
        } else {
            expect(packageJson.dependencies).not.toHaveProperty("@github/copilot");
        }
    });

    it("can pin an internal npm package without contacting GitHub Releases", () => {
        const root = mkdtempSync(join(tmpdir(), "copilot-cli-version-"));
        mkdirSync(join(root, "scripts"), { recursive: true });
        mkdirSync(join(root, "src"), { recursive: true });
        writeFileSync(join(root, "package.json"), "{}\n");
        writeFileSync(
            join(root, "scripts", "set-cli-version.js"),
            readFileSync(join(import.meta.dirname, "../scripts/set-cli-version.js"))
        );

        const result = spawnSync(
            process.execPath,
            [join(root, "scripts", "set-cli-version.js"), "9.9.9-canary.test", "--npm-package"],
            { encoding: "utf8" }
        );
        expect(result.status, result.stderr).toBe(0);
        expect(JSON.parse(readFileSync(join(root, "package.json"), "utf8"))).toMatchObject({
            copilotCliVersion: "9.9.9-canary.test",
        });
        expect(existsSync(join(root, "copilot-cli.json"))).toBe(false);
        expect(readFileSync(join(root, "src", "cliVersion.ts"), "utf8")).toContain(
            "COPILOT_CLI_USE_NPM_PACKAGE = true"
        );
    });

    it.each([
        ["darwin", "arm64", false, "darwin-arm64"],
        ["darwin", "x64", false, "darwin-x64"],
        ["linux", "arm64", false, "linux-arm64"],
        ["linux", "x64", true, "linuxmusl-x64"],
        ["win32", "arm64", false, "win32-arm64"],
    ])("maps %s/%s to %s", (platform, arch, musl, expected) => {
        expect(getRuntimePlatform(platform, arch, musl)).toBe(expected);
    });

    it("uses the platform npm tarball published in the CLI release", () => {
        expect(getRuntimeReleaseAssetName("1.2.3-4", "linux-x64")).toBe(
            "github-copilot-1.2.3-4-linux-x64.tgz"
        );
    });

    it("uses the SDK platform package namespace", () => {
        expect(getRuntimePackageName("linux-x64")).toBe("@github/copilot-sdk-linux-x64");
    });
});

describe("materializeRuntimeBundle", () => {
    afterEach(() => vi.unstubAllEnvs());

    it("materializes an adjacent pair from an absent cache with a stripped environment", () => {
        const sourceDir = mkdtempSync(join(tmpdir(), "copilot-runtime-source-"));
        const cacheRoot = join(sourceDir, "absent-cache");
        const emptyPath = join(sourceDir, "empty-path");
        mkdirSync(emptyPath);
        const platform = process.platform === "win32" ? "win32-x64" : "test-platform";
        const wrapperName = platform.startsWith("win32")
            ? "copilot-runtime.exe"
            : "copilot-runtime";
        const prebuilds = join(sourceDir, "prebuilds", platform);
        const wrapper = join(prebuilds, wrapperName);
        const runtimeNode = join(prebuilds, "runtime.node");
        mkdirSync(prebuilds, { recursive: true });
        writeFileSync(wrapper, "wrapper");
        writeFileSync(runtimeNode, "runtime");
        mkdirSync(join(sourceDir, "ripgrep", "bin", platform), { recursive: true });
        writeFileSync(join(sourceDir, "ripgrep", "bin", platform, "rg"), "ripgrep");
        mkdirSync(join(sourceDir, "definitions"), { recursive: true });
        writeFileSync(join(sourceDir, "definitions", "future.json"), "{}");
        mkdirSync(join(sourceDir, "copilot-sdk"), { recursive: true });
        writeFileSync(join(sourceDir, "copilot-sdk", "extension.js"), "extension SDK");
        mkdirSync(join(sourceDir, "preloads"), { recursive: true });
        writeFileSync(join(sourceDir, "preloads", "extension_bootstrap.mjs"), "bootstrap");
        mkdirSync(join(sourceDir, "sdk"), { recursive: true });
        writeFileSync(join(sourceDir, "sdk", "index.js"), "legacy SDK");
        writeFileSync(join(sourceDir, "app.js"), "excluded");
        writeFileSync(join(sourceDir, "copilot"), "excluded");
        writeFileSync(join(sourceDir, "copilot.exe"), "excluded");
        writeFileSync(join(sourceDir, "LICENSE.md"), "excluded");
        writeFileSync(join(sourceDir, "README.md"), "excluded");

        vi.stubEnv("PATH", emptyPath);
        vi.stubEnv("COPILOT_CLI_PATH", undefined);
        vi.stubEnv("COPILOT_RUNTIME_HOST_COMMAND", undefined);
        vi.stubEnv("COPILOT_RUNTIME_PROVIDER_LIB", undefined);

        expect(process.env.COPILOT_CLI_PATH).toBeUndefined();
        expect(process.env.COPILOT_RUNTIME_HOST_COMMAND).toBeUndefined();
        expect(process.env.COPILOT_RUNTIME_PROVIDER_LIB).toBeUndefined();

        const installedWrapper = materializeRuntimeBundle(
            { packageRoot: sourceDir, platform },
            cacheRoot
        );
        const installDir = resolve(dirname(installedWrapper), "..", "..");

        expect(readFileSync(installedWrapper, "utf8")).toBe("wrapper");
        expect(readFileSync(join(installDir, "prebuilds", platform, "runtime.node"), "utf8")).toBe(
            "runtime"
        );
        expect(readFileSync(join(installDir, "ripgrep", "bin", platform, "rg"), "utf8")).toBe(
            "ripgrep"
        );
        expect(readFileSync(join(installDir, "copilot-sdk", "extension.js"), "utf8")).toBe(
            "extension SDK"
        );
        expect(readFileSync(join(installDir, "preloads", "extension_bootstrap.mjs"), "utf8")).toBe(
            "bootstrap"
        );
        expect(readFileSync(join(installDir, "sdk", "index.js"), "utf8")).toBe("legacy SDK");
        expect(existsSync(join(installDir, "app.js"))).toBe(false);
        expect(existsSync(join(installDir, "copilot"))).toBe(false);
        expect(existsSync(join(installDir, "copilot.exe"))).toBe(false);
        expect(existsSync(join(installDir, "LICENSE.md"))).toBe(false);
        expect(existsSync(join(installDir, "README.md"))).toBe(false);
        if (process.platform !== "win32") {
            expect(statSync(installedWrapper).mode & 0o111).not.toBe(0);
        }
    });

    it("fails clearly when the package has no runtime.node", () => {
        const sourceDir = mkdtempSync(join(tmpdir(), "copilot-runtime-missing-node-"));
        const platform = process.platform === "win32" ? "win32-x64" : "test-platform";
        const wrapperName = platform.startsWith("win32")
            ? "copilot-runtime.exe"
            : "copilot-runtime";
        const prebuilds = join(sourceDir, "prebuilds", platform);
        const wrapper = join(prebuilds, wrapperName);
        mkdirSync(prebuilds, { recursive: true });
        writeFileSync(wrapper, "wrapper");

        expect(() =>
            materializeRuntimeBundle(
                {
                    packageRoot: sourceDir,
                    platform,
                },
                join(sourceDir, "cache")
            )
        ).toThrow(/Copilot runtime\.node not found/);
    });
});

describe("ensureRuntimeBundle", () => {
    it("resolves the installed platform runtime without network access", async () => {
        const root = mkdtempSync(join(tmpdir(), "copilot-packaged-runtime-"));
        const nodeModules = join(root, "node_modules");
        const platform = "linux-x64";
        const packageRoot = join(nodeModules, ...getRuntimePackageName(platform).split("/"));
        const prebuilds = join(packageRoot, "prebuilds", platform);
        mkdirSync(prebuilds, { recursive: true });
        writeFileSync(join(packageRoot, "package.json"), "{}");
        writeFileSync(join(prebuilds, "copilot-runtime"), "wrapper");
        writeFileSync(join(prebuilds, "runtime.node"), "runtime");
        mkdirSync(join(packageRoot, "schemas"));
        writeFileSync(join(packageRoot, "schemas", "api.schema.json"), "{}");
        const fetcher = vi.fn(() => {
            throw new Error("runtime resolution must not fetch");
        });
        vi.stubGlobal("fetch", fetcher);

        const runtimePath = await ensureRuntimeBundle(COPILOT_CLI_VERSION, {
            packageSearchPaths: [nodeModules],
            platform,
        });

        expect(runtimePath).toBe(join(prebuilds, "copilot-runtime"));
        expect(readFileSync(join(dirname(runtimePath), "runtime.node"), "utf8")).toBe("runtime");
        expect(fetcher).not.toHaveBeenCalled();
        vi.unstubAllGlobals();
    });

    it("fails clearly when the platform package is not installed", async () => {
        await expect(
            ensureRuntimeBundle(COPILOT_CLI_VERSION, {
                packageSearchPaths: [],
                platform: "linux-x64",
            })
        ).rejects.toThrow(
            "Could not resolve @github/copilot-sdk-linux-x64. Reinstall @github/copilot-sdk"
        );
    });
});

describe("release package acquisition", () => {
    it("downloads, verifies, and caches a release package for packaging", async () => {
        const sourceRoot = mkdtempSync(join(tmpdir(), "copilot-release-source-"));
        const packageRoot = join(sourceRoot, "package");
        const platform = "linux-x64";
        const prebuilds = join(packageRoot, "prebuilds", platform);
        mkdirSync(prebuilds, { recursive: true });
        writeFileSync(join(prebuilds, "copilot-runtime"), "wrapper");
        writeFileSync(join(prebuilds, "runtime.node"), "runtime");
        mkdirSync(join(packageRoot, "schemas"), { recursive: true });
        writeFileSync(join(packageRoot, "schemas", "api.schema.json"), "{}");

        const archivePath = join(sourceRoot, "runtime.tgz");
        await createTar({ cwd: sourceRoot, file: archivePath, gzip: true }, ["package"]);
        const archive = readFileSync(archivePath);
        const version = "1.2.3-4";
        const assetName = getRuntimeReleaseAssetName(version, platform);
        const checksum = createHash("sha256").update(archive).digest("hex");
        const fetcher = vi.fn(async (input: string | URL | Request) =>
            String(input).endsWith("/SHA256SUMS.txt")
                ? new Response(`${checksum}  ${assetName}\n`)
                : new Response(archive)
        );
        const cacheRoot = join(sourceRoot, "cache");

        const downloadedPackage = await ensureCopilotPackage(version, {
            cacheRoot,
            fetch: fetcher,
            platform,
        });
        expect(readFileSync(join(downloadedPackage, "schemas", "api.schema.json"), "utf8")).toBe(
            "{}"
        );

        await expect(
            ensureCopilotPackage(version, {
                cacheRoot,
                fetch: fetcher,
                platform,
            })
        ).resolves.toBe(downloadedPackage);
        expect(fetcher).toHaveBeenCalledTimes(2);
    });

    it("rejects a release package that does not match SHA256SUMS.txt", async () => {
        const cacheRoot = mkdtempSync(join(tmpdir(), "copilot-release-mismatch-"));
        const assetName = getRuntimeReleaseAssetName("1.2.3", "linux-x64");
        const fetcher = vi.fn(async (input: string | URL | Request) =>
            String(input).endsWith("/SHA256SUMS.txt")
                ? new Response(`${"0".repeat(64)}  ${assetName}\n`)
                : new Response("corrupt archive")
        );

        await expect(
            ensureCopilotPackage("1.2.3", {
                cacheRoot,
                fetch: fetcher,
                platform: "linux-x64",
            })
        ).rejects.toThrow("Checksum mismatch");
        expect(existsSync(join(cacheRoot, "1.2.3", "packages", "linux-x64"))).toBe(false);
    });

    it("times out and retries a stalled release download", async () => {
        const cacheRoot = mkdtempSync(join(tmpdir(), "copilot-release-timeout-"));
        const fetcher = vi.fn(
            (_input: string | URL | Request, init?: RequestInit) =>
                new Promise<Response>((_resolve, reject) => {
                    const signal = init?.signal;
                    if (!signal) {
                        reject(new Error("Expected a request timeout signal."));
                        return;
                    }
                    signal.addEventListener("abort", () => reject(signal.reason), { once: true });
                })
        );

        await expect(
            ensureCopilotPackage("1.2.3-timeout", {
                cacheRoot,
                fetch: fetcher,
                fetchTimeoutMs: 10,
                platform: "linux-x64",
            })
        ).rejects.toThrow("Failed to download");
        expect(fetcher).toHaveBeenCalledTimes(3);
    });
});
