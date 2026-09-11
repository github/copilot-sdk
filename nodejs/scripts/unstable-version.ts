import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import * as semver from "semver";

export interface ReleaseRecord {
    draft?: boolean;
    prerelease?: boolean;
    published_at: string | null;
    tag_name: string;
}

export interface UnstableVersionOptions {
    createdAt: string;
    firstParentTags: string[];
    releases: ReleaseRecord[];
    runId: string;
    sdkSha: string;
    versionOverride?: string;
}

export interface CanaryVersionOptions {
    createdAt: string;
    releases: ReleaseRecord[];
    runNumber: string;
    sdkSha: string;
}

function canonicalVersion(tag: string): string | undefined {
    if (!tag.startsWith("v")) {
        return undefined;
    }
    const version = tag.slice(1);
    return semver.valid(version) === version ? version : undefined;
}

export function targetCoreFromBaseline(baseline: string): string {
    const parsed = semver.parse(baseline);
    if (!parsed) {
        throw new Error(`Invalid SDK release baseline: ${baseline}`);
    }
    if (parsed.prerelease.length > 0) {
        return `${parsed.major}.${parsed.minor}.${parsed.patch}`;
    }
    return `${parsed.major}.${parsed.minor}.${parsed.patch + 1}`;
}

function validateReleaseIdentity(
    createdAt: string,
    runIdentity: string,
    runIdentityLabel: "ID" | "number",
    sdkSha: string
): number {
    if (!/^[0-9]+$/.test(runIdentity)) {
        throw new Error(`Invalid workflow run ${runIdentityLabel}: ${runIdentity}`);
    }
    if (!/^[0-9a-f]{40}$/i.test(sdkSha)) {
        throw new Error(`Invalid full SDK SHA: ${sdkSha}`);
    }
    const createdAtTime = Date.parse(createdAt);
    if (!Number.isFinite(createdAtTime)) {
        throw new Error(`Invalid workflow creation time: ${createdAt}`);
    }
    return createdAtTime;
}

export function calculateCanaryVersion(options: CanaryVersionOptions): string {
    const createdAt = validateReleaseIdentity(
        options.createdAt,
        options.runNumber,
        "number",
        options.sdkSha
    );
    const baseline = options.releases
        .filter((release) => {
            if (release.draft || release.prerelease || release.published_at === null) {
                return false;
            }
            const version = canonicalVersion(release.tag_name);
            return (
                version !== undefined &&
                semver.prerelease(version) === null &&
                Date.parse(release.published_at) <= createdAt
            );
        })
        .sort((left, right) => {
            const publishedDifference =
                Date.parse(right.published_at!) - Date.parse(left.published_at!);
            if (publishedDifference !== 0) {
                return publishedDifference;
            }
            return semver.rcompare(
                canonicalVersion(left.tag_name)!,
                canonicalVersion(right.tag_name)!
            );
        })
        .map((release) => canonicalVersion(release.tag_name)!)[0];
    if (!baseline) {
        throw new Error("No stable SDK release was published before this workflow run.");
    }
    return `${targetCoreFromBaseline(baseline)}-canary.${options.runNumber}.g${options.sdkSha.slice(0, 7)}`;
}

export function calculateUnstableVersion(options: UnstableVersionOptions): string {
    const createdAt = validateReleaseIdentity(
        options.createdAt,
        options.runId,
        "ID",
        options.sdkSha
    );

    if (options.versionOverride) {
        const parsed = semver.parse(options.versionOverride);
        if (
            !parsed ||
            semver.valid(options.versionOverride) !== options.versionOverride ||
            parsed.prerelease[0] !== "unstable"
        ) {
            throw new Error(
                `Explicit unstable SDK version must be valid SemVer with an unstable prerelease: ${options.versionOverride}`
            );
        }
        return `${parsed.major}.${parsed.minor}.${parsed.patch}-${parsed.prerelease.join(".")}.${options.runId}.g${options.sdkSha.slice(0, 7)}`;
    }

    const eligibleTags = new Set(
        options.releases
            .filter(
                (release) =>
                    !release.draft &&
                    release.published_at !== null &&
                    Date.parse(release.published_at) <= createdAt &&
                    canonicalVersion(release.tag_name) !== undefined
            )
            .map((release) => release.tag_name)
    );
    const baselineTag = options.firstParentTags.find((tag) => eligibleTags.has(tag));
    const baseline = baselineTag ? canonicalVersion(baselineTag) : undefined;
    if (!baseline) {
        throw new Error(
            "No eligible SDK release tag was found on the selected SDK branch's first-parent history."
        );
    }

    return `${targetCoreFromBaseline(baseline)}-unstable.${options.runId}.g${options.sdkSha.slice(0, 7)}`;
}

function getFirstParentTags(sdkSha: string): string[] {
    const commits = execFileSync("git", ["rev-list", "--first-parent", sdkSha], {
        encoding: "utf8",
    })
        .trim()
        .split(/\r?\n/)
        .filter(Boolean);
    const position = new Map(commits.map((commit, index) => [commit, index]));
    return execFileSync("git", ["tag", "--list", "v*"], { encoding: "utf8" })
        .trim()
        .split(/\r?\n/)
        .filter((tag) => canonicalVersion(tag) !== undefined)
        .map((tag) => ({
            tag,
            commit: execFileSync("git", ["rev-parse", `${tag}^{commit}`], {
                encoding: "utf8",
            }).trim(),
        }))
        .filter(({ commit }) => position.has(commit))
        .sort(
            (left, right) =>
                (position.get(left.commit) ?? Number.MAX_SAFE_INTEGER) -
                (position.get(right.commit) ?? Number.MAX_SAFE_INTEGER)
        )
        .map(({ tag }) => tag);
}

function requireEnvironment(name: string): string {
    const value = process.env[name]?.trim();
    if (!value) {
        throw new Error(`${name} is required.`);
    }
    return value;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
    const releasesPath = requireEnvironment("SDK_RELEASES_FILE");
    const releases = JSON.parse(readFileSync(releasesPath, "utf8")) as ReleaseRecord[];
    const sdkSha = requireEnvironment("SDK_SHA");
    const createdAt = requireEnvironment("WORKFLOW_CREATED_AT");
    const version =
        requireEnvironment("SDK_CHANNEL") === "canary"
            ? calculateCanaryVersion({
                  createdAt,
                  releases,
                  runNumber: requireEnvironment("WORKFLOW_RUN_NUMBER"),
                  sdkSha,
              })
            : calculateUnstableVersion({
                  createdAt,
                  firstParentTags: getFirstParentTags(sdkSha),
                  releases,
                  runId: requireEnvironment("WORKFLOW_RUN_ID"),
                  sdkSha,
                  versionOverride: process.env.SDK_VERSION_OVERRIDE?.trim() || undefined,
              });
    process.stdout.write(`${version}\n`);
}
