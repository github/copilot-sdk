import { createHash } from "node:crypto";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it, vi } from "vitest";
import { assertVersionAbsent, publishManifest, publishTarball } from "../scripts/npm-release.js";

const packageName = "@github/copilot-sdk";
const version = "1.2.3-unstable.34640000001.gabcdef0";
const registry = "https://registry.example.test";
const identity = { name: packageName, version };
const result = (status: number, stdout = "", stderr = "") => ({ status, stdout, stderr });

describe("npm release preflight", () => {
    it("recognizes only a structured E404 as absent", async () => {
        const runner = vi
            .fn()
            .mockResolvedValue(result(1, JSON.stringify({ error: { code: "E404" } })));
        await expect(
            assertVersionAbsent(packageName, version, registry, runner)
        ).resolves.toBeUndefined();
    });

    it.each([
        ["an existing version", result(0, JSON.stringify(version)), "already exists"],
        ["a transient error", result(1, "", "npm error code E500"), "Could not read"],
        ["malformed output", result(1, "not-json"), "Could not read"],
        [
            "a non-404 error containing E404 and 404 text",
            result(
                1,
                JSON.stringify({ error: { code: "E500", summary: "version 1.2.3-E404.404" } }),
                "npm error code E500 for 1.2.3-E404.404"
            ),
            "Could not read",
        ],
    ])("fails for %s", async (_name, response, message) => {
        const runner = vi.fn().mockResolvedValue(response);
        await expect(assertVersionAbsent(packageName, version, registry, runner)).rejects.toThrow(
            message
        );
    });
});

describe("npm release publishing", () => {
    it("treats a successful publish as success without registry metadata", async () => {
        const runner = vi.fn().mockResolvedValue(result(0));
        await expect(
            publishTarball("package.tgz", "unstable", registry, "public", runner)
        ).resolves.toBeUndefined();
        expect(runner).toHaveBeenCalledTimes(1);
    });

    it("accepts recognized immutable-version conflicts without registry integrity", async () => {
        const runner = vi.fn().mockResolvedValue(result(1, "", "npm error code EPUBLISHCONFLICT"));
        await expect(
            publishTarball("package.tgz", "unstable", registry, "public", runner, identity)
        ).resolves.toBeUndefined();

        runner.mockResolvedValue(
            result(
                1,
                "",
                "npm error 403 https://pkgs.dev.azure.com/example - The feed 'copilot-canary' already contains file 'package.tgz' in package '@github/copilot-sdk'."
            )
        );
        await expect(
            publishTarball("package.tgz", "canary", registry, "azure", runner, identity)
        ).resolves.toBeUndefined();
        expect(runner).toHaveBeenCalledTimes(2);
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
        ["an unrelated npm failure", "npm error E500", "public"],
    ])("rejects %s", async (_name, error, mode) => {
        const runner = vi.fn().mockResolvedValue(result(1, "", error));
        await expect(
            publishTarball("package.tgz", "unstable", registry, mode, runner)
        ).rejects.toThrow("npm publish failed");
    });

    it("validates all packages, publishes platforms before the umbrella, and tags last", async () => {
        const directory = mkdtempSync(join(tmpdir(), "copilot-sdk-npm-release-"));
        mkdirSync(directory, { recursive: true });
        const packages = [
            "@github/copilot-sdk",
            ...[
                "darwin-arm64",
                "darwin-x64",
                "linux-arm64",
                "linux-x64",
                "linuxmusl-arm64",
                "linuxmusl-x64",
                "win32-arm64",
                "win32-x64",
            ].map((platform) => `@github/copilot-sdk-${platform}`),
        ].map((name, index) => {
            const filename = `package-${index}.tgz`;
            const bytes = Buffer.from(name);
            writeFileSync(join(directory, filename), bytes);
            return {
                filename,
                integrity: `sha512-${createHash("sha512").update(bytes).digest("base64")}`,
                name,
                size: bytes.length,
            };
        });
        const manifestPath = join(directory, "release-manifest.json");
        writeFileSync(
            manifestPath,
            JSON.stringify({ schemaVersion: 1, sdk: { version }, packages })
        );
        const calls: string[][] = [];
        const runner = vi.fn(async (_command: string, args: string[]) => {
            calls.push(args);
            if (args[0] === "view") {
                return result(0, JSON.stringify(version));
            }
            return result(0);
        });

        try {
            await publishManifest(manifestPath, directory, "unstable", registry, "public", runner);
            const publishCalls = calls.filter((args) => args[0] === "publish");
            expect(publishCalls).toHaveLength(9);
            expect(publishCalls.at(-1)?.[1]).toContain("package-0.tgz");
            expect(calls.filter((args) => args[0] === "dist-tag")).toHaveLength(0);
            expect(calls.some((args) => args.includes("dist.integrity"))).toBe(false);

            const staleTagRunner = vi.fn(async (_command: string, args: string[]) => {
                const name = args[1].slice(0, args[1].lastIndexOf("@"));
                const packed = packages.find((candidate) => candidate.name === name)!;
                return result(
                    0,
                    JSON.stringify(args[2] === "version" ? "9.0.0-unstable.1" : packed.integrity)
                );
            });
            await expect(
                publishManifest(
                    manifestPath,
                    directory,
                    "unstable",
                    registry,
                    "public",
                    staleTagRunner
                )
            ).rejects.toThrow("refusing to rewind");
            await expect(
                publishManifest(
                    manifestPath,
                    directory,
                    "unstable",
                    registry,
                    "azure",
                    staleTagRunner
                )
            ).rejects.toThrow("refusing to rewind");
            const azureConflictRunner = vi.fn(async (_command: string, args: string[]) =>
                args[0] === "view"
                    ? result(0, JSON.stringify(version))
                    : result(
                          1,
                          "",
                          "npm error 403 https://pkgs.dev.azure.com/example - The feed 'copilot-canary' already contains file 'package.tgz' in package '@github/copilot-sdk'."
                      )
            );
            await expect(
                publishManifest(
                    manifestPath,
                    directory,
                    "unstable",
                    registry,
                    "azure",
                    azureConflictRunner
                )
            ).resolves.toBeUndefined();
            expect(
                azureConflictRunner.mock.calls.some(([, args]) => args.includes("dist.integrity"))
            ).toBe(false);
            const missingTagRunner = vi.fn(async (_command: string, args: string[]) => {
                return args[0] === "view"
                    ? result(1, JSON.stringify({ error: { code: "E404" } }))
                    : result(0);
            });
            await expect(
                publishManifest(
                    manifestPath,
                    directory,
                    "unstable",
                    registry,
                    "public",
                    missingTagRunner
                )
            ).rejects.toThrow("Public trusted publishing cannot repair dist-tags");
        } finally {
            rmSync(directory, { recursive: true, force: true });
        }
    });
});
