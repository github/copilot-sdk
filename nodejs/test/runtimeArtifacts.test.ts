import { mkdtempSync, mkdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
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
        const wrapper = join(sourceDir, wrapperName);
        const runtimeNode = join(sourceDir, "runtime.node");
        writeFileSync(wrapper, "wrapper");
        writeFileSync(runtimeNode, "runtime");

        vi.stubEnv("PATH", emptyPath);
        vi.stubEnv("COPILOT_CLI_PATH", undefined);
        vi.stubEnv("COPILOT_RUNTIME_HOST_COMMAND", undefined);
        vi.stubEnv("COPILOT_RUNTIME_PROVIDER_LIB", undefined);

        expect(process.env.COPILOT_CLI_PATH).toBeUndefined();
        expect(process.env.COPILOT_RUNTIME_HOST_COMMAND).toBeUndefined();
        expect(process.env.COPILOT_RUNTIME_PROVIDER_LIB).toBeUndefined();

        const installedWrapper = materializeRuntimeBundle(
            { wrapper, runtimeNode, platform: "test-platform" },
            cacheRoot
        );
        const installDir = dirname(installedWrapper);

        expect(readFileSync(installedWrapper, "utf8")).toBe("wrapper");
        expect(readFileSync(join(installDir, "runtime.node"), "utf8")).toBe("runtime");
        if (process.platform !== "win32") {
            expect(statSync(installedWrapper).mode & 0o111).not.toBe(0);
        }
    });

    it("fails clearly when the package has no runtime.node", () => {
        const sourceDir = mkdtempSync(join(tmpdir(), "copilot-runtime-missing-node-"));
        const wrapperName =
            process.platform === "win32" ? "copilot-runtime.exe" : "copilot-runtime";
        const wrapper = join(sourceDir, wrapperName);
        const runtimeNode = join(sourceDir, "runtime.node");
        writeFileSync(wrapper, "wrapper");

        expect(() =>
            materializeRuntimeBundle(
                {
                    wrapper,
                    runtimeNode,
                    platform: "test-platform",
                },
                join(sourceDir, "cache")
            )
        ).toThrow(/Copilot runtime\.node not found/);
    });
});
