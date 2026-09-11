import { describe, expect, it } from "vitest";
import {
    type ReleaseDispatchInputs,
    validateReleaseDispatch,
    validateRuntimeVersionChannel,
} from "../scripts/runtime-release-identity.js";

const runtime = {
    run_id: "34640000001",
    sha: "abcdef0123456789abcdef0123456789abcdef01",
    version: "1.0.83-5.unstable.123.gabcdef0",
};
const inputs: ReleaseDispatchInputs = {
    distTag: "unstable",
    mode: "publish",
    runtimeJson: "",
    version: "",
};

describe("runtime version identity", () => {
    it.each([
        ["canary", "1.2.4-canary.7.gdef5678.signed"],
        ["canary", "1.2.4-canary.8.gdef5678.unsigned"],
        ["canary", "9.9.9-canary.test"],
        ["unstable", "1.0.83-5.unstable.123.gabcdef0"],
        ["unstable", "9.9.9-unstable.test"],
        ["unstable", "1.0.83-5.unstable.123.gabcdef0+build.42"],
    ] as const)("accepts a %s runtime version: %s", (channel, runtimeVersion) => {
        expect(() => validateRuntimeVersionChannel(runtimeVersion, channel)).not.toThrow();
    });

    it.each([
        ["unstable", "1.2.4-canary.7.gdef5678.signed"],
        ["canary", "1.0.83-5.unstable.123.gabcdef0"],
        ["canary", "1.2.4-canaryish.7.gdef5678"],
    ] as const)("rejects a runtime version outside %s: %s", (channel, runtimeVersion) => {
        expect(() => validateRuntimeVersionChannel(runtimeVersion, channel)).toThrow(
            `does not belong to the '${channel}' channel`
        );
    });

    it("rejects non-canonical runtime versions", () => {
        expect(() => validateRuntimeVersionChannel(" 1.2.3-unstable.4", "unstable")).toThrow();
        expect(() => validateRuntimeVersionChannel("1.2.3", "unstable")).toThrow();
    });
});

describe("release dispatch", () => {
    it.each(["latest", "prerelease", "unstable"] as const)(
        "accepts direct %s publication",
        (distTag) => {
            expect(validateReleaseDispatch({ ...inputs, distTag })).toEqual({
                kind: "direct",
                runtimeRunId: "",
                runtimeSha: "",
                runtimeVersion: "",
            });
        }
    );

    it("accepts direct unstable dry-run", () => {
        expect(
            validateReleaseDispatch({ ...inputs, distTag: "unstable", mode: "dry-run" }).kind
        ).toBe("direct");
    });

    it.each([
        ["canary", "dry-run", "1.0.83-5.canary.123.gabcdef0"],
        ["canary", "publish", "1.0.83-5.canary.123.gabcdef0"],
        ["unstable", "dry-run", runtime.version],
        ["unstable", "publish", runtime.version],
    ] as const)("accepts runtime-backed %s %s", (distTag, mode, version) => {
        expect(
            validateReleaseDispatch({
                ...inputs,
                distTag,
                mode,
                runtimeJson: JSON.stringify({ ...runtime, version }),
            })
        ).toEqual({
            kind: "runtime",
            runtimeRunId: runtime.run_id,
            runtimeSha: runtime.sha,
            runtimeVersion: version,
        });
    });

    it("rejects canary without runtime JSON", () => {
        expect(() => validateReleaseDispatch({ ...inputs, distTag: "canary" })).toThrow(
            "Canary releases require runtime JSON"
        );
    });

    it.each(["latest", "prerelease"] as const)("rejects %s dry-run", (distTag) => {
        expect(() => validateReleaseDispatch({ ...inputs, distTag, mode: "dry-run" })).toThrow(
            "Dry-run mode is supported only for canary and unstable releases"
        );
    });

    it.each(["latest", "prerelease"] as const)("rejects runtime JSON for %s", (distTag) => {
        expect(() =>
            validateReleaseDispatch({
                ...inputs,
                distTag,
                runtimeJson: JSON.stringify(runtime),
            })
        ).toThrow("Runtime JSON is supported only for canary and unstable releases");
    });

    it("rejects a direct version with runtime JSON", () => {
        expect(() =>
            validateReleaseDispatch({
                ...inputs,
                runtimeJson: JSON.stringify(runtime),
                version: "2.0.0-unstable.manual",
            })
        ).toThrow("direct version input");
    });

    it("rejects unknown dist-tags and modes", () => {
        expect(() =>
            validateReleaseDispatch({ ...inputs, distTag: "preview" as "unstable" })
        ).toThrow("Invalid release dist-tag");
        expect(() => validateReleaseDispatch({ ...inputs, mode: "test" as "publish" })).toThrow(
            "Invalid release mode"
        );
    });

    it.each([
        ["malformed JSON", "{"],
        ["whitespace-only input", " "],
        ["surrounding whitespace", ` ${JSON.stringify(runtime)}`],
        ["null", "null"],
        ["array", "[]"],
        ["missing key", JSON.stringify({ version: runtime.version, sha: runtime.sha })],
        ["extra key", JSON.stringify({ ...runtime, source: "github-packages" })],
        ["non-string field", JSON.stringify({ ...runtime, run_id: 123 })],
        ["zero run ID", JSON.stringify({ ...runtime, run_id: "0" })],
        ["non-canonical run ID", JSON.stringify({ ...runtime, run_id: "0123" })],
        ["uppercase SHA", JSON.stringify({ ...runtime, sha: runtime.sha.toUpperCase() })],
        ["wrong channel", JSON.stringify({ ...runtime, version: "1.0.83-5.canary.1" })],
    ])("rejects %s", (_name, runtimeJson) => {
        expect(() => validateReleaseDispatch({ ...inputs, runtimeJson })).toThrow();
    });
});
