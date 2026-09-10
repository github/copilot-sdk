import assert from "node:assert/strict";
import * as semver from "semver";

export type RuntimeReleaseChannel = "canary" | "unstable";

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
