import assert from "node:assert/strict";
import { appendFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import * as semver from "semver";

export type ReleaseDistTag = "canary" | "latest" | "prerelease" | "unstable";
export type ReleaseMode = "dry-run" | "publish";
export type RuntimeReleaseChannel = "canary" | "unstable";

export interface ReleaseDispatchInputs {
    distTag: ReleaseDistTag;
    mode: ReleaseMode;
    runtimeJson: string;
    version: string;
}

export interface ReleaseDispatchPlan {
    kind: "direct" | "runtime";
    runtimeRunId: string;
    runtimeSha: string;
    runtimeVersion: string;
}

interface RuntimeDescriptor {
    run_id: string;
    sha: string;
    version: string;
}

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

function parseRuntimeDescriptor(value: string): RuntimeDescriptor {
    let parsed: unknown;
    try {
        parsed = JSON.parse(value);
    } catch {
        throw new Error("Runtime input must be valid JSON.");
    }
    assert(
        typeof parsed === "object" && parsed !== null && !Array.isArray(parsed),
        "Runtime input must be a JSON object"
    );
    assert.deepEqual(
        Object.keys(parsed).sort(),
        ["run_id", "sha", "version"],
        "Runtime input must contain exactly version, sha, and run_id"
    );
    const runtime = parsed as Partial<RuntimeDescriptor>;
    for (const name of ["version", "sha", "run_id"] as const) {
        assert.equal(typeof runtime[name], "string", `Runtime ${name} must be a string`);
    }
    return runtime as RuntimeDescriptor;
}

export function validateReleaseDispatch(inputs: ReleaseDispatchInputs): ReleaseDispatchPlan {
    assert(
        inputs.distTag === "latest" ||
            inputs.distTag === "prerelease" ||
            inputs.distTag === "unstable" ||
            inputs.distTag === "canary",
        "Invalid release dist-tag"
    );
    assert(inputs.mode === "publish" || inputs.mode === "dry-run", "Invalid release mode");

    if (inputs.runtimeJson === "") {
        assert(inputs.distTag !== "canary", "Canary releases require runtime JSON");
        assert(
            inputs.mode !== "dry-run" || inputs.distTag === "unstable",
            "Dry-run mode is supported only for canary and unstable releases"
        );
        return {
            kind: "direct",
            runtimeRunId: "",
            runtimeSha: "",
            runtimeVersion: "",
        };
    }

    assert.equal(
        inputs.runtimeJson,
        inputs.runtimeJson.trim(),
        "Runtime input must not contain surrounding whitespace"
    );
    assert(
        inputs.distTag === "canary" || inputs.distTag === "unstable",
        "Runtime JSON is supported only for canary and unstable releases"
    );
    assert.equal(
        inputs.version,
        "",
        "The direct version input cannot be combined with runtime JSON"
    );
    const runtime = parseRuntimeDescriptor(inputs.runtimeJson);
    assert.match(
        runtime.run_id,
        /^[1-9][0-9]*$/,
        "Runtime run_id must be a positive canonical integer"
    );
    assert.match(runtime.sha, /^[0-9a-f]{40}$/, "Runtime sha must be a lowercase full SHA");
    validateRuntimeVersionChannel(runtime.version, inputs.distTag);
    return {
        kind: "runtime",
        runtimeRunId: runtime.run_id,
        runtimeSha: runtime.sha,
        runtimeVersion: runtime.version,
    };
}

function requiredEnvironment(name: string): string {
    const value = process.env[name];
    if (value === undefined || value === "") {
        throw new Error(`${name} is required.`);
    }
    return value;
}

function main(): void {
    const plan = validateReleaseDispatch({
        distTag: requiredEnvironment("DIST_TAG") as ReleaseDistTag,
        mode: requiredEnvironment("MODE") as ReleaseMode,
        runtimeJson: process.env.RUNTIME_JSON ?? "",
        version: process.env.VERSION_OVERRIDE ?? "",
    });
    appendFileSync(
        requiredEnvironment("GITHUB_OUTPUT"),
        [
            `kind=${plan.kind}`,
            `runtime_run_id=${plan.runtimeRunId}`,
            `runtime_sha=${plan.runtimeSha}`,
            `runtime_version=${plan.runtimeVersion}`,
            "",
        ].join("\n")
    );
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
    try {
        main();
    } catch (error) {
        console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    }
}
