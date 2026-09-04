import { describe, expect, it } from "vitest";
import { calculateUnstableVersion, targetCoreFromBaseline } from "../scripts/unstable-version.js";

const sha = "abcdef0123456789abcdef0123456789abcdef01";
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
    });

    it("accepts only explicit unstable SemVer overrides", () => {
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
        ).toBe("2.0.0-unstable.manual.1");
        expect(() =>
            calculateUnstableVersion({ ...options, versionOverride: "2.0.0-preview.1" })
        ).toThrow("unstable prerelease");
    });
});
