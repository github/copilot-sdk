import assert from "node:assert/strict";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import * as semver from "semver";

export type RuntimeReleaseChannel = "canary" | "unstable";
export type RuntimeReleaseMode = "publish" | "tests-only";

export interface RuntimeReleaseInputs {
    channel: RuntimeReleaseChannel;
    mode: RuntimeReleaseMode;
    runtimeRunId: string;
    runtimeSha: string;
    runtimeVersion: string;
    versionOverride: string;
}

const canonicalNumericIdPattern = /^[1-9][0-9]*$/;

export function validateRuntimeVersionChannel(
    version: string,
    channel: RuntimeReleaseChannel
): void {
    assert(channel === "canary" || channel === "unstable", "Invalid channel");
    const parsed = semver.parse(version);
    assert(parsed, "Runtime version must be exact SemVer");
    const canonicalVersion = `${parsed.version}${
        parsed.build.length > 0 ? `+${parsed.build.join(".")}` : ""
    }`;
    assert.equal(version, canonicalVersion, "Runtime version must be exact SemVer");
    assert(
        parsed.prerelease.some((identifier) => identifier === channel),
        `Runtime version '${version}' does not belong to the '${channel}' channel`
    );
}

export function validateRuntimeReleaseInputs(inputs: RuntimeReleaseInputs): void {
    assert(inputs.channel === "canary" || inputs.channel === "unstable", "Invalid release channel");
    assert(
        inputs.channel === "canary"
            ? inputs.mode === "tests-only" || inputs.mode === "publish"
            : inputs.mode === "publish",
        "Invalid channel or mode combination"
    );
    assert.match(
        inputs.runtimeRunId,
        canonicalNumericIdPattern,
        "Runtime workflow run ID must be a positive canonical integer"
    );
    assert.match(inputs.runtimeSha, /^[0-9a-f]{40}$/, "Runtime SHA must be lowercase full SHA");
    validateRuntimeVersionChannel(inputs.runtimeVersion, inputs.channel);
    assert.equal(
        inputs.versionOverride,
        inputs.versionOverride.trim(),
        "SDK version override must not contain surrounding whitespace"
    );
    assert(
        inputs.channel !== "canary" || inputs.versionOverride === "",
        "Canary runs do not accept a version override"
    );
}

function requiredEnvironment(name: string): string {
    const value = process.env[name];
    if (value === undefined || value === "") {
        throw new Error(`${name} is required.`);
    }
    return value;
}

function main(): void {
    validateRuntimeReleaseInputs({
        channel: requiredEnvironment("CHANNEL") as RuntimeReleaseChannel,
        mode: requiredEnvironment("MODE") as RuntimeReleaseMode,
        runtimeRunId: requiredEnvironment("RUNTIME_RUN_ID"),
        runtimeSha: requiredEnvironment("RUNTIME_SHA"),
        runtimeVersion: requiredEnvironment("RUNTIME_VERSION"),
        versionOverride: process.env.VERSION_OVERRIDE ?? "",
    });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
    try {
        main();
    } catch (error) {
        console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    }
}
