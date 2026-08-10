import { describe, expect, it, vi } from "vitest";
import { assertVersionAbsent, publishTarball } from "../scripts/npm-release.js";

const packageName = "@github/copilot-sdk";
const version = "1.2.3";
const registry = "https://registry.example.test";
const result = (status: number, stdout = "", stderr = "") => ({ status, stdout, stderr });

describe("npm release preflight", () => {
    it("succeeds only for a structured E404 response", async () => {
        const runner = vi
            .fn()
            .mockResolvedValue(result(1, JSON.stringify({ error: { code: "E404" } })));
        await expect(
            assertVersionAbsent(packageName, version, registry, runner)
        ).resolves.toBeUndefined();
    });

    it.each([
        ["an existing version", result(0, JSON.stringify(version)), "already exists"],
        ["a transient error", result(1, "", "npm error code E500"), "Could not confirm"],
        ["malformed output", result(1, "not-json"), "Could not confirm"],
        [
            "a non-404 error containing E404 and 404 text",
            result(
                1,
                JSON.stringify({ error: { code: "E500", summary: "version 1.2.3-E404.404" } }),
                "npm error code E500 for 1.2.3-E404.404"
            ),
            "Could not confirm",
        ],
    ])("fails for %s", async (_name, response, message) => {
        const runner = vi.fn().mockResolvedValue(response);
        await expect(assertVersionAbsent(packageName, version, registry, runner)).rejects.toThrow(
            message
        );
    });
});

describe("npm release publishing", () => {
    it("succeeds after a normal publish", async () => {
        const runner = vi.fn().mockResolvedValue(result(0));
        await expect(
            publishTarball("package.tgz", "latest", registry, "public", runner)
        ).resolves.toBeUndefined();
    });

    it.each([
        ["npm error code EPUBLISHCONFLICT", "public"],
        [
            "npm error 403 403 Forbidden - PUT https://registry.npmjs.org/package - You cannot publish over the previously published versions: 1.2.3.",
            "public",
        ],
        [
            "npm error 403 403 Forbidden - The feed 'copilot-canary' already contains file 'copilot-sdk-0.0.0-29613896246.tgz' in package '@github/copilot-sdk 0.0.0-29613896246'.",
            "azure",
        ],
    ])("recovers the immutable conflict: %s", async (error, mode) => {
        const runner = vi.fn().mockResolvedValue(result(1, "", error));
        await expect(
            publishTarball("package.tgz", "latest", registry, mode, runner)
        ).resolves.toBeUndefined();
    });

    it.each([
        ["a generic Azure 403", "403 Forbidden", "azure"],
        [
            "an Azure non-tarball conflict",
            "npm error 403 already contains file 'package.json' in package '@github/copilot-sdk/1.2.3'",
            "azure",
        ],
        [
            "an embedded public phrase",
            "npm error network timeout while parsing 'cannot publish over the previously published versions'",
            "public",
        ],
        [
            "an embedded Azure phrase",
            "npm error network timeout while parsing \"already contains file 'package.tgz' in package '@github/copilot-sdk/1.2.3'\"",
            "azure",
        ],
    ])("fails for %s", async (_name, error, mode) => {
        const runner = vi.fn().mockResolvedValue(result(1, "", error));
        await expect(
            publishTarball("package.tgz", "latest", registry, mode, runner)
        ).rejects.toThrow("npm publish failed");
    });
});
