import { describe, expect, it } from "vitest";
import {
    type ReleaseDispatchInputs,
    validateReleaseDispatch,
    validateRuntimeReleaseIdentity,
} from "../scripts/runtime-release-identity.js";

const runtime = {
    run_id: "34640000001",
    sha: "abcdef0123456789abcdef0123456789abcdef01",
    version: "1.0.83-5.unstable.r34640000001.gabcdef0",
};
const inputs: ReleaseDispatchInputs = {
    distTag: "unstable",
    mode: "publish",
    runtimeJson: "",
    testPolicy: "required",
    version: "",
};

describe("runtime version identity", () => {
    it.each([
        ["canary", "1.2.4-canary.r34640000001.gabcdef0.signed"],
        ["canary", "1.2.4-canary.r34640000001.gabcdef0.unsigned"],
        ["canary", "1.2.4-7.canary.r34640000001.gabcdef0.signed"],
        ["canary", "1.2.4-7.canary.r34640000001.gabcdef0.unsigned"],
        ["unstable", "1.2.4-unstable.r34640000001.gabcdef0"],
        ["unstable", "1.2.4-7.unstable.r34640000001.gabcdef0"],
    ] as const)("accepts a %s runtime version: %s", (channel, runtimeVersion) => {
        expect(() =>
            validateRuntimeReleaseIdentity({
                channel,
                runId: runtime.run_id,
                sha: runtime.sha,
                version: runtimeVersion,
            })
        ).not.toThrow();
    });

    it.each([
        ["wrong channel", "unstable", "1.2.4-canary.r34640000001.gabcdef0.signed"],
        ["test suffix", "unstable", "1.2.4-unstable.r34640000001.gabcdef0.test"],
        ["canary test suffix", "canary", "1.2.4-canary.r34640000001.gabcdef0.test"],
        ["old numeric run", "unstable", "1.2.4-unstable.34640000001.gabcdef0"],
        ["missing r", "unstable", "1.2.4-unstable.34640000001.gabcdef0"],
        ["mismatched run", "unstable", "1.2.4-unstable.r34640000002.gabcdef0"],
        ["mismatched sha", "unstable", "1.2.4-unstable.r34640000001.g1234567"],
        ["build metadata", "unstable", "1.2.4-unstable.r34640000001.gabcdef0+build.42"],
        ["canary without signing", "canary", "1.2.4-canary.r34640000001.gabcdef0"],
        [
            "canary extra signing suffix",
            "canary",
            "1.2.4-canary.r34640000001.gabcdef0.signed.extra",
        ],
        ["unstable signed", "unstable", "1.2.4-unstable.r34640000001.gabcdef0.signed"],
        [
            "non-numeric baseline prerelease",
            "unstable",
            "1.2.4-preview.unstable.r34640000001.gabcdef0",
        ],
        ["not a prerelease", "unstable", "1.2.4"],
        ["surrounding whitespace", "unstable", " 1.2.4-unstable.r34640000001.gabcdef0"],
    ] as const)("rejects %s: %s", (_name, channel, runtimeVersion) => {
        expect(() =>
            validateRuntimeReleaseIdentity({
                channel,
                runId: runtime.run_id,
                sha: runtime.sha,
                version: runtimeVersion,
            })
        ).toThrow();
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
                testPolicy: "required",
            });
        }
    );

    it("accepts direct unstable dry-run", () => {
        expect(
            validateReleaseDispatch({ ...inputs, distTag: "unstable", mode: "dry-run" }).kind
        ).toBe("direct");
    });

    it.each([
        ["canary", "dry-run", "1.0.83-5.canary.r34640000001.gabcdef0.unsigned"],
        ["canary", "publish", "1.0.83-5.canary.r34640000001.gabcdef0.signed"],
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
            testPolicy: "required",
        });
    });

    it.each(["required", "advisory", "skipped"] as const)(
        "accepts runtime-backed releases with %s tests",
        (testPolicy) => {
            expect(
                validateReleaseDispatch({
                    ...inputs,
                    runtimeJson: JSON.stringify(runtime),
                    testPolicy,
                }).testPolicy
            ).toBe(testPolicy);
        }
    );

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

    it.each(["advisory", "skipped"] as const)(
        "rejects %s tests for direct releases",
        (testPolicy) => {
            expect(() => validateReleaseDispatch({ ...inputs, testPolicy })).toThrow(
                "Direct releases require the default runtime E2E test policy"
            );
        }
    );

    it("rejects unknown dist-tags, modes, and test policies", () => {
        expect(() =>
            validateReleaseDispatch({ ...inputs, distTag: "preview" as "unstable" })
        ).toThrow("Invalid release dist-tag");
        expect(() => validateReleaseDispatch({ ...inputs, mode: "test" as "publish" })).toThrow(
            "Invalid release mode"
        );
        expect(() =>
            validateReleaseDispatch({ ...inputs, testPolicy: "optional" as "required" })
        ).toThrow("Invalid runtime E2E test policy");
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
        [
            "wrong channel",
            JSON.stringify({
                ...runtime,
                version: "1.0.83-5.canary.r34640000001.gabcdef0.signed",
            }),
        ],
        [
            "mismatched version run ID",
            JSON.stringify({
                ...runtime,
                version: "1.0.83-5.unstable.r34640000002.gabcdef0",
            }),
        ],
        [
            "mismatched version SHA",
            JSON.stringify({
                ...runtime,
                version: "1.0.83-5.unstable.r34640000001.g1234567",
            }),
        ],
        [
            "obsolete test version",
            JSON.stringify({
                ...runtime,
                version: "1.0.83-5.unstable.r34640000001.gabcdef0.test",
            }),
        ],
    ])("rejects %s", (_name, runtimeJson) => {
        expect(() => validateReleaseDispatch({ ...inputs, runtimeJson })).toThrow();
    });
});
