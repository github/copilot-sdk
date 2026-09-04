import { createHash } from "node:crypto";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it, vi } from "vitest";
import {
    assertPublishedIntegrity,
    assertVersionAbsent,
    publishManifest,
    publishTarball,
} from "../scripts/npm-release.js";

const packageName = "@github/copilot-sdk";
const version = "1.2.3-unstable.7.gabcdef0";
const registry = "https://registry.example.test";
const integrity = "sha512-expected";
const identity = { name: packageName, version, integrity };
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

    it("accepts an existing package only when integrity matches", async () => {
        const matching = vi.fn().mockResolvedValue(result(0, JSON.stringify(integrity)));
        await expect(
            assertPublishedIntegrity(packageName, version, integrity, registry, matching)
        ).resolves.toBe("matching");

        const conflicting = vi
            .fn()
            .mockResolvedValue(result(0, JSON.stringify("sha512-conflicting")));
        await expect(
            assertPublishedIntegrity(packageName, version, integrity, registry, conflicting)
        ).rejects.toThrow("has integrity sha512-conflicting");
    });

    it("does not treat malformed or transient failures as absence", async () => {
        const runner = vi.fn().mockResolvedValue(result(1, "not-json", "npm error code E500"));
        await expect(assertVersionAbsent(packageName, version, registry, runner)).rejects.toThrow(
            "Could not read"
        );
    });
});

describe("npm release publishing", () => {
    it("verifies registry integrity after a normal publish", async () => {
        const runner = vi
            .fn()
            .mockResolvedValueOnce(result(0))
            .mockResolvedValueOnce(result(0, JSON.stringify(integrity)));
        await expect(
            publishTarball("package.tgz", "unstable", registry, "public", identity, runner)
        ).resolves.toBeUndefined();
    });

    it("recovers a publication conflict only when registry integrity matches", async () => {
        const runner = vi
            .fn()
            .mockResolvedValueOnce(result(1, "", "EPUBLISHCONFLICT"))
            .mockResolvedValueOnce(result(0, JSON.stringify(integrity)));
        await expect(
            publishTarball("package.tgz", "unstable", registry, "public", identity, runner)
        ).resolves.toBeUndefined();
    });

    it("fails a publication conflict with different content", async () => {
        const runner = vi
            .fn()
            .mockResolvedValueOnce(result(1, "", "EPUBLISHCONFLICT"))
            .mockResolvedValueOnce(result(0, JSON.stringify("sha512-other")));
        await expect(
            publishTarball("package.tgz", "unstable", registry, "public", identity, runner)
        ).rejects.toThrow("sha512-other");
    });

    it("preflights all packages, publishes platforms before the umbrella, and tags last", async () => {
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
                const name = args[1].slice(0, args[1].lastIndexOf("@"));
                const packed = packages.find((candidate) => candidate.name === name);
                if (args[2] === "version") {
                    return result(0, JSON.stringify(version));
                }
                return result(
                    calls
                        .filter((call) => call[0] === "publish")
                        .some((call) => call[1].includes(packed!.filename))
                        ? 0
                        : 1,
                    calls
                        .filter((call) => call[0] === "publish")
                        .some((call) => call[1].includes(packed!.filename))
                        ? JSON.stringify(packed!.integrity)
                        : JSON.stringify({ error: { code: "E404" } })
                );
            }
            return result(0);
        });

        try {
            await publishManifest(manifestPath, directory, "unstable", registry, "public", runner);
            const publishCalls = calls.filter((args) => args[0] === "publish");
            expect(publishCalls).toHaveLength(9);
            expect(publishCalls.at(-1)?.[1]).toContain("package-0.tgz");
            expect(calls.filter((args) => args[0] === "dist-tag")).toHaveLength(0);
            expect(
                Math.max(
                    ...calls.map((args, index) =>
                        args[0] === "view" && args[2] === "version" ? index : -1
                    )
                )
            ).toBeGreaterThan(calls.map((args) => args[0]).lastIndexOf("publish"));

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
            const missingTagRunner = vi.fn(async (_command: string, args: string[]) => {
                const name = args[1].slice(0, args[1].lastIndexOf("@"));
                const packed = packages.find((candidate) => candidate.name === name)!;
                return args[2] === "version"
                    ? result(1, JSON.stringify({ error: { code: "E404" } }))
                    : result(0, JSON.stringify(packed.integrity));
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
