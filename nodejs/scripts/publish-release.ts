/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { cpSync, mkdirSync, readFileSync, readdirSync, rmSync } from "node:fs";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { x as extractTar } from "tar";
import * as semver from "semver";
import {
    identityFromEnvironment,
    sha256,
    verifyPackages,
    type ReleaseIdentity,
} from "./unified-release.js";
import { JAVA_CLASSIFIERS } from "./package-release.js";

type NativeCommand = (
    command: string,
    args: string[],
    cwd: string,
    env?: NodeJS.ProcessEnv
) => string;

type GithubRelease = {
    tag_name: string;
    draft: boolean;
    prerelease: boolean;
    published_at: string | null;
};

const runNativeCommand: NativeCommand = (command, args, cwd, env = process.env) =>
    execFileSync(command, args, {
        cwd,
        env,
        encoding: "utf8",
        stdio: command === "git" ? "pipe" : "inherit",
    }) ?? "";

const runGh = (args: string[]): string =>
    execFileSync("gh", args, { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] }).trim();

/** Record the independent SDK baseline in the runtime repository's source history. */
export function recordRuntimeSdkVersion(identity: ReleaseIdentity, gh = runGh): void {
    assert.equal(identity.visibility, "public");
    assert.notEqual(
        identity.channel,
        "unstable",
        "Only normal public SDK releases create baseline tags"
    );
    const repository = "github/copilot-agent-runtime";
    const tag = `sdk-${identity.sdkVersion}`;
    let reference;
    try {
        reference = JSON.parse(gh(["api", `repos/${repository}/git/ref/tags/${tag}`]));
    } catch (error) {
        if (!/\(HTTP 404\)/.test(String((error as { stderr?: string }).stderr))) throw error;
    }
    if (reference) {
        assert.equal(reference.object.type, "tag", `Existing ${tag} must be annotated`);
        const existing = JSON.parse(
            gh(["api", `repos/${repository}/git/tags/${reference.object.sha}`])
        );
        assert.equal(existing.tag, tag, "Existing SDK tag has another name");
        assert.equal(
            existing.object.type,
            "commit",
            "SDK tag must point directly to the release source"
        );
        assert.equal(
            existing.object.sha,
            identity.sourceSha,
            `Existing ${tag} points to another source`
        );
        assert(
            Number.isFinite(Date.parse(existing.tagger?.date)),
            "Existing SDK tag has no valid publication time"
        );
        return;
    }
    const annotated = JSON.parse(
        gh([
            "api",
            "--method",
            "POST",
            `repos/${repository}/git/tags`,
            "-f",
            `tag=${tag}`,
            "-f",
            `message=SDK ${identity.sdkVersion}`,
            "-f",
            `object=${identity.sourceSha}`,
            "-f",
            "type=commit",
            "-f",
            "tagger[name]=github-actions[bot]",
            "-f",
            "tagger[email]=41898282+github-actions[bot]@users.noreply.github.com",
            "-f",
            `tagger[date]=${new Date().toISOString()}`,
        ])
    );
    assert.match(annotated.sha, /^[0-9a-f]{40}$/, "GitHub did not return an annotated tag object");
    gh([
        "api",
        "--method",
        "POST",
        `repos/${repository}/git/refs`,
        "-f",
        `ref=refs/tags/${tag}`,
        "-f",
        `sha=${annotated.sha}`,
    ]);
}

async function restoreNativeSource(
    directory: string,
    identity: ReleaseIdentity,
    product: string,
    language: "rust" | "java",
    run: NativeCommand
): Promise<string> {
    assert(product, "Native publishing requires the selected product checkout");
    assert.equal(
        run("git", ["rev-parse", "HEAD"], product).trim(),
        identity.sourceSha,
        "Native publishing checkout does not match the selected product SHA"
    );
    const sdk = join(product, "src", "sdk");
    const destination = join(sdk, language);
    rmSync(destination, { recursive: true, force: true });
    mkdirSync(sdk, { recursive: true });
    await extractTar({
        file: join(directory, "source.tar.gz"),
        cwd: sdk,
        strict: true,
        preservePaths: false,
        filter: (path) => {
            assert(
                path.startsWith(`${language}/`),
                "Retained native source must stay in its product directory"
            );
            return true;
        },
    });
    return destination;
}

/** Preserve native Cargo repackaging from retained source, lockfile, and runtime pins. */
export async function publishRust(
    directory: string,
    identity: ReleaseIdentity,
    product: string,
    run: NativeCommand = runNativeCommand
): Promise<void> {
    const archives = readdirSync(directory).filter((file) => file.endsWith(".crate"));
    assert.deepEqual(
        archives,
        [`github-copilot-sdk-${identity.sdkVersion}.crate`],
        "Wrong retained Rust version"
    );
    const rust = await restoreNativeSource(directory, identity, product, "rust", run);
    assert(readFileSync(join(rust, "Cargo.lock")).length > 0, "Missing retained Cargo.lock");
    assert.equal(
        /^version = "([^"]+)"$/m.exec(readFileSync(join(rust, "Cargo.toml"), "utf8"))?.[1],
        identity.sdkVersion,
        "Retained Rust SDK version differs"
    );
    for (const file of ["cli-version.txt", "cli-version-in-process.txt"]) {
        assert.equal(
            /^version=(.+)$/m.exec(readFileSync(join(rust, file), "utf8"))?.[1],
            identity.runtimeVersion,
            `Retained runtime pin differs in ${file}`
        );
    }
    run(
        "cargo",
        [
            "publish",
            "--locked",
            "--allow-dirty",
            "--no-verify",
            "--target-dir",
            join(rust, "target"),
        ],
        rust
    );
}

/** Native Maven deploy rebuilds and signs retained source with the retained classifier inputs. */
export async function publishJava(
    directory: string,
    identity: ReleaseIdentity,
    product: string,
    run: NativeCommand = runNativeCommand
): Promise<void> {
    const java = await restoreNativeSource(directory, identity, product, "java", run);
    const nodeManifest = join(directory, "nodejs-package.json");
    assert.equal(
        JSON.parse(readFileSync(nodeManifest, "utf8")).copilotCliVersion,
        identity.runtimeVersion,
        "Retained Java runtime pin differs"
    );
    cpSync(nodeManifest, join(product, "src", "sdk", "nodejs", "package.json"));
    const runtimeArchive = join(
        directory,
        `github-copilot-${identity.runtimeVersion}-linux-x64.tgz`
    );
    const nativeArgs = Object.entries(JAVA_CLASSIFIERS).map(
        ([classifier, property]) =>
            `-Dcopilot.native.external.${property}.classifier.path=${join(
                directory,
                "copilot-sdk-java-runtime",
                `copilot-sdk-java-runtime-${identity.sdkVersion}-${classifier}.jar`
            )}`
    );
    run(
        "bash",
        [
            "mvnw",
            "-B",
            "deploy",
            "-Prelease",
            "-DskipTests",
            "-DskipITs",
            `-Drevision=${identity.sdkVersion}`,
            "-Dcopilot.native.libc=glibc",
            ...nativeArgs,
        ],
        java,
        {
            ...process.env,
            COPILOT_CLI_RELEASE_TARBALL: runtimeArchive,
            COPILOT_CLI_RELEASE_SHA256: sha256(runtimeArchive),
        }
    );
}

export function previousSdkReleaseTag(
    releases: GithubRelease[],
    identity: ReleaseIdentity
): string | undefined {
    return releases
        .filter(
            (release) =>
                !release.draft &&
                release.published_at &&
                release.tag_name !== `v${identity.sdkVersion}` &&
                release.tag_name.startsWith("v") &&
                semver.valid(release.tag_name.slice(1)) &&
                (identity.channel !== "latest" || !release.prerelease)
        )
        .sort((left, right) => Date.parse(right.published_at!) - Date.parse(left.published_at!))[0]
        ?.tag_name;
}

export async function publishSourceRelease(
    identity: ReleaseIdentity,
    snapshot: string,
    gh: (args: string[]) => string = runGh
): Promise<void> {
    assert.equal(identity.visibility, "public");
    assert.match(
        snapshot,
        /^[0-9a-f]{40}$/,
        "Expected exact exported public SDK commit, not the runtime SHA"
    );
    assert.notEqual(
        snapshot,
        identity.sourceSha,
        "SDK source tags cannot reference the private runtime SHA"
    );
    const repository = "github/copilot-sdk";
    for (const tag of [
        `v${identity.sdkVersion}`,
        ...["go", "rust", "java"].map((language) => `${language}/v${identity.sdkVersion}`),
    ]) {
        let reference;
        try {
            reference = JSON.parse(gh(["api", `repos/${repository}/git/ref/tags/${tag}`]));
        } catch (error) {
            if (!/\(HTTP 404\)/.test(String((error as { stderr?: string }).stderr))) throw error;
        }
        if (reference) {
            let object: { type: string; sha: string } = reference.object;
            for (let depth = 0; object.type === "tag" && depth < 10; depth++) {
                object = JSON.parse(
                    gh(["api", `repos/${repository}/git/tags/${object.sha}`])
                ).object;
            }
            assert.equal(object.type, "commit", `Cannot resolve source tag ${tag}`);
            assert.equal(object.sha, snapshot, `Existing tag ${tag} points to another source`);
        } else {
            gh([
                "api",
                "--method",
                "POST",
                `repos/${repository}/git/refs`,
                "-f",
                `ref=refs/tags/${tag}`,
                "-f",
                `sha=${snapshot}`,
            ]);
        }
    }
    if (identity.channel === "unstable") return;
    let release;
    try {
        release = JSON.parse(
            gh(["api", `repos/${repository}/releases/tags/v${identity.sdkVersion}`])
        );
    } catch (error) {
        if (!/\(HTTP 404\)/.test(String((error as { stderr?: string }).stderr))) throw error;
    }
    if (release) {
        assert.equal(
            release.prerelease,
            identity.channel === "prerelease",
            "Existing SDK release has another channel"
        );
        assert.equal(release.draft, false, "Existing SDK release is a draft");
        return;
    }
    let releases: GithubRelease[];
    if (identity.channel === "latest") {
        try {
            releases = [JSON.parse(gh(["api", `repos/${repository}/releases/latest`]))];
        } catch (error) {
            if (!/\(HTTP 404\)/.test(String((error as { stderr?: string }).stderr))) throw error;
            releases = [];
        }
    } else {
        const output = gh([
            "api",
            "--paginate",
            "--jq",
            ".[] | {tag_name, draft, prerelease, published_at}",
            `repos/${repository}/releases?per_page=100`,
        ]);
        releases = output ? output.split("\n").map((line) => JSON.parse(line)) : [];
    }
    const previous = previousSdkReleaseTag(releases, identity);
    gh([
        "release",
        "create",
        `v${identity.sdkVersion}`,
        "--repo",
        repository,
        "--verify-tag",
        "--title",
        `v${identity.sdkVersion}`,
        "--generate-notes",
        ...(previous ? ["--notes-start-tag", previous] : []),
        ...(identity.channel === "prerelease" ? ["--prerelease", "--latest=false"] : ["--latest"]),
    ]);
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
    const [command, directory, flag] = process.argv.slice(2);
    if (command === "--help") {
        console.log(`Usage: publish-release.ts <rust|java|source> <retained-packages> --run
       publish-release.ts record-version --run
Publishes only with --run, after validating the retained release manifest.
Required environment: SOURCE_SHA, RUNTIME_VERSION, SDK_VERSION, CHANNEL, VISIBILITY.
Rust: CARGO_REGISTRY_TOKEN and PRODUCT_CHECKOUT (checked out at SOURCE_SHA).
Java: PRODUCT_CHECKOUT, Maven Central settings, and imported GPG key with JAVA_GPG_PASSPHRASE.
Source release: GH_TOKEN and SDK_SNAPSHOT_SHA (the exported public commit).`);
    } else if (command === "record-version") {
        assert.equal(directory, "--run", "Publication requires --run");
        assert.equal(flag, undefined, "Unexpected record-version argument");
        recordRuntimeSdkVersion(identityFromEnvironment());
    } else {
        assert.equal(flag, "--run", "Publication requires --run; use --help for usage");
        const identity = identityFromEnvironment();
        assert.equal(
            identity.visibility,
            "public",
            "This helper never publishes internal releases"
        );
        verifyPackages(resolve(directory), identity);
        if (command === "rust")
            await publishRust(resolve(directory, "rust"), identity, process.env.PRODUCT_CHECKOUT!);
        else if (command === "java") {
            await publishJava(resolve(directory, "java"), identity, process.env.PRODUCT_CHECKOUT!);
        } else if (command === "source")
            await publishSourceRelease(identity, process.env.SDK_SNAPSHOT_SHA!);
        else throw new Error(`Unknown publisher: ${command}`);
    }
}
