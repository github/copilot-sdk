import { describe, expect, it } from "vitest";
import * as semver from "semver";
import {
    calculateCanaryVersion,
    calculateUnstableVersion,
    targetCoreFromBaseline,
} from "../scripts/unstable-version.js";

const sha = "abcdef0123456789abcdef0123456789abcdef01";
const otherSha = "123456789abcdef0123456789abcdef012345678";
const runId = "34640000001";
const release = (tag_name: string, published_at = "2026-09-01T00:00:00Z") => ({
    tag_name,
    published_at,
});

describe("unstable SDK version planning", () => {
    it("increments a stable baseline patch", () => {
        expect(targetCoreFromBaseline("1.0.11")).toBe("1.0.12");
    });

    it("uses a prerelease baseline's release core", () => {
        expect(targetCoreFromBaseline("1.0.13-preview.4")).toBe("1.0.13");
    });

    it("selects the nearest eligible release on first-parent history", () => {
        expect(
            calculateUnstableVersion({
                createdAt: "2026-09-04T00:00:00Z",
                firstParentTags: ["v1.0.13-preview.4", "v1.0.12", "v1.0.11"],
                releases: [
                    release("v1.0.13-preview.4"),
                    release("v1.0.12", "2026-09-05T00:00:00Z"),
                    release("v1.0.11"),
                ],
                runId,
                sdkSha: sha,
            })
        ).toBe("1.0.13-unstable.34640000001.gabcdef0");
    });

    it("freezes eligible release history at workflow creation time", () => {
        const options = {
            createdAt: "2026-09-04T00:00:00Z",
            firstParentTags: ["v1.0.12", "v1.0.11"],
            releases: [
                release("v1.0.12", "2026-09-05T00:00:00Z"),
                release("v1.0.11", "2026-09-01T00:00:00Z"),
            ],
            runId,
            sdkSha: sha,
        };
        const planned = calculateUnstableVersion(options);
        expect(planned).toBe("1.0.12-unstable.34640000001.gabcdef0");
        expect(
            calculateUnstableVersion({
                ...options,
                releases: [...options.releases, release("v1.0.13", "2026-09-06T00:00:00Z")],
            })
        ).toBe(planned);
    });

    it("is stable across retries and unique across repository-wide workflow run IDs", () => {
        const options = {
            createdAt: "2026-09-04T00:00:00Z",
            firstParentTags: ["v1.0.11"],
            releases: [release("v1.0.11")],
            runId,
            sdkSha: sha,
        };
        expect(calculateUnstableVersion(options)).toBe(calculateUnstableVersion(options));
        expect(calculateUnstableVersion({ ...options, runId: "34640000002" })).not.toBe(
            calculateUnstableVersion(options)
        );
        expect(calculateUnstableVersion({ ...options, sdkSha: otherSha })).not.toBe(
            calculateUnstableVersion(options)
        );
    });

    it("keeps explicit and generated versions in the same numeric-first ordering", () => {
        const options = {
            createdAt: "2026-09-04T00:00:00Z",
            firstParentTags: ["v1.9.9"],
            releases: [release("v1.9.9")],
            runId,
            sdkSha: sha,
        };
        const explicit = calculateUnstableVersion({
            ...options,
            versionOverride: "1.9.10-unstable",
        });
        const laterGenerated = calculateUnstableVersion({
            ...options,
            runId: "34640000002",
        });

        expect(explicit).toBe("1.9.10-unstable.34640000001.gabcdef0");
        expect(laterGenerated).toBe("1.9.10-unstable.34640000002.gabcdef0");
        expect(semver.gt(laterGenerated, explicit)).toBe(true);
        expect(
            calculateUnstableVersion({
                ...options,
                sdkSha: otherSha,
                versionOverride: "1.9.10-unstable",
            })
        ).toBe("1.9.10-unstable.34640000001.g1234567");
    });

    it.each(["2.0.0-unstable.manual.1", "2.0.0-unstable+build.1", "2.0.0-preview.1"])(
        "rejects unsupported explicit unstable base %s",
        (versionOverride) => {
            expect(() =>
                calculateUnstableVersion({
                    createdAt: "2026-09-04T00:00:00Z",
                    firstParentTags: [],
                    releases: [],
                    runId,
                    sdkSha: sha,
                    versionOverride,
                })
            ).toThrow("<core>-unstable");
        }
    );

    it("cannot collide across workflows with coincident per-workflow run numbers", () => {
        const options = {
            createdAt: "2026-09-04T00:00:00Z",
            firstParentTags: ["v1.0.11"],
            releases: [release("v1.0.11")],
            sdkSha: sha,
        };
        const direct = calculateUnstableVersion({ ...options, runId });
        const runtimeDriven = calculateUnstableVersion({ ...options, runId: "34640000002" });
        expect(direct).toBe("1.0.12-unstable.34640000001.gabcdef0");
        expect(runtimeDriven).toBe("1.0.12-unstable.34640000002.gabcdef0");
        expect(direct).not.toBe(runtimeDriven);
    });
});

describe("canary SDK version planning", () => {
    it("is stable across retries and unique across new workflow runs", () => {
        const options = {
            createdAt: "2026-09-04T00:00:00Z",
            releases: [release("v1.0.11")],
            runNumber: "8123",
            sdkSha: sha,
        };
        expect(calculateCanaryVersion(options)).toBe(calculateCanaryVersion(options));
        expect(calculateCanaryVersion({ ...options, runNumber: "8124" })).not.toBe(
            calculateCanaryVersion(options)
        );
        expect(calculateCanaryVersion({ ...options, sdkSha: otherSha })).not.toBe(
            calculateCanaryVersion(options)
        );
    });

    it("freezes the stable baseline at workflow creation time", () => {
        const options = {
            createdAt: "2026-09-04T00:00:00Z",
            releases: [
                release("v1.0.11", "2026-09-01T00:00:00Z"),
                release("v1.0.12", "2026-09-05T00:00:00Z"),
                release("v1.0.13-preview.1", "2026-09-03T00:00:00Z"),
                {
                    ...release("v2.0.0", "2026-09-02T00:00:00Z"),
                    prerelease: true,
                },
            ],
            runNumber: "8123",
            sdkSha: sha,
        };
        const planned = calculateCanaryVersion(options);
        expect(planned).toBe("1.0.12-canary.8123.gabcdef0");
        expect(
            calculateCanaryVersion({
                ...options,
                releases: [...options.releases, release("v1.0.13", "2026-09-06T00:00:00Z")],
            })
        ).toBe(planned);
    });
});
