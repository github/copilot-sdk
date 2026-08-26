import { existsSync, mkdtempSync, mkdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { afterEach, describe, expect, it, vi } from "vitest";

import { materializeRuntimeBundle } from "../src/runtimeArtifacts.js";

describe("materializeRuntimeBundle", () => {
    afterEach(() => vi.unstubAllEnvs());

    it("materializes an adjacent pair from an absent cache with a stripped environment", () => {
        const sourceDir = mkdtempSync(join(tmpdir(), "copilot-runtime-source-"));
        const cacheRoot = join(sourceDir, "absent-cache");
        const emptyPath = join(sourceDir, "empty-path");
        mkdirSync(emptyPath);
        const wrapperName =
            process.platform === "win32" ? "copilot-runtime.exe" : "copilot-runtime";
        const prebuilds = join(sourceDir, "prebuilds", "test-platform");
        const wrapper = join(prebuilds, wrapperName);
        const runtimeNode = join(prebuilds, "runtime.node");
        mkdirSync(prebuilds, { recursive: true });
        writeFileSync(wrapper, "wrapper");
        writeFileSync(runtimeNode, "runtime");
        mkdirSync(join(sourceDir, "ripgrep", "bin", "test-platform"), { recursive: true });
        writeFileSync(join(sourceDir, "ripgrep", "bin", "test-platform", "rg"), "ripgrep");
        mkdirSync(join(sourceDir, "definitions"), { recursive: true });
        writeFileSync(join(sourceDir, "definitions", "future.json"), "{}");
        writeFileSync(join(sourceDir, "app.js"), "excluded");
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
            { packageRoot: sourceDir, platform: "test-platform" },
            cacheRoot
        );
        const installDir = dirname(installedWrapper);

        expect(readFileSync(installedWrapper, "utf8")).toBe("wrapper");
        expect(readFileSync(join(installDir, "runtime.node"), "utf8")).toBe("runtime");
        expect(
            readFileSync(join(installDir, "ripgrep", "bin", "test-platform", "rg"), "utf8")
        ).toBe("ripgrep");
        expect(existsSync(join(installDir, "app.js"))).toBe(false);
        expect(existsSync(join(installDir, "LICENSE.md"))).toBe(false);
        expect(existsSync(join(installDir, "README.md"))).toBe(false);
        if (process.platform !== "win32") {
            expect(statSync(installedWrapper).mode & 0o111).not.toBe(0);
        }
    });

    it("fails clearly when the package has no runtime.node", () => {
        const sourceDir = mkdtempSync(join(tmpdir(), "copilot-runtime-missing-node-"));
        const wrapperName =
            process.platform === "win32" ? "copilot-runtime.exe" : "copilot-runtime";
        const prebuilds = join(sourceDir, "prebuilds", "test-platform");
        const wrapper = join(prebuilds, wrapperName);
        mkdirSync(prebuilds, { recursive: true });
        writeFileSync(wrapper, "wrapper");

        expect(() =>
            materializeRuntimeBundle(
                {
                    packageRoot: sourceDir,
                    platform: "test-platform",
                },
                join(sourceDir, "cache")
            )
        ).toThrow(/Copilot runtime\.node not found/);
    });
});
