import { createHash } from "node:crypto";
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
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
        ["a public conflict from Azure", "npm error code EPUBLISHCONFLICT", "azure"],
        [
            "an Azure conflict from public npm",
            "npm error 403 https://pkgs.dev.azure.com/example - The feed 'copilot-canary' already contains file 'package.tgz' in package '@github/copilot-sdk'.",
            "public",
        ],
        ["an unrelated npm failure", "npm error E500", "public"],
    ])("rejects %s", async (_name, error, mode) => {
        const runner = vi.fn().mockResolvedValue(result(1, "", error));
        await expect(
            publishTarball("package.tgz", "unstable", registry, mode, runner)
        ).rejects.toThrow("npm publish failed");
    });

    it("validates locally and publishes nine exact tarballs sequentially with the umbrella last", async () => {
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
        let activePublishes = 0;
        let maxActivePublishes = 0;
        const runner = vi.fn(async (_command: string, args: string[]) => {
            calls.push(args);
            activePublishes++;
            maxActivePublishes = Math.max(maxActivePublishes, activePublishes);
            await Promise.resolve();
            activePublishes--;
            return result(0);
        });

        try {
            await publishManifest(manifestPath, directory, "unstable", registry, "public", runner);
            expect(calls).toHaveLength(9);
            expect(calls.every((args) => args[0] === "publish")).toBe(true);
            expect(calls.map((args) => args[1])).toEqual([
                ...packages.slice(1).map(({ filename }) => resolve(directory, filename)),
                resolve(directory, packages[0].filename),
            ]);
            expect(maxActivePublishes).toBe(1);

            writeFileSync(join(directory, packages[1].filename), "tampered");
            runner.mockClear();
            await expect(
                publishManifest(manifestPath, directory, "unstable", registry, "public", runner)
            ).rejects.toThrow(/Size mismatch|Integrity mismatch/);
            expect(runner).not.toHaveBeenCalled();
        } finally {
            rmSync(directory, { recursive: true, force: true });
        }
    });
});

const workflow = readFileSync(
    resolve(dirname(fileURLToPath(import.meta.url)), "../../.github/workflows/publish.yml"),
    "utf8"
).replaceAll("\r\n", "\n");

function workflowJob(jobId: string): string {
    const marker = `  ${jobId}:\n`;
    const start = workflow.indexOf(marker);
    if (start < 0) throw new Error(`Workflow job not found: ${jobId}`);
    const rest = workflow.slice(start + marker.length);
    const nextJob = rest.search(/^  [a-z0-9-]+:\n/m);
    return nextJob < 0 ? rest : rest.slice(0, nextJob);
}

describe("runtime-backed npm publishing workflow", () => {
    it.each(["runtime-publish-internal", "runtime-publish-public"])(
        "%s validates retained packages before using the shared publisher",
        (jobId) => {
            const job = workflowJob(jobId);
            const validation = job.indexOf("- name: Validate retained release");
            const publication = job.indexOf("npm-release.js publish-manifest");

            expect(validation).toBeGreaterThanOrEqual(0);
            expect(publication).toBeGreaterThan(validation);
            expect(job.match(/npm-release\.js publish-manifest/g)).toHaveLength(1);
        }
    );
});
