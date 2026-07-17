import { createHash } from "node:crypto";
import { describe, expect, it, vi } from "vitest";
import { assertVersionAbsent, publishTarball } from "../scripts/npm-release.js";

function integrity(value: string) {
    return `sha512-${createHash("sha512").update(value).digest("base64")}`;
}

const packageInfo = {
    name: "@github/copilot-sdk",
    version: "1.2.3",
    integrity: integrity("match"),
};

function result(status: number, stdout = "", stderr = "") {
    return { status, stdout, stderr };
}

describe("npm release preflight", () => {
    it("succeeds only for a definitive not-found response", async () => {
        const runner = vi.fn().mockResolvedValue(
            result(
                1,
                JSON.stringify({
                    error: { code: "E404", summary: "No match found for version 1.2.3" },
                }),
                "npm error code E404\nnpm error 404"
            )
        );
        await expect(
            assertVersionAbsent({
                packageName: packageInfo.name,
                version: packageInfo.version,
                registry: "https://registry.npmjs.org",
                runner,
            })
        ).resolves.toBeUndefined();
    });

    it("fails when the version already exists", async () => {
        const runner = vi.fn().mockResolvedValue(result(0, JSON.stringify(packageInfo.version)));
        await expect(
            assertVersionAbsent({
                packageName: packageInfo.name,
                version: packageInfo.version,
                registry: "https://registry.npmjs.org",
                runner,
            })
        ).rejects.toThrow("already exists");
    });

    it("fails on transient registry errors", async () => {
        const runner = vi.fn().mockResolvedValue(result(1, "", "npm error code E500"));
        await expect(
            assertVersionAbsent({
                packageName: packageInfo.name,
                version: packageInfo.version,
                registry: "https://registry.npmjs.org",
                runner,
            })
        ).rejects.toThrow("Could not confirm");
    });

    it("fails when a non-404 error contains E404 and 404 text", async () => {
        const runner = vi.fn().mockResolvedValue(
            result(
                1,
                JSON.stringify({
                    error: {
                        code: "E500",
                        summary: "Failed to query version 1.2.3-E404.404",
                    },
                }),
                "npm error code E500 for version 1.2.3-E404.404"
            )
        );
        await expect(
            assertVersionAbsent({
                packageName: packageInfo.name,
                version: "1.2.3-E404.404",
                registry: "https://registry.npmjs.org",
                runner,
            })
        ).rejects.toThrow("Could not confirm");
    });

    it("fails on malformed registry metadata", async () => {
        const runner = vi.fn().mockResolvedValue(result(0, "not-json"));
        await expect(
            assertVersionAbsent({
                packageName: packageInfo.name,
                version: packageInfo.version,
                registry: "https://registry.npmjs.org",
                runner,
            })
        ).rejects.toThrow("malformed version metadata");
    });
});

describe("npm release publishing", () => {
    const baseOptions = {
        tarball: "package.tgz",
        registry: "https://registry.example.test",
        tag: "latest",
        inspect: vi.fn().mockResolvedValue(packageInfo),
        sleep: vi.fn().mockResolvedValue(undefined),
        retryDelaysMs: [0, 1],
    };

    it("succeeds after a normal publish", async () => {
        const runner = vi.fn().mockResolvedValue(result(0));
        await expect(publishTarball({ ...baseOptions, runner })).resolves.toEqual({
            recoveredConflict: false,
        });
    });

    it("recovers a public conflict only when integrity matches", async () => {
        const runner = vi
            .fn()
            .mockResolvedValueOnce(result(1, "", "npm error code EPUBLISHCONFLICT"))
            .mockResolvedValueOnce(result(0, JSON.stringify(packageInfo.integrity)));
        await expect(publishTarball({ ...baseOptions, runner })).resolves.toEqual({
            recoveredConflict: true,
        });
    });

    it("recovers the known public immutable-version diagnostic", async () => {
        const runner = vi
            .fn()
            .mockResolvedValueOnce(
                result(
                    1,
                    "",
                    "npm error 403 403 Forbidden - PUT https://registry.npmjs.org/@github%2fcopilot-sdk - You cannot publish over the previously published versions: 1.2.3."
                )
            )
            .mockResolvedValueOnce(result(0, JSON.stringify(packageInfo.integrity)));
        await expect(publishTarball({ ...baseOptions, runner })).resolves.toEqual({
            recoveredConflict: true,
        });
    });

    it("recovers an Azure tarball conflict only when integrity matches", async () => {
        const runner = vi
            .fn()
            .mockResolvedValueOnce(
                result(
                    1,
                    "",
                    "npm error 403 403 Forbidden - PUT https://pkgs.dev.azure.com/feed - already contains file 'github-copilot-sdk-1.2.3.tgz' in package '@github/copilot-sdk/1.2.3'"
                )
            )
            .mockResolvedValueOnce(result(0, JSON.stringify(packageInfo.integrity)));
        await expect(publishTarball({ ...baseOptions, azure: true, runner })).resolves.toEqual({
            recoveredConflict: true,
        });
    });

    it("fails when published integrity differs", async () => {
        const runner = vi
            .fn()
            .mockResolvedValueOnce(result(1, "", "npm error code EPUBLISHCONFLICT"))
            .mockResolvedValueOnce(result(0, JSON.stringify(integrity("different"))));
        await expect(publishTarball({ ...baseOptions, runner })).rejects.toThrow(
            "Published integrity mismatch"
        );
    });

    it("fails after bounded retries when integrity is unavailable", async () => {
        const runner = vi
            .fn()
            .mockResolvedValueOnce(result(1, "", "npm error code EPUBLISHCONFLICT"))
            .mockResolvedValue(result(1, "", "npm error code E404"));
        await expect(publishTarball({ ...baseOptions, runner })).rejects.toThrow(
            "after 2 attempts"
        );
        expect(runner).toHaveBeenCalledTimes(3);
    });

    it("fails after bounded retries when integrity is malformed", async () => {
        const runner = vi
            .fn()
            .mockResolvedValueOnce(result(1, "", "npm error code EPUBLISHCONFLICT"))
            .mockResolvedValue(result(0, JSON.stringify("sha512-c2hvcnQ=")));
        await expect(publishTarball({ ...baseOptions, runner })).rejects.toThrow(
            "after 2 attempts"
        );
        expect(runner).toHaveBeenCalledTimes(3);
    });

    it("does not recover a generic Azure 403", async () => {
        const runner = vi.fn().mockResolvedValue(result(1, "", "403 Forbidden"));
        await expect(publishTarball({ ...baseOptions, azure: true, runner })).rejects.toThrow(
            "npm publish failed"
        );
        expect(runner).toHaveBeenCalledTimes(1);
    });

    it("does not recover an Azure non-tarball conflict", async () => {
        const runner = vi
            .fn()
            .mockResolvedValue(
                result(
                    1,
                    "",
                    "npm error 403 already contains file 'package.json' in package '@github/copilot-sdk/1.2.3'"
                )
            );
        await expect(publishTarball({ ...baseOptions, azure: true, runner })).rejects.toThrow(
            "npm publish failed"
        );
        expect(runner).toHaveBeenCalledTimes(1);
    });

    it("does not recover an unrelated public error embedding the known phrase", async () => {
        const runner = vi
            .fn()
            .mockResolvedValue(
                result(
                    1,
                    "",
                    "npm error network timeout while parsing 'cannot publish over the previously published versions'"
                )
            );
        await expect(publishTarball({ ...baseOptions, runner })).rejects.toThrow(
            "npm publish failed"
        );
        expect(runner).toHaveBeenCalledTimes(1);
    });

    it("does not recover an unrelated Azure error embedding the known phrase", async () => {
        const runner = vi
            .fn()
            .mockResolvedValue(
                result(
                    1,
                    "",
                    "npm error network timeout while parsing \"already contains file 'package.tgz' in package '@github/copilot-sdk/1.2.3'\""
                )
            );
        await expect(publishTarball({ ...baseOptions, azure: true, runner })).rejects.toThrow(
            "npm publish failed"
        );
        expect(runner).toHaveBeenCalledTimes(1);
    });
});
