/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import {
    cpSync,
    mkdirSync,
    readFileSync,
    readdirSync,
    rmSync,
    statSync,
    writeFileSync,
} from "node:fs";
import { basename, join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import * as semver from "semver";
import { x as extractTar } from "tar";
import { calculateVersion } from "./calculate-version.js";
import { assertVersionAbsent, getRegistryVersion } from "./npm-release.js";
import { calculateUnstableVersion } from "./unstable-version.js";
import { RUNTIME_PLATFORMS, validateFile } from "../src/runtimeArtifacts.js";
import { checkGenerated } from "../../scripts/ci/check-generated.mjs";

export interface ReleaseIdentity {
    sourceSha: string;
    runtimeVersion: string;
    sdkVersion: string;
    channel: "latest" | "prerelease" | "unstable";
    visibility: "public" | "internal";
}

const sdkNames = {
    nodejs: "Node.js",
    python: "Python",
    go: "Go",
    dotnet: ".NET",
    rust: "Rust",
    java: "Java",
};

export type SdkLanguage = keyof typeof sdkNames;
export type PackagedSdkLanguage = Exclude<SdkLanguage, "go">;

// https://github.com/github/copilot-sdk/releases/tag/v1.0.15-preview.1
// Its exported SDK tree matches CLI 1.0.89-0, apart from the runtime version pins.
const SDK_BOOTSTRAP_BASELINE = {
    version: "1.0.15-preview.1",
    sourceSha: "898cd62f80fc25d28b190d0682111a5418eacbd7",
    publishedAt: "2026-09-22T22:26:49Z",
};

export function selectedSdks(channel: string, visibility: string): SdkLanguage[] {
    assert(["latest", "prerelease", "unstable"].includes(channel), "Invalid SDK channel");
    assert(["public", "internal"].includes(visibility), "Invalid SDK visibility");
    assert(visibility === "public" || channel === "unstable", "Internal releases must be unstable");
    if (visibility === "public") return ["nodejs", "python", "go", "dotnet", "rust", "java"];
    return ["nodejs"];
}

export function packagedSdks(channel: string, visibility: string): PackagedSdkLanguage[] {
    return selectedSdks(channel, visibility).filter((language) => language !== "go");
}

export function packageMatrix(
    channel: string,
    visibility: string
): {
    include: { language: PackagedSdkLanguage; name: string }[];
} {
    return {
        include: packagedSdks(channel, visibility).map((language) => ({
            language,
            name: sdkNames[language],
        })),
    };
}

export function publicationSdks(
    channel: string,
    visibility: string,
    destination: string
): PackagedSdkLanguage[] {
    assert(
        destination === "public" || destination === "internal",
        "Invalid publication destination"
    );
    assert(
        destination !== "public" || visibility === "public",
        "Internal releases cannot publish publicly"
    );
    // Public Python publishes in the caller so PyPI authentication and attestations share one workflow identity.
    return packagedSdks(channel, visibility).filter((language) =>
        destination === "public"
            ? language !== "python"
            : language === "nodejs" ||
              (channel !== "unstable" && (language === "dotnet" || language === "python"))
    );
}

export function releaseLanguages(
    identity: ReleaseIdentity,
    language?: string
): PackagedSdkLanguage[] {
    const selected = packagedSdks(identity.channel, identity.visibility);
    if (language === undefined) return selected;
    const matched = selected.find((candidate) => candidate === language);
    assert(
        matched,
        `SDK ${language} is not selected for ${identity.channel}/${identity.visibility}`
    );
    return [matched];
}

export function planSdkVersion({
    channel,
    visibility,
    sourceSha,
    runId,
    override = "",
    versions,
}: Omit<ReleaseIdentity, "runtimeVersion" | "sdkVersion"> & {
    runId: string;
    override?: string;
    versions: { latest: string; prerelease?: string };
}): string {
    selectedSdks(channel, visibility);
    assert.notEqual(channel, "unstable", "Unstable SDK versions require source-history planning");
    assert.match(sourceSha, /^[0-9a-f]{40}$/, "Expected a full product source SHA");
    assert.match(runId, /^[1-9][0-9]*$/, "Expected a workflow run ID");
    const published = Object.values(versions).filter(
        (version): version is string => version !== undefined
    );
    for (const version of published) {
        assert.equal(semver.valid(version), version, `Invalid registry version: ${version}`);
    }
    const version =
        override ||
        calculateVersion(channel, {
            latest: versions.latest,
            prerelease: versions.prerelease,
        });
    assert.equal(semver.valid(version), version, "SDK version must be canonical SemVer");
    assert.equal(semver.parse(version)!.build.length, 0, "SDK version cannot have build metadata");
    assert.equal(
        semver.prerelease(version) === null,
        channel === "latest",
        "SDK version does not match channel"
    );
    return version;
}

export function planGitUnstableSdkVersion(
    repository: string,
    sourceSha: string,
    createdAt: string,
    runId: string,
    bootstrap = SDK_BOOTSTRAP_BASELINE
): string {
    assert.match(sourceSha, /^[0-9a-f]{40}$/, "Expected a full product source SHA");
    const git = (args: string[]) =>
        execFileSync("git", args, {
            cwd: repository,
            encoding: "utf8",
            stdio: ["ignore", "pipe", "pipe"],
        }).trim();
    const commits = git(["rev-list", "--first-parent", sourceSha]).split(/\r?\n/);
    const positions = new Map(commits.map((sha, index) => [sha, index]));
    const tags = git([
        "for-each-ref",
        "--format=%(refname:short)%09%(objecttype)%09%(objectname)%09%(*objectname)%09%(*objecttype)%09%(taggerdate:iso-strict)",
        "refs/tags/sdk-*",
    ])
        .split(/\r?\n/)
        .filter(Boolean)
        .flatMap((line) => {
            const [tag, type, object, target, targetType, date] = line.split("\t");
            const version = tag.slice("sdk-".length);
            if (semver.valid(version) !== version) return [];
            if (!positions.has(type === "tag" ? target : object)) return [];
            assert(
                type === "tag" && targetType === "commit",
                `SDK release tag ${tag} must be an annotated tag pointing directly to a commit`
            );
            assert(
                Number.isFinite(Date.parse(date)),
                `SDK release tag ${tag} has no valid tagger timestamp`
            );
            return [{ version, target, date }];
        });
    if (
        !tags.some((tag) => Date.parse(tag.date) <= Date.parse(createdAt)) &&
        positions.has(bootstrap.sourceSha) &&
        Date.parse(bootstrap.publishedAt) <= Date.parse(createdAt)
    ) {
        tags.push({
            version: bootstrap.version,
            target: bootstrap.sourceSha,
            date: bootstrap.publishedAt,
        });
    }
    tags.sort(
        (left, right) =>
            positions.get(left.target)! - positions.get(right.target)! ||
            semver.rcompare(left.version, right.version)
    );
    return calculateUnstableVersion({
        createdAt,
        sdkSha: sourceSha,
        runId,
        firstParentTags: tags.map((tag) => `v${tag.version}`),
        releases: tags.map((tag) => ({ tag_name: `v${tag.version}`, published_at: tag.date })),
    });
}

export function checkReleaseSources(
    product: string,
    channel: string,
    visibility: string,
    run: (args: string[]) => void = (args) => {
        execFileSync(process.execPath, args, { cwd: product, stdio: "inherit", windowsHide: true });
    }
): void {
    const languages = selectedSdks(channel, visibility);
    checkGenerated({
        root: product,
        baseline: undefined,
        generateSchemas: () =>
            run([
                "src/sdk/scripts/run-tasks.mjs",
                "generate:schemas",
                "--runtime-source",
                "checkout",
            ]),
        generateSdk: () => {
            if (visibility === "public") {
                run(["src/sdk/scripts/run-tasks.mjs", "generate", "--runtime-source", "checkout"]);
                return;
            }
            for (const language of languages) {
                run([
                    "src/sdk/scripts/run-tasks.mjs",
                    "generate",
                    language,
                    "--runtime-source",
                    "checkout",
                ]);
            }
        },
        // All six protocol constants are inexpensive Node-generated files, requiring no language toolchains.
        generateProtocol: () => run(["src/sdk/nodejs/scripts/update-protocol-version.ts"]),
    });
}

export function validateIdentity(identity: ReleaseIdentity): void {
    selectedSdks(identity.channel, identity.visibility);
    assert.match(identity.sourceSha, /^[0-9a-f]{40}$/, "Invalid product SHA");
    for (const version of [identity.runtimeVersion, identity.sdkVersion]) {
        assert.equal(semver.valid(version), version, "Invalid release version");
    }
    if (identity.channel === "unstable") {
        assert(
            semver.prerelease(identity.sdkVersion)?.[0] === "unstable",
            "SDK version does not use the unstable channel"
        );
    } else {
        assert.equal(
            semver.prerelease(identity.sdkVersion) === null,
            identity.channel === "latest",
            "SDK version does not match channel"
        );
    }
}

export function checksums(text: string): Map<string, string> {
    const result = new Map<string, string>();
    for (const line of text.trim().split(/\r?\n/)) {
        const match = /^([0-9a-f]{64})\s+\*?([^/\\\s]+)$/i.exec(line);
        assert(match, `Invalid checksum line: ${line}`);
        assert(!result.has(match[2]), `Duplicate checksum for ${match[2]}`);
        result.set(match[2], match[1].toLowerCase());
    }
    return result;
}

export function sha256(file: string): string {
    return createHash("sha256").update(readFileSync(file)).digest("hex");
}

export function verifyAsset(file: string, hashes: Map<string, string>): void {
    const expected = hashes.get(basename(file));
    assert(expected, `Missing checksum: ${basename(file)}`);
    assert.equal(sha256(file), expected, `Checksum mismatch: ${basename(file)}`);
}

export function rustReleaseLocation(identity: ReleaseIdentity): {
    url: string;
    cachePrefix: string;
} {
    validateIdentity(identity);
    assert.equal(identity.visibility, "public", "Private Rust acquisition is not supported");
    return identity.channel === "unstable"
        ? {
              url: `https://github.com/github/copilot-sdk/releases/download/runtime-${identity.runtimeVersion}`,
              cachePrefix: `copilot-sdk-runtime-${identity.runtimeVersion}`,
          }
        : {
              url: `https://github.com/github/copilot-cli/releases/download/v${identity.runtimeVersion}`,
              cachePrefix: `v${identity.runtimeVersion}`,
          };
}

export function seedRustRuntime(bundle: string, cache: string, identity: ReleaseIdentity): void {
    const { cachePrefix } = rustReleaseLocation(identity);
    const release = join(bundle, "github-release");
    const hashes = checksums(readFileSync(join(release, "dist-bin", "SHA256SUMS.txt"), "utf8"));
    mkdirSync(cache, { recursive: true });
    for (const [directory, asset] of [
        ["dist-bin", "copilot-linux-x64.tar.gz"],
        ["dist-pkg-tarballs", `github-copilot-${identity.runtimeVersion}-linux-x64.tgz`],
    ]) {
        const file = join(release, directory, asset);
        verifyAsset(file, hashes);
        cpSync(file, join(cache, `${cachePrefix}-${asset}`));
    }
}

/** Only accepts the signed, same-run release bundle; never falls back to a published release. */
export async function stageRuntime(
    bundle: string,
    output: string,
    identity: ReleaseIdentity
): Promise<void> {
    validateIdentity(identity);
    const release = join(bundle, "github-release");
    const recorded = JSON.parse(readFileSync(join(release, "release-identity.json"), "utf8"));
    assert.equal(recorded.signed, true, "SDK packaging requires signed runtime artifacts");
    assert.equal(recorded.sourceSha, identity.sourceSha, "Runtime bundle has a different source");
    assert.equal(recorded.channel, identity.channel, "Runtime bundle has a different channel");
    assert.equal(
        recorded.visibility,
        identity.visibility,
        "Runtime bundle has a different visibility"
    );
    assert.equal(
        recorded.runtimeVersion,
        identity.runtimeVersion,
        "Runtime bundle has a different version"
    );
    const hashes = checksums(readFileSync(join(release, "dist-bin", "SHA256SUMS.txt"), "utf8"));
    for (const platform of RUNTIME_PLATFORMS) {
        const archive = join(
            release,
            "dist-pkg-tarballs",
            `github-copilot-${identity.runtimeVersion}-${platform}.tgz`
        );
        verifyAsset(archive, hashes);
        const destination = join(output, platform);
        rmSync(destination, { force: true, recursive: true });
        mkdirSync(destination, { recursive: true });
        await extractTar({
            file: archive,
            cwd: destination,
            strip: 1,
            strict: true,
            preservePaths: false,
        });
        const manifest = JSON.parse(readFileSync(join(destination, "package.json"), "utf8"));
        assert.equal(
            manifest.version,
            identity.runtimeVersion,
            `Wrong runtime version for ${platform}`
        );
        if (manifest.copilotRuntime) {
            assert.equal(
                manifest.copilotRuntime.sourceSha,
                identity.sourceSha,
                `Wrong runtime SHA for ${platform}`
            );
        }
        for (const file of [
            "LICENSE.md",
            join(
                "prebuilds",
                platform,
                platform.startsWith("win32") ? "copilot-runtime.exe" : "copilot-runtime"
            ),
            join("prebuilds", platform, "runtime.node"),
            join("copilot-sdk", "extension.js"),
            join("preloads", "extension_bootstrap.mjs"),
        ])
            validateFile(join(destination, file), `${platform} ${file}`);
        const executable = `copilot-${platform}.${platform.startsWith("win32") ? "zip" : "tar.gz"}`;
        verifyAsset(join(release, "dist-bin", executable), hashes);
    }
}

export function filesUnder(root: string): string[] {
    return readdirSync(root, { withFileTypes: true }).flatMap((entry) => {
        const file = join(root, entry.name);
        assert(!entry.isSymbolicLink(), `Release artifacts cannot contain symlinks: ${file}`);
        return entry.isDirectory() ? filesUnder(file) : [file];
    });
}

export function sealPackages(
    directory: string,
    identity: ReleaseIdentity,
    language?: string
): void {
    validateIdentity(identity);
    const languages = releaseLanguages(identity, language);
    const packages = filesUnder(directory)
        .filter((file) => file !== join(directory, "release-manifest.json"))
        .map((file) => ({
            filename: file.slice(directory.length + 1).replaceAll("\\", "/"),
            sha256: sha256(file),
            size: statSync(file).size,
        }));
    for (const file of packages) {
        assert(
            languages.some((sdk) => file.filename.startsWith(`${sdk}/`)),
            `Unexpected SDK artifact: ${file.filename}`
        );
    }
    for (const sdk of languages) {
        assert(
            packages.some((file) => file.filename.startsWith(`${sdk}/`)),
            `Missing ${sdk} release artifacts`
        );
    }
    writeFileSync(
        join(directory, "release-manifest.json"),
        `${JSON.stringify({ schemaVersion: 1, ...identity, language, packages }, null, 2)}\n`
    );
}

export function verifyPackages(
    directory: string,
    identity: ReleaseIdentity,
    language?: string
): void {
    validateIdentity(identity);
    const languages = releaseLanguages(identity, language);
    const manifest = JSON.parse(readFileSync(join(directory, "release-manifest.json"), "utf8"));
    for (const [key, value] of Object.entries(identity))
        assert.equal(manifest[key], value, `Retained ${key} mismatch`);
    assert.equal(manifest.schemaVersion, 1);
    assert.equal(manifest.language, language, "Retained SDK selection mismatch");
    assert(
        Array.isArray(manifest.packages) && manifest.packages.length > 0,
        "Empty retained package set"
    );
    for (const file of manifest.packages) {
        assert(
            typeof file.filename === "string" &&
                !file.filename.includes("\\") &&
                file.filename
                    .split("/")
                    .every((part: string) => part && part !== "." && part !== "..") &&
                !file.filename.includes(":"),
            "Unsafe retained artifact filename"
        );
        assert(
            languages.some((sdk) => file.filename.startsWith(`${sdk}/`)),
            `Unexpected SDK artifact: ${file.filename}`
        );
        const path = join(directory, file.filename);
        assert.equal(statSync(path).size, file.size, `Retained size mismatch: ${file.filename}`);
        assert.equal(sha256(path), file.sha256, `Retained hash mismatch: ${file.filename}`);
    }
    for (const sdk of languages) {
        assert(
            manifest.packages.some((file: { filename: string }) =>
                file.filename.startsWith(`${sdk}/`)
            ),
            `Missing ${sdk} release artifacts`
        );
    }
    assert.deepEqual(
        filesUnder(directory)
            .filter((file) => file !== join(directory, "release-manifest.json"))
            .map((file) => file.slice(directory.length + 1).replaceAll("\\", "/"))
            .sort(),
        manifest.packages.map((file: { filename: string }) => file.filename).sort(),
        "Unexpected retained files"
    );
}

/** Join independently validated language artifacts without changing publisher inputs. */
export function assemblePackages(parts: string, output: string, identity: ReleaseIdentity): void {
    const languages = packagedSdks(identity.channel, identity.visibility);
    assert.deepEqual(
        readdirSync(parts).sort(),
        languages.map((language) => `sdk-package-${language}`).sort(),
        "Incomplete or unexpected SDK packaging artifacts"
    );
    // Validate every part before copying any package into the assembled release.
    for (const language of languages) {
        verifyPackages(join(parts, `sdk-package-${language}`), identity, language);
    }
    mkdirSync(output, { recursive: true });
    assert.equal(readdirSync(output).length, 0, "Assembled SDK output must be empty");
    for (const language of languages) {
        cpSync(join(parts, `sdk-package-${language}`, language), join(output, language), {
            recursive: true,
        });
    }
    sealPackages(output, identity);
}

export function identityFromEnvironment(sdkVersion = process.env.SDK_VERSION!): ReleaseIdentity {
    const identity = {
        sourceSha: process.env.SOURCE_SHA!,
        runtimeVersion: process.env.RUNTIME_VERSION!,
        sdkVersion,
        channel: process.env.CHANNEL as ReleaseIdentity["channel"],
        visibility: process.env.VISIBILITY as ReleaseIdentity["visibility"],
    };
    validateIdentity(identity);
    return identity;
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
    const [command, ...args] = process.argv.slice(2);
    if (command === "--help") {
        console.log(`Usage: unified-release.ts check-sources <product> | languages | package-matrix | plan <json> [product] | stage-runtime <bundle> <directory> | assemble <parts> <packages> | seal <packages> | verify <packages>
Plans or validates release artifacts without publishing.
Required environment: SOURCE_SHA, RUNTIME_VERSION, SDK_VERSION, CHANNEL, VISIBILITY.
Normal planning uses public npm channel metadata; SDK_VERSION is an optional override.
Unstable planning uses annotated sdk-* Git tags or the source-anchored bootstrap baseline; it requires a selected product checkout and WORKFLOW_CREATED_AT, never npm baselines.
Stage-runtime accepts only the signed same-run cli-release-bundle, including release-identity.json and checksums.`);
    } else if (command === "check-sources") {
        checkReleaseSources(resolve(args[0]), process.env.CHANNEL!, process.env.VISIBILITY!);
    } else if (command === "languages") {
        console.log(
            JSON.stringify(
                publicationSdks(
                    process.env.CHANNEL!,
                    process.env.VISIBILITY!,
                    process.env.DESTINATION!
                )
            )
        );
    } else if (command === "package-matrix") {
        console.log(JSON.stringify(packageMatrix(process.env.CHANNEL!, process.env.VISIBILITY!)));
    } else if (command === "plan") {
        let sdkVersion: string;
        if (process.env.CHANNEL === "unstable") {
            assert(!process.env.SDK_VERSION, "Unstable SDK versions cannot be overridden");
            assert(args[1], "Unstable planning requires the selected product checkout");
            sdkVersion = planGitUnstableSdkVersion(
                resolve(args[1]),
                process.env.SOURCE_SHA!,
                process.env.WORKFLOW_CREATED_AT!,
                process.env.GITHUB_RUN_ID!
            );
        } else {
            const [latest, prerelease] = await Promise.all(
                ["latest", "prerelease"].map((tag) =>
                    getRegistryVersion("@github/copilot-sdk", tag, "https://registry.npmjs.org")
                )
            );
            assert(latest, "No latest SDK version exists");
            sdkVersion = planSdkVersion({
                channel: process.env.CHANNEL as ReleaseIdentity["channel"],
                visibility: process.env.VISIBILITY as ReleaseIdentity["visibility"],
                sourceSha: process.env.SOURCE_SHA!,
                runId: process.env.GITHUB_RUN_ID!,
                override: process.env.SDK_VERSION,
                versions: { latest, prerelease },
            });
        }
        if (process.env.VISIBILITY === "public") {
            await assertVersionAbsent(
                "@github/copilot-sdk",
                sdkVersion,
                "https://registry.npmjs.org"
            );
        }
        writeFileSync(args[0], `${JSON.stringify(identityFromEnvironment(sdkVersion), null, 2)}\n`);
    } else if (command === "stage-runtime") {
        await stageRuntime(resolve(args[0]), resolve(args[1]), identityFromEnvironment());
    } else if (command === "assemble") {
        assemblePackages(resolve(args[0]), resolve(args[1]), identityFromEnvironment());
    } else if (command === "seal") {
        sealPackages(resolve(args[0]), identityFromEnvironment());
    } else if (command === "verify") {
        verifyPackages(resolve(args[0]), identityFromEnvironment());
    } else {
        throw new Error(
            "Usage: unified-release.ts check-sources <product> | languages | package-matrix | plan <json> [product] | stage-runtime <bundle> <directory> | assemble <parts> <packages> | seal <packages> | verify <packages>"
        );
    }
}
