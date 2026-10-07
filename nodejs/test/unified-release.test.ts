/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import {
    existsSync,
    mkdirSync,
    mkdtempSync,
    readFileSync,
    readdirSync,
    rmSync,
    writeFileSync,
} from "node:fs";
import { execFileSync } from "node:child_process";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { c as createTar } from "tar";
import { afterEach, describe, expect, it, vi } from "vitest";
import { getTaskSteps } from "../../scripts/run-tasks.mjs";
import { RUNTIME_PLATFORMS } from "../src/runtimeArtifacts.js";
import {
    copyPackages,
    packageRelease,
    pythonPackageVersion,
    stampRustPackageManifest,
    writeRustReleaseSnapshots,
} from "../scripts/package-release.js";
import {
    previousSdkReleaseTag,
    publishJava,
    publishRust,
    publishSourceRelease,
    recordRuntimeSdkVersion,
} from "../scripts/publish-release.js";
import {
    checksums,
    checkReleaseSources,
    assemblePackages,
    packageMatrix,
    packagedSdks,
    planSdkVersion,
    planGitUnstableSdkVersion,
    publicationSdks,
    rustReleaseLocation,
    sealPackages,
    seedRustRuntime,
    selectedSdks,
    sha256,
    stageRuntime,
    verifyPackages,
    type ReleaseIdentity,
} from "../scripts/unified-release.js";

const roots: string[] = [];
const sourceSha = "a".repeat(40);
const identity: ReleaseIdentity = {
    sourceSha,
    runtimeVersion: "1.0.89-2",
    sdkVersion: "0.4.0-preview.1",
    channel: "prerelease",
    visibility: "public",
};
const plan = {
    sourceSha,
    channel: "latest" as const,
    visibility: "public" as const,
    runId: "123",
    versions: { latest: "0.3.0", prerelease: "0.4.0-preview.0" },
};

function fixture(): string {
    const root = mkdtempSync(join(process.cwd(), ".sdk-release-test-"));
    roots.push(root);
    return root;
}

function write(path: string, text: string): void {
    mkdirSync(resolve(path, ".."), { recursive: true });
    writeFileSync(path, text);
}

afterEach(() => {
    vi.unstubAllEnvs();
    for (const root of roots.splice(0)) rmSync(root, { recursive: true, force: true });
});

describe("release helper command contracts", () => {
    it.each([
        ["unstable", "public", [["nodejs", "python", "go", "dotnet", "java", "rust"]]],
        ["unstable", "internal", [["nodejs"]]],
        ["prerelease", "public", [["nodejs", "python", "go", "dotnet", "java", "rust"]]],
        ["latest", "public", [["nodejs", "python", "go", "dotnet", "java", "rust"]]],
    ] as const)(
        "dispatches valid freshness tasks for %s/%s",
        (channel, visibility, expectedLanguages) => {
            const product = fixture();
            execFileSync("git", ["init", "--quiet"], { cwd: product, windowsHide: true });
            const generation: { task: string; languages: string[] }[] = [];
            const protocolCommands: string[][] = [];

            // Use the real task registry; only the compiler/code-generator execution is replaced.
            checkReleaseSources(product, channel, visibility, (args) => {
                if (args[0] === "src/sdk/scripts/run-tasks.mjs") {
                    expect(args.slice(-2)).toEqual(["--runtime-source", "checkout"]);
                    const language = args[2]?.startsWith("--") ? undefined : args[2];
                    const steps = getTaskSteps(args[1], language);
                    generation.push({
                        task: args[1],
                        languages: steps.map((step) => step.language),
                    });
                } else {
                    protocolCommands.push(args);
                }
            });

            expect(generation).toEqual([
                { task: "generate:schemas", languages: [] },
                ...expectedLanguages.map((languages) => ({ task: "generate", languages })),
            ]);
            expect(protocolCommands).toEqual([
                ["src/sdk/nodejs/scripts/update-protocol-version.ts"],
            ]);
        }
    );

    it.each(["unified-release.ts", "package-release.ts", "publish-release.ts"])(
        "starts the %s CLI without publication",
        (script) => {
            const directory = fixture();
            const loader = pathToFileURL(
                resolve(import.meta.dirname, "../node_modules/tsx/dist/loader.mjs")
            ).href;
            const output = execFileSync(
                process.execPath,
                ["--import", loader, resolve(import.meta.dirname, "../scripts", script), "--help"],
                { cwd: directory, encoding: "utf8", windowsHide: true }
            );
            expect(output).toMatch(/^Usage:/);
            expect(readdirSync(directory)).toEqual([]);
        }
    );
});

describe("matched SDK release planning", () => {
    it("supports direct stable and compatible promotion versions using the existing calculator", () => {
        expect(planSdkVersion({ ...plan, versions: { latest: "0.3.0" } })).toBe("0.3.1");
        expect(
            planSdkVersion({ ...plan, versions: { latest: "0.3.0", prerelease: undefined } })
        ).toBe("0.3.1");
        expect(planSdkVersion(plan)).toBe("0.4.0");
        expect(planSdkVersion({ ...plan, channel: "prerelease" })).toBe("0.4.0-preview.1");
        expect(planSdkVersion({ ...plan, override: "2.0.0" })).toBe("2.0.0");
        expect(planSdkVersion({ ...plan, override: "0.3.1" })).toBe("0.3.1");
        expect(planSdkVersion({ ...plan, channel: "prerelease", override: "0.5.0-pre.2" })).toBe(
            "0.5.0-pre.2"
        );
    });

    it("never derives an unstable version from npm baselines", () => {
        expect(() => planSdkVersion({ ...plan, channel: "unstable" })).toThrow(
            "require source-history planning"
        );
    });

    describe("runtime-repository SDK version tags", () => {
        function history() {
            const root = fixture();
            const git = (args: string[], date = "2026-01-01T12:00:00Z") =>
                execFileSync("git", args, {
                    cwd: root,
                    encoding: "utf8",
                    env: {
                        ...process.env,
                        GIT_AUTHOR_DATE: date,
                        GIT_COMMITTER_DATE: date,
                    },
                }).trim();
            git(["init", "--quiet"]);
            git(["config", "user.name", "release-test"]);
            git(["config", "user.email", "release-test@example.invalid"]);
            git(["config", "commit.gpgsign", "false"]);
            git(["config", "tag.gpgsign", "false"]);
            git(["commit", "--quiet", "--allow-empty", "-m", "SDK source"]);
            return { root, git, sha: git(["rev-parse", "HEAD"]) };
        }

        it("uses nearest eligible first-parent tags, highest version ties, and tag publication time", () => {
            const { root, git, sha: base } = history();
            git(["tag", "-a", "sdk-0.3.0", "-m", "Released"]);
            git(["commit", "--quiet", "--allow-empty", "-m", "Next source"]);
            git(["tag", "-a", "sdk-0.4.0-preview.2", "-m", "Preview"], "2026-01-02T00:00:00Z");
            git(
                ["tag", "-a", "sdk-0.5.0-preview.1", "-m", "Higher preview"],
                "2026-01-03T00:00:00Z"
            );
            git(["commit", "--quiet", "--allow-empty", "-m", "Selected source"]);
            const selected = git(["rev-parse", "HEAD"]);
            git(["tag", "-a", "sdk-0.6.0", "-m", "Later publication"], "2026-02-01T00:00:00Z");
            git(["checkout", "--quiet", "--detach", base]);
            git(["commit", "--quiet", "--allow-empty", "-m", "Other history"]);
            git(["tag", "-a", "sdk-9.0.0", "-m", "Not an ancestor"]);
            const version = planGitUnstableSdkVersion(
                root,
                selected,
                "2026-01-05T00:00:00Z",
                "123"
            );
            expect(version).toBe(`0.5.0-unstable.123.g${selected.slice(0, 7)}`);
            expect(planGitUnstableSdkVersion(root, selected, "2026-02-02T00:00:00Z", "124")).toBe(
                `0.6.1-unstable.124.g${selected.slice(0, 7)}`
            );
            expect(planGitUnstableSdkVersion(root, base, "2026-01-05T00:00:00Z", "125")).toBe(
                `0.3.1-unstable.125.g${base.slice(0, 7)}`
            );
        });

        it("uses a source-anchored bootstrap until an eligible SDK tag exists", () => {
            const { root, git, sha } = history();
            const bootstrap = {
                version: "0.3.0",
                sourceSha: sha,
                publishedAt: "2026-01-02T00:00:00Z",
            };
            git(["commit", "--quiet", "--allow-empty", "-m", "Selected source"]);
            const selected = git(["rev-parse", "HEAD"]);
            const version = () =>
                planGitUnstableSdkVersion(root, selected, "2026-01-05T00:00:00Z", "123", bootstrap);
            expect(version()).toBe(`0.3.1-unstable.123.g${selected.slice(0, 7)}`);
            git(["tag", "-a", "sdk-9.0.0", "-m", "Future"], "2026-02-01T00:00:00Z");
            expect(version()).toBe(`0.3.1-unstable.123.g${selected.slice(0, 7)}`);
            git(["tag", "-a", "sdk-0.4.0", "-m", "Released"], "2026-01-03T00:00:00Z");
            expect(version()).toBe(`0.4.1-unstable.123.g${selected.slice(0, 7)}`);
        });

        it("does not apply the bootstrap before publication or outside its source history", () => {
            const { root, git, sha } = history();
            const bootstrap = {
                version: "0.3.0",
                sourceSha: sha,
                publishedAt: "2026-01-02T00:00:00Z",
            };
            expect(() =>
                planGitUnstableSdkVersion(root, sha, "2026-01-01T00:00:00Z", "123", bootstrap)
            ).toThrow("No eligible");
            git(["commit", "--quiet", "--allow-empty", "-m", "Later baseline"]);
            expect(() =>
                planGitUnstableSdkVersion(root, sha, "2026-01-05T00:00:00Z", "123", {
                    ...bootstrap,
                    sourceSha: git(["rev-parse", "HEAD"]),
                })
            ).toThrow("No eligible");
        });

        it("rejects absent, unpublished-at-start, and lightweight SDK baselines", () => {
            const { root, git, sha } = history();
            expect(() =>
                planGitUnstableSdkVersion(root, sha, "2026-01-05T00:00:00Z", "123")
            ).toThrow("No eligible");
            git(["tag", "-a", "sdk-0.4.0", "-m", "Future"], "2026-02-01T00:00:00Z");
            expect(() =>
                planGitUnstableSdkVersion(root, sha, "2026-01-05T00:00:00Z", "123")
            ).toThrow("No eligible");
            git(["tag", "sdk-0.3.0"]);
            expect(() =>
                planGitUnstableSdkVersion(root, sha, "2026-01-05T00:00:00Z", "123")
            ).toThrow("must be an annotated");
        });

        it("plans an internal unstable release through the actual CLI without npm metadata", () => {
            const { root, git, sha } = history();
            git(["tag", "-a", "sdk-0.3.0", "-m", "Verified baseline"]);
            const output = join(root, "plan.json");
            const loader = pathToFileURL(resolve("node_modules/tsx/dist/loader.mjs")).href;
            execFileSync(
                process.execPath,
                ["--import", loader, resolve("scripts/unified-release.ts"), "plan", output, root],
                {
                    encoding: "utf8",
                    env: {
                        ...process.env,
                        SOURCE_SHA: sha,
                        RUNTIME_VERSION: "1.0.89-unstable.r123.gabcdef0",
                        CHANNEL: "unstable",
                        VISIBILITY: "internal",
                        SDK_VERSION: "",
                        GITHUB_RUN_ID: "123",
                        WORKFLOW_CREATED_AT: "2026-01-05T00:00:00Z",
                        // Any accidental npm baseline request fails instead of reaching the registry.
                        npm_config_offline: "true",
                        npm_config_cache: join(root, "empty-npm-cache"),
                    },
                }
            );
            expect(JSON.parse(readFileSync(output, "utf8")).sdkVersion).toBe(
                `0.3.1-unstable.123.g${sha.slice(0, 7)}`
            );
        });

        it("records annotated source tags once and preserves their timestamp on retry", () => {
            const calls: string[][] = [];
            const tagSha = "b".repeat(40);
            let created = false;
            const run = (args: string[]) => {
                calls.push(args);
                if (args[1]?.endsWith(`/git/ref/tags/sdk-${identity.sdkVersion}`)) {
                    if (created) return JSON.stringify({ object: { type: "tag", sha: tagSha } });
                    throw Object.assign(new Error("missing"), { stderr: "(HTTP 404)" });
                }
                if (args[1]?.endsWith(`/git/tags/${tagSha}`))
                    return JSON.stringify({
                        tag: `sdk-${identity.sdkVersion}`,
                        object: { type: "commit", sha: identity.sourceSha },
                        tagger: { date: "2026-01-01T00:00:00Z" },
                    });
                if (args[3]?.endsWith("/git/tags")) return JSON.stringify({ sha: tagSha });
                if (args[3]?.endsWith("/git/refs")) {
                    created = true;
                    return "{}";
                }
                throw new Error(`Unexpected Git request: ${args}`);
            };
            recordRuntimeSdkVersion(identity, run);
            const writes = calls.filter((args) => args.includes("POST"));
            expect(writes).toHaveLength(2);
            expect(writes[0]).toContain(`object=${identity.sourceSha}`);
            expect(writes[1]).toContain(`sha=${tagSha}`);
            recordRuntimeSdkVersion(identity, run);
            expect(calls.filter((args) => args.includes("POST"))).toHaveLength(2);
        });

        it.each(["lightweight", "conflicting"])("rejects an existing %s source tag", (kind) => {
            const run = (args: string[]) =>
                args[1].includes("/git/ref/")
                    ? JSON.stringify({
                          object: {
                              type: kind === "lightweight" ? "commit" : "tag",
                              sha: "b".repeat(40),
                          },
                      })
                    : JSON.stringify({
                          tag: `sdk-${identity.sdkVersion}`,
                          object: { type: "commit", sha: "c".repeat(40) },
                          tagger: { date: "2026-01-01T00:00:00Z" },
                      });
            expect(() => recordRuntimeSdkVersion(identity, run)).toThrow(
                kind === "lightweight" ? "annotated" : "another source"
            );
        });
    });

    it("rejects invalid channel/visibility/version combinations before packaging", () => {
        expect(selectedSdks("latest", "public")).toEqual([
            "nodejs",
            "python",
            "go",
            "dotnet",
            "rust",
            "java",
        ]);
        expect(selectedSdks("unstable", "public")).toEqual(selectedSdks("latest", "public"));
        expect(publicationSdks("unstable", "public", "internal")).toEqual(["nodejs"]);
        expect(publicationSdks("latest", "public", "internal")).toEqual(["nodejs", "dotnet"]);
        expect(publicationSdks("unstable", "public", "public")).toEqual([
            "nodejs",
            "dotnet",
            "rust",
            "java",
        ]);
        expect(packagedSdks("unstable", "public")).toContain("python");
        expect(selectedSdks("unstable", "internal")).toEqual(["nodejs"]);
        expect(() => selectedSdks("latest", "internal")).toThrow("must be unstable");
        expect(() => selectedSdks("canary", "public")).toThrow("Invalid SDK channel");
        expect(() => planSdkVersion({ ...plan, override: "0.5.0-preview.1" })).toThrow("channel");
        expect(
            planSdkVersion({ ...plan, channel: "prerelease", override: "0.5.0-chicken.1" })
        ).toBe("0.5.0-chicken.1");
    });
});

describe("same-run SDK artifacts", () => {
    it("keeps Go as a source release without a packaging job or archive", async () => {
        for (const [key, value] of Object.entries({
            SOURCE_SHA: identity.sourceSha,
            RUNTIME_VERSION: identity.runtimeVersion,
            SDK_VERSION: identity.sdkVersion,
            CHANNEL: identity.channel,
            VISIBILITY: identity.visibility,
        }))
            vi.stubEnv(key, value);
        expect(selectedSdks(identity.channel, identity.visibility)).toContain("go");
        await expect(packageRelease("go", "", "", "", "")).rejects.toThrow("not selected");
    });

    it.each([
        ["python", "prerelease"],
        ["python", "unstable"],
        ["dotnet", "prerelease"],
        ["dotnet", "unstable"],
        ["rust", "prerelease"],
    ] as const)(
        "packages %s/%s without running a consumer application",
        async (language, channel) => {
            vi.stubEnv("COPILOT_SKIP_CLI_DOWNLOAD", undefined);
            vi.stubEnv("DOCS_RS", undefined);
            const root = fixture();
            const product = join(root, "product");
            const runtime = join(root, "runtime");
            const output = join(root, "packages");
            mkdirSync(product);
            execFileSync("git", ["init", "--quiet"], { cwd: product });
            execFileSync(
                "git",
                [
                    "-c",
                    "user.name=release-test",
                    "-c",
                    "user.email=release-test@example.invalid",
                    "commit",
                    "--quiet",
                    "--allow-empty",
                    "-m",
                    "Selected product",
                ],
                { cwd: product }
            );
            const sha = execFileSync("git", ["rev-parse", "HEAD"], {
                cwd: product,
                encoding: "utf8",
            }).trim();
            const release = {
                ...identity,
                sourceSha: sha,
                channel,
                sdkVersion:
                    channel === "unstable"
                        ? `0.4.0-unstable.123.g${sha.slice(0, 7)}`
                        : identity.sdkVersion,
                runtimeVersion:
                    channel === "unstable"
                        ? `1.0.89-2.unstable.r123.g${sha.slice(0, 7)}`
                        : identity.runtimeVersion,
            };
            for (const [key, value] of Object.entries({
                SOURCE_SHA: sha,
                RUNTIME_VERSION: release.runtimeVersion,
                SDK_VERSION: release.sdkVersion,
                CHANNEL: release.channel,
                VISIBILITY: release.visibility,
            }))
                vi.stubEnv(key, value);
            for (const schema of ["api.schema.json", "session-events.schema.json"]) {
                write(join(product, "generated", schema), '{"same":"source"}');
                for (const platform of RUNTIME_PLATFORMS) {
                    write(join(runtime, platform, "schemas", schema), '{"same":"source"}');
                }
            }
            const commands: string[] = [];
            if (language === "python") {
                write(
                    join(product, "src", "sdk", "python", "pyproject.toml"),
                    'version = "0.0.0"\n'
                );
            }
            const rust = join(product, "src", "sdk", "rust");
            const bundle = join(root, "bundle");
            if (language === "rust") {
                write(
                    join(rust, "Cargo.toml"),
                    '[package]\nname = "github-copilot-sdk"\nversion = "0.0.0-dev"\n'
                );
                write(join(rust, "Cargo.lock"), "original dependency resolutions");
                const assets = [
                    ["dist-bin", "copilot-linux-x64.tar.gz"],
                    [
                        "dist-pkg-tarballs",
                        `github-copilot-${identity.runtimeVersion}-linux-x64.tgz`,
                    ],
                ].map(([directory, name]) => {
                    const file = join(bundle, "github-release", directory, name);
                    write(file, "runtime bytes");
                    return `${sha256(file)}  ${name}`;
                });
                write(
                    join(bundle, "github-release", "dist-bin", "SHA256SUMS.txt"),
                    assets.join("\n")
                );
            }
            await packageRelease(language, product, bundle, runtime, output, (command, args) => {
                commands.push(command === process.execPath ? args[0] : command);
                if (command === "tar") write(args[1], "selected Rust source");
                if (command === "uv") {
                    expect(args.slice(0, 3)).toEqual(["build", "--wheel", "--out-dir"]);
                    expect(
                        readFileSync(
                            join(product, "src", "sdk", "python", "pyproject.toml"),
                            "utf8"
                        )
                    ).toBe(
                        `version = "${channel === "unstable" ? "0.4.0.dev123" : identity.sdkVersion}"\n`
                    );
                    write(join(args[3], "sdk.whl"), "packaged Python SDK");
                }
                if (command === "dotnet") {
                    expect(args[0]).toBe("pack");
                    expect(args).toContain(`-p:Version=${release.sdkVersion}`);
                    expect(args).toContain(`-p:CopilotCliVersion=${release.runtimeVersion}`);
                    const destination = args.find((arg) => arg.startsWith("-p:PackageOutputPath="));
                    expect(destination).toBe(`-p:PackageOutputPath=${join(output, "dotnet")}`);
                    write(join(output, "dotnet", "sdk.nupkg"), "packaged .NET SDK");
                }
                if (command === "cargo") {
                    expect(readFileSync(join(rust, "Cargo.toml"), "utf8")).toContain(
                        `version = "${identity.sdkVersion}"`
                    );
                    if (args[0] === "update") {
                        expect(args).toEqual(["update", "--workspace"]);
                        write(
                            join(rust, "Cargo.lock"),
                            "updated workspace version; original dependency resolutions"
                        );
                    } else {
                        expect(args).toEqual(["package", "--locked", "--allow-dirty"]);
                        expect(readFileSync(join(rust, "Cargo.lock"), "utf8")).toBe(
                            "updated workspace version; original dependency resolutions"
                        );
                        write(join(rust, "target", "package", "sdk.crate"), "packaged Rust SDK");
                    }
                }
            });
            expect(commands).toEqual([
                "scripts/set-cli-version.js",
                "scripts/set-version.js",
                ...(language === "python"
                    ? ["scripts/inject-cli-version.mjs", "uv"]
                    : language === "rust"
                      ? ["bash", "bash", "cargo", "cargo", "tar"]
                      : ["dotnet"]),
            ]);
            expect(() => verifyPackages(output, release, language)).not.toThrow();
            expect(readdirSync(output).sort()).toEqual([language, "release-manifest.json"].sort());
            if (language !== "rust") {
                vi.stubEnv("CHANNEL", "unstable");
                vi.stubEnv("SDK_VERSION", "0.4.0-unstable.123");
                vi.stubEnv("VISIBILITY", "internal");
                await expect(
                    packageRelease(language, product, "", "", "", () => {
                        throw new Error("Unselected language must not execute");
                    })
                ).rejects.toThrow("not selected");
            }
        }
    );

    it("maps unstable Python versions to unique ordered PyPI development releases", () => {
        expect(pythonPackageVersion(identity)).toBe(identity.sdkVersion);
        expect(pythonPackageVersion({ ...identity, channel: "latest", sdkVersion: "1.0.15" })).toBe(
            "1.0.15"
        );
        for (const run of ["123", "36638597907"]) {
            expect(
                pythonPackageVersion({
                    ...identity,
                    channel: "unstable",
                    sdkVersion: `1.0.15-unstable.${run}.gabcdef0`,
                })
            ).toBe(`1.0.15.dev${run}`);
        }
        expect(() =>
            pythonPackageVersion({
                ...identity,
                channel: "unstable",
                sdkVersion: "1.0.15-unstable",
            })
        ).toThrow("run ID and source SHA");
    });

    it("keeps a staged Rust SDK independent of ancestor runtime workspaces", () => {
        const manifest =
            '[package]\nname = "github-copilot-sdk"\nversion = "0.0.0-dev"\nedition = "2024"\n';
        const stamped = stampRustPackageManifest(manifest, "1.0.15-preview.3");
        expect(stamped).toBe(
            '[package]\nname = "github-copilot-sdk"\nversion = "1.0.15-preview.3"\nedition = "2024"\n\n[workspace]\n'
        );
        expect(stampRustPackageManifest(stamped, "1.0.15-preview.3")).toBe(stamped);
        const existing = `${manifest}\n[workspace.package]\nlicense = "MIT"\n`;
        expect(stampRustPackageManifest(existing, "1.0.15-preview.3")).toBe(
            existing.replace('"0.0.0-dev"', '"1.0.15-preview.3"')
        );
    });

    describe("parallel SDK artifact assembly", () => {
        function part(parts: string, language: string, release = identity): string {
            const directory = join(parts, `sdk-package-${language}`);
            write(join(directory, language, "package.bin"), `${language} bytes`);
            sealPackages(directory, release, language);
            return directory;
        }

        it.each([
            ["latest", "public", ["nodejs", "python", "dotnet", "rust", "java"]],
            ["prerelease", "public", ["nodejs", "python", "dotnet", "rust", "java"]],
            ["unstable", "public", ["nodejs", "python", "dotnet", "rust", "java"]],
            ["unstable", "internal", ["nodejs"]],
        ] as const)(
            "assembles exactly the %s/%s language matrix",
            (channel, visibility, languages) => {
                const release = {
                    ...identity,
                    channel,
                    visibility,
                    sdkVersion:
                        channel === "latest"
                            ? "0.4.0"
                            : channel === "unstable"
                              ? "0.4.0-unstable.123"
                              : identity.sdkVersion,
                };
                const root = fixture();
                const parts = join(root, "parts");
                const output = join(root, "release");
                const matrix = packageMatrix(channel, visibility);
                expect(matrix.include.map((entry) => entry.language)).toEqual(languages);
                expect(selectedSdks(channel, visibility).includes("go")).toBe(
                    visibility === "public"
                );
                expect(matrix.include.every((entry) => entry.name.length > 0)).toBe(true);
                for (const language of languages) part(parts, language, release);
                assemblePackages(parts, output, release);
                expect(() => verifyPackages(output, release)).not.toThrow();
                expect(existsSync(join(output, "go"))).toBe(false);
                for (const language of languages) {
                    expect(readFileSync(join(output, language, "package.bin"), "utf8")).toBe(
                        `${language} bytes`
                    );
                }
                expect(
                    JSON.parse(readFileSync(join(output, "release-manifest.json"), "utf8")).language
                ).toBeUndefined();
            }
        );

        it.each(["missing", "extra", "identity", "tampered", "wrong-language"])(
            "rejects %s language artifacts before producing an assembled release",
            (failure) => {
                const root = fixture();
                const parts = join(root, "parts");
                const output = join(root, "release");
                for (const language of packagedSdks(identity.channel, identity.visibility)) {
                    if (failure === "missing" && language === "java") continue;
                    part(parts, language);
                }
                if (failure === "extra") mkdirSync(join(parts, "sdk-package-unexpected"));
                const node = join(parts, "sdk-package-nodejs");
                if (failure === "identity")
                    sealPackages(node, { ...identity, sdkVersion: "0.4.0-preview.2" }, "nodejs");
                if (failure === "tampered")
                    write(join(node, "nodejs", "package.bin"), "modified package");
                if (failure === "wrong-language") {
                    const manifestPath = join(node, "release-manifest.json");
                    const manifest = JSON.parse(readFileSync(manifestPath, "utf8"));
                    manifest.language = "rust";
                    write(manifestPath, JSON.stringify(manifest));
                }
                expect(() => assemblePackages(parts, output, identity)).toThrow();
                expect(existsSync(output)).toBe(false);
            }
        );

        it("rejects artifacts for a language outside its matrix job's scope", () => {
            const root = fixture();
            write(join(root, "nodejs", "package.tgz"), "node");
            write(join(root, "rust", "package.crate"), "rust");
            expect(() => sealPackages(root, identity, "nodejs")).toThrow("Unexpected SDK artifact");
        });
    });

    it("retains the Python wheel rather than uv output-directory metadata", () => {
        const root = fixture();
        const output = join(root, "uv-output");
        const retained = join(root, "retained");
        const wheel = "github_copilot_sdk-1.0.15rc3-py3-none-any.whl";
        write(join(output, ".gitignore"), "*");
        write(join(output, wheel), "built wheel");
        write(join(output, "build.log"), "auxiliary output");
        mkdirSync(retained);
        expect(copyPackages(output, retained, [".whl"])).toEqual([join(retained, wheel)]);
        expect(readdirSync(retained)).toEqual([wheel]);
        expect(readFileSync(join(retained, wheel), "utf8")).toBe("built wheel");
    });

    it("accepts unstable packaging only when the selected Rust implementation supports its release location", () => {
        const rust = join(fixture(), "unstable-rust");
        const implementation =
            '"release-url"\nhttps://github.com/github/copilot-sdk/releases/download/runtime-';
        write(join(rust, "build", "in_process.rs"), implementation);
        const commands: string[][] = [];
        expect(() =>
            writeRustReleaseSnapshots(
                rust,
                "bundle",
                {
                    ...identity,
                    channel: "unstable",
                    sdkVersion: "0.4.0-unstable.123",
                },
                (args) => commands.push(args)
            )
        ).not.toThrow();
        expect(commands).toHaveLength(2);
        expect(commands[0]).toContain(
            `https://github.com/github/copilot-sdk/releases/download/runtime-${identity.runtimeVersion}`
        );
        expect(readFileSync(join(rust, "build", "in_process.rs"), "utf8")).toBe(implementation);
    });

    it.each(["latest", "prerelease"] as const)(
        "uses reviewed snapshot producers for compatible old %s source without changing Rust code",
        (channel) => {
            const rust = join(fixture(), "old-rust");
            const implementation = "selected legacy Rust runtime implementation";
            write(join(rust, "build.rs"), "selected build entrypoint");
            write(join(rust, "build", "in_process.rs"), implementation);
            write(join(rust, "Cargo.toml"), "selected manifest");
            const commands: string[][] = [];
            const releaseIdentity = {
                ...identity,
                channel,
                sdkVersion: channel === "latest" ? "0.4.0" : identity.sdkVersion,
            };
            writeRustReleaseSnapshots(rust, "bundle", releaseIdentity, (args) =>
                commands.push(args)
            );
            expect(commands).toHaveLength(2);
            expect(commands.map((args) => args.at(-1))).toEqual([
                join(rust, "cli-version.txt"),
                join(rust, "cli-version-in-process.txt"),
            ]);
            for (const args of commands) {
                expect(args[0]).not.toContain(rust);
                expect(args).toEqual(
                    expect.arrayContaining([
                        "--version",
                        identity.runtimeVersion,
                        "--release-url",
                        `https://github.com/github/copilot-cli/releases/download/v${identity.runtimeVersion}`,
                        "--checksums",
                        join("bundle", "github-release", "dist-bin", "SHA256SUMS.txt"),
                        "--output",
                    ])
                );
            }
            expect(existsSync(join(rust, "scripts"))).toBe(false);
            expect(readFileSync(join(rust, "build.rs"), "utf8")).toBe("selected build entrypoint");
            expect(readFileSync(join(rust, "build", "in_process.rs"), "utf8")).toBe(implementation);
            expect(readFileSync(join(rust, "Cargo.toml"), "utf8")).toBe("selected manifest");
            expect(() =>
                writeRustReleaseSnapshots(
                    rust,
                    "bundle",
                    { ...identity, channel: "unstable" },
                    () => {
                        throw new Error("unsupported source must not run snapshot tools");
                    }
                )
            ).toThrow("Selected Rust source does not support SDK-hosted unstable");
        }
    );

    it("stages all verified platforms and rejects version, SHA, checksum and support-file mismatches", async () => {
        const root = fixture();
        const bundle = join(root, "bundle");
        const packages = join(bundle, "github-release", "dist-pkg-tarballs");
        const binaries = join(bundle, "github-release", "dist-bin");
        mkdirSync(packages, { recursive: true });
        mkdirSync(binaries, { recursive: true });
        write(
            join(bundle, "github-release", "release-identity.json"),
            JSON.stringify({ ...identity, signed: true })
        );
        const sums: string[] = [];
        for (const platform of RUNTIME_PLATFORMS) {
            const input = join(root, "input", platform);
            write(
                join(input, "package", "package.json"),
                JSON.stringify({ version: identity.runtimeVersion })
            );
            for (const file of [
                "LICENSE.md",
                "copilot-sdk/extension.js",
                "preloads/extension_bootstrap.mjs",
                `prebuilds/${platform}/runtime.node`,
                `prebuilds/${platform}/copilot-runtime${platform.startsWith("win32") ? ".exe" : ""}`,
            ]) {
                write(join(input, "package", file), "runtime");
            }
            const name = `github-copilot-${identity.runtimeVersion}-${platform}.tgz`;
            await createTar({ cwd: input, file: join(packages, name), gzip: true }, ["package"]);
            sums.push(`${sha256(join(packages, name))}  ${name}`);
            const binary = `copilot-${platform}.${platform.startsWith("win32") ? "zip" : "tar.gz"}`;
            write(join(binaries, binary), "signed executable archive");
            sums.push(`${sha256(join(binaries, binary))}  ${binary}`);
        }
        write(join(binaries, "SHA256SUMS.txt"), `${sums.join("\n")}\n`);
        const output = join(root, "runtime");
        await stageRuntime(bundle, output, identity);
        expect(
            readFileSync(
                join(output, "linux-x64", "prebuilds", "linux-x64", "runtime.node"),
                "utf8"
            )
        ).toBe("runtime");
        const cache = join(root, "rust-cache");
        seedRustRuntime(bundle, cache, identity);
        expect(
            readFileSync(
                join(cache, `v${identity.runtimeVersion}-copilot-linux-x64.tar.gz`),
                "utf8"
            )
        ).toBe("signed executable archive");
        const unstable = {
            ...identity,
            channel: "unstable" as const,
            sdkVersion: "0.4.0-unstable.123.gaaaaaaa",
        };
        seedRustRuntime(bundle, cache, unstable);
        expect(rustReleaseLocation(unstable).url).toBe(
            `https://github.com/github/copilot-sdk/releases/download/runtime-${identity.runtimeVersion}`
        );
        expect(
            readFileSync(
                join(
                    cache,
                    `copilot-sdk-runtime-${identity.runtimeVersion}-copilot-linux-x64.tar.gz`
                ),
                "utf8"
            )
        ).toBe("signed executable archive");
        expect(() => rustReleaseLocation({ ...unstable, visibility: "internal" })).toThrow(
            "Private Rust"
        );
        await expect(
            stageRuntime(bundle, output, { ...identity, sourceSha: "b".repeat(40) })
        ).rejects.toThrow("different source");
        await expect(
            stageRuntime(bundle, output, { ...identity, runtimeVersion: "9.0.0" })
        ).rejects.toThrow("different version");
        const linuxInput = join(root, "input", "linux-x64");
        const linuxAsset = `github-copilot-${identity.runtimeVersion}-linux-x64.tgz`;
        const repackLinux = async () => {
            await createTar({ cwd: linuxInput, file: join(packages, linuxAsset), gzip: true }, [
                "package",
            ]);
            sums[sums.findIndex((line) => line.endsWith(`  ${linuxAsset}`))] =
                `${sha256(join(packages, linuxAsset))}  ${linuxAsset}`;
            write(join(binaries, "SHA256SUMS.txt"), `${sums.join("\n")}\n`);
        };
        write(join(linuxInput, "package", "package.json"), '{"version":"9.0.0"}');
        await repackLinux();
        await expect(stageRuntime(bundle, output, identity)).rejects.toThrow(
            "Wrong runtime version"
        );
        write(
            join(linuxInput, "package", "package.json"),
            JSON.stringify({ version: identity.runtimeVersion })
        );
        rmSync(join(linuxInput, "package", "preloads", "extension_bootstrap.mjs"));
        await repackLinux();
        await expect(stageRuntime(bundle, output, identity)).rejects.toThrow(
            "extension_bootstrap.mjs"
        );
        write(join(packages, linuxAsset), "corrupt");
        await expect(stageRuntime(bundle, output, identity)).rejects.toThrow("Checksum mismatch");
        expect(() => checksums(`${sums[0]}\n${sums[0]}`)).toThrow("Duplicate checksum");
    });

    it("binds retained packages to source, channel, visibility, version and exact bytes", () => {
        const root = fixture();
        const internal = {
            ...identity,
            sdkVersion: "0.4.0-unstable.123.gaaaaaaa",
            channel: "unstable" as const,
            visibility: "internal" as const,
        };
        write(join(root, "nodejs", "package.tgz"), "exact package");
        sealPackages(root, internal);
        expect(() => verifyPackages(root, internal)).not.toThrow();
        expect(() => verifyPackages(root, { ...internal, visibility: "public" })).toThrow(
            "visibility"
        );
        expect(() => verifyPackages(root, { ...internal, sourceSha: "b".repeat(40) })).toThrow(
            "sourceSha"
        );
        write(join(root, "nodejs", "extra.tgz"), "unexpected");
        expect(() => verifyPackages(root, internal)).toThrow("Unexpected retained files");
        rmSync(join(root, "nodejs", "extra.tgz"));
        write(join(root, "nodejs", "package.tgz"), "wrong package");
        expect(() => verifyPackages(root, internal)).toThrow("Retained");
    });

    it.each([false, true])(
        "uses native Cargo with immutable source, lockfile and runtime pins (wrong pin=%s)",
        async (wrongPin) => {
            const root = fixture();
            const source = join(root, "source");
            const retained = join(root, "retained");
            const product = join(root, "product");
            const archiveName = `github-copilot-sdk-${identity.sdkVersion}.crate`;
            write(join(retained, archiveName), "verified crate");
            write(
                join(source, "rust", "Cargo.toml"),
                `[package]\nversion = "${identity.sdkVersion}"\n`
            );
            write(join(source, "rust", "Cargo.lock"), "retained Cargo.lock");
            write(
                join(source, "rust", "cli-version.txt"),
                `version=${wrongPin ? "9.0.0" : identity.runtimeVersion}\n`
            );
            write(
                join(source, "rust", "cli-version-in-process.txt"),
                `version=${identity.runtimeVersion}\n`
            );
            await createTar({ cwd: source, file: join(retained, "source.tar.gz"), gzip: true }, [
                "rust",
            ]);
            const commands: string[][] = [];
            const run = (command: string, args: string[], cwd: string) => {
                commands.push([command, ...args]);
                if (command === "git") return identity.sourceSha;
                expect(readFileSync(join(cwd, "Cargo.lock"), "utf8")).toBe("retained Cargo.lock");
                expect(readFileSync(join(cwd, "cli-version.txt"), "utf8")).toBe(
                    `version=${identity.runtimeVersion}\n`
                );
                expect(readFileSync(join(cwd, "cli-version-in-process.txt"), "utf8")).toBe(
                    `version=${identity.runtimeVersion}\n`
                );
                return "";
            };
            const publication = publishRust(retained, identity, product, run);
            if (wrongPin) await expect(publication).rejects.toThrow("runtime pin differs");
            else await expect(publication).resolves.toBeUndefined();
            expect(commands.filter((command) => command[1] === "publish")).toHaveLength(
                wrongPin ? 0 : 1
            );
            expect(commands.some((command) => command[1] === "package")).toBe(false);
            for (const command of commands.filter((command) => command[0] === "cargo")) {
                expect(command).toEqual(
                    expect.arrayContaining(["--locked", "--allow-dirty", "--no-verify"])
                );
            }
            await expect(
                publishRust(retained, identity, product, () => "b".repeat(40))
            ).rejects.toThrow("selected product SHA");
        }
    );

    it("uses native Maven deploy with retained classifier inputs and the exact runtime pin", async () => {
        const root = fixture();
        const source = join(root, "source");
        const retained = join(root, "retained");
        const product = join(root, "product");
        const runtime = join(retained, `github-copilot-${identity.runtimeVersion}-linux-x64.tgz`);
        write(join(source, "java", "pom.xml"), "retained Maven source");
        write(
            join(retained, "nodejs-package.json"),
            JSON.stringify({ copilotCliVersion: identity.runtimeVersion })
        );
        write(join(product, "src", "sdk", "nodejs", "package.json"), "{}");
        write(runtime, "verified runtime archive");
        await createTar({ cwd: source, file: join(retained, "source.tar.gz"), gzip: true }, [
            "java",
        ]);
        const commands: string[][] = [];
        const run = (command: string, args: string[], cwd: string, env?: NodeJS.ProcessEnv) => {
            commands.push([command, ...args]);
            if (command === "git") return identity.sourceSha;
            expect(command).toBe("bash");
            expect(readFileSync(join(cwd, "pom.xml"), "utf8")).toBe("retained Maven source");
            expect(args).toEqual(
                expect.arrayContaining([
                    "mvnw",
                    "deploy",
                    "-Prelease",
                    `-Drevision=${identity.sdkVersion}`,
                ])
            );
            expect(args.filter((arg) => arg.startsWith("-Dcopilot.native.external."))).toHaveLength(
                6
            );
            expect(env?.COPILOT_CLI_RELEASE_TARBALL).toBe(runtime);
            expect(env?.COPILOT_CLI_RELEASE_SHA256).toBe(sha256(runtime));
            return "";
        };
        await publishJava(retained, identity, product, run);
        expect(commands.filter((command) => command[0] === "bash")).toHaveLength(1);
        write(join(retained, "nodejs-package.json"), '{"copilotCliVersion":"9.0.0"}');
        await expect(publishJava(retained, identity, product, run)).rejects.toThrow(
            "runtime pin differs"
        );
        expect(commands.filter((command) => command[0] === "bash")).toHaveLength(1);
    });

    it.each(["prerelease", "unstable"] as const)(
        "creates immutable %s source tags and announces only normal releases",
        async (channel) => {
            const release = {
                ...identity,
                channel,
                sdkVersion:
                    channel === "unstable" ? "0.4.0-unstable.123.gaaaaaaa" : identity.sdkVersion,
            };
            const snapshot = "b".repeat(40);
            const requests: string[][] = [];
            const gh = (args: string[]) => {
                requests.push(args);
                if (args[0] === "api" && args[1].includes("/git/ref/")) {
                    throw Object.assign(new Error("not found"), { stderr: "(HTTP 404)" });
                }
                if (args[0] === "api" && args[1].includes("/releases/tags/")) {
                    throw Object.assign(new Error("not found"), { stderr: "(HTTP 404)" });
                }
                if (args.includes("--slurp")) {
                    return JSON.stringify([
                        [
                            {
                                tag_name: "v0.3.0",
                                draft: false,
                                prerelease: false,
                                published_at: "2026-09-20T00:00:00Z",
                            },
                        ],
                    ]);
                }
                return "{}";
            };
            await publishSourceRelease(release, snapshot, gh);
            const tags = requests.filter((args) => args.includes("POST"));
            expect(tags).toHaveLength(4);
            expect(
                tags.some((args) => args.includes(`ref=refs/tags/go/v${release.sdkVersion}`))
            ).toBe(true);
            expect(tags.every((args) => args.includes(`sha=${snapshot}`))).toBe(true);
            expect(tags.flat()).not.toContain(`sha=${identity.sourceSha}`);
            if (channel === "unstable") {
                expect(
                    requests.every(
                        (args) => args[0] === "api" && args.some((arg) => arg.includes("/git/"))
                    )
                ).toBe(true);
            } else {
                expect(requests.at(-1)).toContain("--latest=false");
                expect(requests.at(-1)).toEqual(
                    expect.arrayContaining(["--generate-notes", "--notes-start-tag", "v0.3.0"])
                );
            }
            await expect(publishSourceRelease(release, identity.sourceSha, gh)).rejects.toThrow(
                "private runtime SHA"
            );
            await expect(
                publishSourceRelease(release, snapshot, () =>
                    JSON.stringify({ object: { type: "commit", sha: "c".repeat(40) } })
                )
            ).rejects.toThrow("another source");
            if (channel === "unstable") {
                await publishSourceRelease(release, snapshot, (args) => {
                    expect(args[0]).toBe("api");
                    expect(args[1]).toContain("/git/ref/tags/");
                    return JSON.stringify({ object: { type: "commit", sha: snapshot } });
                });
                expect(() => recordRuntimeSdkVersion(release, gh)).toThrow("Only normal public");
            }
        }
    );

    it("keeps stable versus prerelease changelog baselines and excludes hosted runtime releases", () => {
        const releases = [
            {
                tag_name: "v0.3.0",
                draft: false,
                prerelease: false,
                published_at: "2026-09-20T00:00:00Z",
            },
            {
                tag_name: "v0.4.0-preview.0",
                draft: false,
                prerelease: true,
                published_at: "2026-09-21T00:00:00Z",
            },
            {
                tag_name: "runtime-1.0.89-unstable.1",
                draft: false,
                prerelease: true,
                published_at: "2026-09-22T00:00:00Z",
            },
            {
                tag_name: "v0.5.0",
                draft: true,
                prerelease: false,
                published_at: "2026-09-23T00:00:00Z",
            },
        ];
        expect(previousSdkReleaseTag(releases, identity)).toBe("v0.4.0-preview.0");
        expect(
            previousSdkReleaseTag(releases, { ...identity, channel: "latest", sdkVersion: "0.4.0" })
        ).toBe("v0.3.0");
    });
});
