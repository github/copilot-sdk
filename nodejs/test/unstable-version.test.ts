import { describe, expect, it } from "vitest";
import {
    calculateCanaryVersion,
    calculateUnstableVersion,
    targetCoreFromBaseline,
} from "../scripts/unstable-version.js";

const sha = "abcdef0123456789abcdef0123456789abcdef01";
const otherSha = "123456789abcdef0123456789abcdef012345678";
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
                runNumber: "8123",
                sdkSha: sha,
            })
        ).toBe("1.0.13-unstable.8123.gabcdef0");
    });

    it("is stable across retries and unique across new workflow runs", () => {
        const options = {
            createdAt: "2026-09-04T00:00:00Z",
            firstParentTags: ["v1.0.11"],
            releases: [release("v1.0.11")],
            runNumber: "8123",
            sdkSha: sha,
        };
        expect(calculateUnstableVersion(options)).toBe(calculateUnstableVersion(options));
        expect(calculateUnstableVersion({ ...options, runNumber: "8124" })).not.toBe(
            calculateUnstableVersion(options)
        );
        expect(calculateUnstableVersion({ ...options, sdkSha: otherSha })).not.toBe(
            calculateUnstableVersion(options)
        );
    });

    it("appends workflow identity to explicit unstable SemVer bases", () => {
        const options = {
            createdAt: "2026-09-04T00:00:00Z",
            firstParentTags: [],
            releases: [],
            runNumber: "8123",
            sdkSha: sha,
        };
        expect(
            calculateUnstableVersion({
                ...options,
                versionOverride: "2.0.0-unstable.manual.1",
            })
        ).toBe("2.0.0-unstable.manual.1.8123.gabcdef0");
        expect(
            calculateUnstableVersion({
                ...options,
                runNumber: "8124",
                versionOverride: "2.0.0-unstable.manual.1",
            })
        ).toBe("2.0.0-unstable.manual.1.8124.gabcdef0");
        expect(
            calculateUnstableVersion({
                ...options,
                sdkSha: otherSha,
                versionOverride: "2.0.0-unstable.manual.1",
            })
        ).toBe("2.0.0-unstable.manual.1.8123.g1234567");
        expect(() =>
            calculateUnstableVersion({ ...options, versionOverride: "2.0.0-preview.1" })
        ).toThrow("unstable prerelease");
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
