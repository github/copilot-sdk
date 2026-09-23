/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawnSync } from "node:child_process";
import {
    copyFileSync,
    existsSync,
    mkdirSync,
    mkdtempSync,
    readFileSync,
    readdirSync,
    rmSync,
    symlinkSync,
    writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { x as extractTar } from "tar";
import { describe, expect, it, onTestFinished } from "vitest";
import { COPILOT_CLI_VERSION } from "../src/cliVersion.js";
import { RUNTIME_PLATFORMS } from "../src/runtimeArtifacts.js";

describe("release packaging", () => {
    it.each([
        { source: "checkout", platforms: ["linux-x64"] },
        { source: "published", platforms: [...RUNTIME_PLATFORMS] },
    ])(
        "packs and verifies the $source runtime without downloads",
        async ({ source, platforms }) => {
            const root = mkdtempSync(join(tmpdir(), "copilot-sdk-packaging-"));
            onTestFinished(() => rmSync(root, { recursive: true, force: true }));
            const nodeRoot = join(root, "src/sdk/nodejs");
            const sourceNodeRoot = resolve(import.meta.dirname, "..");
            const write = (path: string, contents: string) => {
                mkdirSync(dirname(path), { recursive: true });
                writeFileSync(path, contents);
            };
            for (const file of [
                "scripts/package-sdk.ts",
                "scripts/verify-release-packages.ts",
                "scripts/dependency-policy.ts",
                "scripts/prepare-runtime.ts",
                "scripts/releaseArtifacts.ts",
                "src/runtimeArtifacts.ts",
                "src/cliVersion.ts",
                "../scripts/runtime-layout.mjs",
            ]) {
                const destination = join(nodeRoot, file);
                mkdirSync(dirname(destination), { recursive: true });
                copyFileSync(join(sourceNodeRoot, file), destination);
            }
            symlinkSync(
                join(sourceNodeRoot, "node_modules"),
                join(nodeRoot, "node_modules"),
                "junction"
            );
            const manifest = JSON.stringify({
                name: "@github/copilot-sdk",
                version: "0.0.0-packaging-test",
                type: "module",
                repository: "https://github.com/github/copilot-sdk.git",
                dependencies: {},
                files: ["dist"],
                scripts: {
                    "pack:release": "tsx scripts/package-sdk.ts",
                    "verify:release-packages": "tsx scripts/verify-release-packages.ts",
                },
            });
            write(join(nodeRoot, "package.json"), manifest);
            write(join(nodeRoot, "dist/index.js"), "export {};");
            write(join(nodeRoot, "dist/cjs/index.js"), "module.exports = {};");

            const environment = { ...process.env };
            for (const key of [
                "COPILOT_RUNTIME_SOURCE",
                "COPILOT_SDK_RUNTIME_PLATFORMS",
                "COPILOT_SDK_RUNTIME_PACKAGE_DIR",
                "COPILOT_CLI_PATH",
                "COPILOT_LEGACY_CLI_PATH",
            ]) {
                delete environment[key];
            }
            environment.HOME = join(root, "home");
            environment.LOCALAPPDATA = join(root, "cache");
            environment.XDG_CACHE_HOME = join(root, "cache");
            const blockNetwork = join(root, "block-network.mjs");
            write(
                blockNetwork,
                `import { writeFileSync } from "node:fs";
globalThis.fetch = async (url) => {
    writeFileSync(${JSON.stringify(join(root, "download-attempt"))}, String(url));
    return new Response(null, { status: 404, statusText: "Unexpected published runtime download" });
};`
            );
            environment.NODE_OPTIONS = `${environment.NODE_OPTIONS ?? ""} --import="${pathToFileURL(blockNetwork).href}"`;
            if (source === "checkout") {
                write(join(root, "script/sea-build.ts"), "");
                environment.COPILOT_SDK_RUNTIME_PLATFORMS = platforms.join(",");
                environment.COPILOT_CLI_PATH = join(
                    root,
                    "prepared/package/prebuilds/linux-x64/copilot-runtime"
                );
            } else {
                environment.COPILOT_SDK_RUNTIME_PACKAGE_DIR = join(root, "acquired");
            }
            const packageRoots = platforms.map((platform) => {
                const packageRoot =
                    source === "checkout"
                        ? join(root, "prepared/package")
                        : join(root, "acquired", platform);
                const wrapper = platform.startsWith("win32")
                    ? "copilot-runtime.exe"
                    : "copilot-runtime";
                write(
                    join(packageRoot, "package.json"),
                    JSON.stringify({ version: COPILOT_CLI_VERSION })
                );
                write(
                    join(packageRoot, "prebuilds", platform, wrapper),
                    `${source} wrapper: ${platform}`
                );
                write(
                    join(packageRoot, "prebuilds", platform, "runtime.node"),
                    `${source} runtime: ${platform}`
                );
                write(join(packageRoot, "copilot-sdk/extension.js"), "extension SDK");
                write(join(packageRoot, "preloads/extension_bootstrap.mjs"), "bootstrap");
                return packageRoot;
            });
            const npmCliPath = process.env.npm_execpath;
            if (!npmCliPath) {
                throw new Error("Run packaging tests through npm test");
            }
            const run = (script: string) =>
                spawnSync(process.execPath, [npmCliPath, "run", script], {
                    cwd: nodeRoot,
                    env: environment,
                    encoding: "utf8",
                    timeout: 60_000,
                });
            const diagnostics = ({ error, stdout, stderr }: ReturnType<typeof run>) =>
                `${error ?? ""}\n${stdout}\n${stderr}`;
            const pack = run("pack:release");
            expect(pack.status, diagnostics(pack)).toBe(0);
            expect(existsSync(join(root, "download-attempt"))).toBe(false);
            expect(readFileSync(join(nodeRoot, "package.json"), "utf8")).toBe(manifest);
            expect(readdirSync(nodeRoot).filter((name) => name.endsWith(".tgz"))).toHaveLength(
                platforms.length + 1
            );
            const verify = run("verify:release-packages");
            expect(verify.status, diagnostics(verify)).toBe(0);

            const unpacked = join(root, "unpacked");
            mkdirSync(unpacked);
            await extractTar({
                file: join(nodeRoot, "github-copilot-sdk-linux-x64-0.0.0-packaging-test.tgz"),
                cwd: unpacked,
            });
            expect(
                readFileSync(join(unpacked, "package/prebuilds/linux-x64/runtime.node"), "utf8")
            ).toBe(`${source} runtime: linux-x64`);

            rmSync(join(packageRoots[0], "copilot-sdk/extension.js"));
            const incompletePack = run("pack:release");
            expect(incompletePack.status, diagnostics(incompletePack)).toBe(0);
            const incompleteVerify = run("verify:release-packages");
            expect(incompleteVerify.status, diagnostics(incompleteVerify)).not.toBe(0);
            expect(incompleteVerify.stderr).toContain(
                "is missing package/copilot-sdk/extension.js"
            );
        }
    );
});
