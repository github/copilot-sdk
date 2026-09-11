import { describe, expect, it } from "vitest";
import {
    type RuntimeReleaseInputs,
    validateRuntimeReleaseInputs,
} from "../scripts/runtime-release-identity.js";

const inputs: RuntimeReleaseInputs = {
    channel: "unstable",
    mode: "publish",
    runtimeRunId: "100",
    runtimeSha: "a".repeat(40),
    runtimeVersion: "1.2.3-unstable.4",
    versionOverride: "",
};

describe("runtime release identity", () => {
    it.each([
        ["canary", "1.2.4-canary.7.gdef5678.signed"],
        ["canary", "1.2.4-canary.8.gdef5678.unsigned"],
        ["canary", "9.9.9-canary.test"],
        ["unstable", "1.0.83-5.unstable.123.gabcdef0"],
        ["unstable", "9.9.9-unstable.test"],
        ["unstable", "1.0.83-5.unstable.123.gabcdef0+build.42"],
    ] satisfies [RuntimeReleaseInputs["channel"], string][])(
        "accepts a %s runtime version with valid producer suffixes: %s",
        (channel, runtimeVersion) => {
            expect(() =>
                validateRuntimeReleaseInputs({
                    ...inputs,
                    channel,
                    mode: channel === "canary" ? "tests-only" : "publish",
                    runtimeVersion,
                })
            ).not.toThrow();
        }
    );

    it.each([
        ["unstable", "1.2.4-canary.7.gdef5678.signed"],
        ["canary", "1.0.83-5.unstable.123.gabcdef0"],
        ["canary", "1.2.4-canaryish.7.gdef5678"],
    ] satisfies [RuntimeReleaseInputs["channel"], string][])(
        "rejects a runtime version outside the %s channel: %s",
        (channel, runtimeVersion) => {
            expect(() =>
                validateRuntimeReleaseInputs({
                    ...inputs,
                    channel,
                    mode: channel === "canary" ? "tests-only" : "publish",
                    runtimeVersion,
                })
            ).toThrow(`does not belong to the '${channel}' channel`);
        }
    );

    it("enforces the channel and mode matrix", () => {
        expect(() =>
            validateRuntimeReleaseInputs({
                ...inputs,
                channel: "canary",
                mode: "tests-only",
                runtimeVersion: "1.2.3-canary.4",
            })
        ).not.toThrow();
        expect(() =>
            validateRuntimeReleaseInputs({
                ...inputs,
                channel: "canary",
                mode: "publish",
                runtimeVersion: "1.2.3-canary.4",
            })
        ).not.toThrow();
        expect(() => validateRuntimeReleaseInputs(inputs)).not.toThrow();
        expect(() =>
            validateRuntimeReleaseInputs({
                ...inputs,
                mode: "tests-only",
            })
        ).toThrow("Invalid channel or mode combination");
        expect(() =>
            validateRuntimeReleaseInputs({
                ...inputs,
                channel: "invalid" as RuntimeReleaseInputs["channel"],
            })
        ).toThrow("Invalid release channel");
    });

    it("rejects non-canonical provenance and identity inputs", () => {
        for (const changed of [
            { runtimeRunId: "0" },
            { runtimeRunId: "0100" },
            { runtimeVersion: " 1.2.3-unstable.4" },
            { runtimeSha: "A".repeat(40) },
            { versionOverride: " 1.2.3-unstable.4" },
        ]) {
            expect(() => validateRuntimeReleaseInputs({ ...inputs, ...changed })).toThrow();
        }
    });

    it("rejects canary SDK version overrides", () => {
        expect(() =>
            validateRuntimeReleaseInputs({
                ...inputs,
                channel: "canary",
                mode: "publish",
                runtimeVersion: "1.2.3-canary.4",
                versionOverride: "1.2.3-canary.manual",
            })
        ).toThrow("Canary runs do not accept a version override");
    });
});
