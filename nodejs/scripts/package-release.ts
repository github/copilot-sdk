/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { cpSync, existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { basename, dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { createPackageSetManifest } from "./release-manifest.js";
import { RUNTIME_PLATFORMS } from "../src/runtimeArtifacts.js";
import {
    checksums,
    identityFromEnvironment,
    rustReleaseLocation,
    sealPackages,
    seedRustRuntime,
    releaseLanguages,
    type ReleaseIdentity,
} from "./unified-release.js";

export const JAVA_CLASSIFIERS = {
    "linux-arm64": "linux.arm64",
    "linuxmusl-x64": "linuxmusl.x64",
    "linuxmusl-arm64": "linuxmusl.arm64",
    "win32-x64": "win32",
    "win32-arm64": "win32.arm64",
    "darwin-arm64": "darwin",
    "darwin-x64": "darwin.x64",
};

export function copyPackages(from: string, to: string, suffixes: string[]): string[] {
    const matches = readdirSync(from).filter((file) =>
        suffixes.some((suffix) => file.endsWith(suffix))
    );
    assert(matches.length > 0, `No packages in ${from}`);
    return matches.map((file) => {
        const destination = join(to, file);
        cpSync(join(from, file), destination);
        return destination;
    });
}

export function stampRustPackageManifest(manifest: string, version: string): string {
    const stamped = manifest.replace(/^version = ".*"$/m, `version = "${version}"`);
    // The selected product checkout is nested beneath another Cargo workspace.
    // Keep this independent SDK out of both runtime workspaces during packaging.
    return /^\[workspace(?:\.|\])/m.test(stamped)
        ? stamped
        : `${stamped.trimEnd()}\n\n[workspace]\n`;
}

export function pythonPackageVersion(identity: ReleaseIdentity): string {
    if (identity.channel !== "unstable") return identity.sdkVersion;
    const match = /^(\d+\.\d+\.\d+)-unstable\.([1-9]\d*)\.g[0-9a-f]{7}$/.exec(identity.sdkVersion);
    assert(match, "Expected an unstable SDK version with a run ID and source SHA");
    // PyPI rejects SemVer's unstable suffix and local (+sha) versions.
    return `${match[1]}.dev${match[2]}`;
}

/** Write pins with reviewed tooling without changing selected-source scripts or implementation. */
export function writeRustReleaseSnapshots(
    rust: string,
    bundle: string,
    identity: ReleaseIdentity,
    run: (args: string[]) => void = (args) => {
        execFileSync("bash", args, { cwd: rust, stdio: "inherit" });
    }
): void {
    if (identity.channel === "unstable") {
        const implementation = join(rust, "build", "in_process.rs");
        const source = existsSync(implementation) ? readFileSync(implementation, "utf8") : "";
        assert(
            source.includes('"release-url"') &&
                source.includes("https://github.com/github/copilot-sdk/releases/download/runtime-"),
            "Selected Rust source does not support SDK-hosted unstable runtime acquisition"
        );
    }
    const reviewedScripts = resolve(dirname(fileURLToPath(import.meta.url)), "../../rust/scripts");
    for (const [script, output] of [
        ["snapshot-bundled-cli-version.sh", "cli-version.txt"],
        ["snapshot-bundled-in-process-version.sh", "cli-version-in-process.txt"],
    ]) {
        run([
            join(reviewedScripts, script),
            "--version",
            identity.runtimeVersion,
            "--release-url",
            rustReleaseLocation(identity).url,
            "--checksums",
            join(bundle, "github-release", "dist-bin", "SHA256SUMS.txt"),
            "--output",
            join(rust, output),
        ]);
    }
}

export async function packageRelease(
    language: string,
    product: string,
    bundle: string,
    runtime: string,
    output: string,
    execute: (command: string, args: string[], cwd: string, env: NodeJS.ProcessEnv) => void = (
        command,
        args,
        cwd,
        env
    ) => {
        execFileSync(command, args, { cwd, env, stdio: "inherit" });
    }
): Promise<void> {
    const identity = identityFromEnvironment();
    releaseLanguages(identity, language);
    const sdk = join(product, "src", "sdk");
    const work = join(product, "sdk-release-work");
    mkdirSync(work, { recursive: true });
    const environment: NodeJS.ProcessEnv = {
        ...process.env,
        COPILOT_RUNTIME_SOURCE: "published",
        COPILOT_SDK_RUNTIME_PACKAGE_DIR: runtime,
        TMPDIR: work,
        TMP: work,
        TEMP: work,
        VERSION: identity.sdkVersion,
    };
    const run = (command: string, args: string[], cwd: string, env = environment) =>
        execute(command, args, cwd, env);
    const node = (args: string[], cwd = sdk) => run(process.execPath, args, cwd);
    const retained = (language: string) => {
        const directory = join(output, language);
        mkdirSync(directory, { recursive: true });
        return directory;
    };
    const source = execFileSync("git", ["rev-parse", "HEAD"], {
        cwd: product,
        encoding: "utf8",
    }).trim();
    assert.equal(
        source,
        identity.sourceSha,
        "SDK product checkout is not the selected runtime source"
    );
    for (const schema of ["api.schema.json", "session-events.schema.json"]) {
        const expected = JSON.parse(readFileSync(join(product, "generated", schema), "utf8"));
        for (const platform of RUNTIME_PLATFORMS) {
            assert.deepEqual(
                JSON.parse(readFileSync(join(runtime, platform, "schemas", schema), "utf8")),
                expected,
                `${platform} runtime schema ${schema} does not match selected SDK sources`
            );
        }
    }
    const nodeRoot = join(sdk, "nodejs");
    node(["scripts/set-cli-version.js", identity.runtimeVersion, "--local-package"], nodeRoot);
    node(["scripts/set-version.js"], nodeRoot);
    if (language === "nodejs") {
        const nodeManifestPath = join(nodeRoot, "package.json");
        const nodeManifest = JSON.parse(readFileSync(nodeManifestPath, "utf8"));
        // npm trusted publishing validates the executing repository; private-source provenance is unavailable.
        nodeManifest.repository = {
            type: "git",
            url: "https://github.com/github/copilot-agent-runtime.git",
            directory: "src/sdk/nodejs",
        };
        nodeManifest.copilotRuntime = {
            sourceSha: identity.sourceSha,
            version: identity.runtimeVersion,
            visibility: identity.visibility,
        };
        writeFileSync(nodeManifestPath, `${JSON.stringify(nodeManifest, null, 4)}\n`);
        run("npm", ["run", "build"], nodeRoot);
        run("npm", ["run", "pack:release"], nodeRoot);
        run("npm", ["run", "verify:release-packages"], nodeRoot);
        const nodeOutput = retained("nodejs");
        copyPackages(nodeRoot, nodeOutput, [".tgz"]);
        writeFileSync(
            join(nodeOutput, "package-set-manifest.json"),
            `${JSON.stringify(
                await createPackageSetManifest(nodeOutput, identity.sdkVersion),
                null,
                2
            )}\n`
        );
    }

    if (language === "python") {
        const python = join(sdk, "python");
        const manifest = join(python, "pyproject.toml");
        writeFileSync(
            manifest,
            readFileSync(manifest, "utf8").replace(
                /^version = ".*"$/m,
                `version = "${pythonPackageVersion(identity)}"`
            )
        );
        node(["scripts/inject-cli-version.mjs"], python);
        const pythonDist = join(work, "python-dist");
        run("uv", ["build", "--wheel", "--out-dir", pythonDist], python);
        const wheels = copyPackages(pythonDist, retained("python"), [".whl"]);
        assert.equal(wheels.length, 1, "Expected one Python release wheel");
    }
    if (language === "dotnet") {
        run(
            "dotnet",
            [
                "pack",
                "src/GitHub.Copilot.SDK.csproj",
                "-c",
                "Release",
                `-p:Version=${identity.sdkVersion}`,
                `-p:CopilotCliVersion=${identity.runtimeVersion}`,
                "-p:CopilotSkipCliDownload=true",
                `-p:RepositoryCommit=${identity.sourceSha}`,
                "-p:RepositoryUrl=https://github.com/github/copilot-agent-runtime",
                `-p:PackageOutputPath=${retained("dotnet")}`,
            ],
            join(sdk, "dotnet")
        );
    }
    if (language === "rust") {
        const rust = join(sdk, "rust");
        const manifest = join(rust, "Cargo.toml");
        writeFileSync(
            manifest,
            stampRustPackageManifest(readFileSync(manifest, "utf8"), identity.sdkVersion)
        );
        writeRustReleaseSnapshots(rust, bundle, identity, (args) => run("bash", args, rust));
        const cache = join(work, "rust-cache");
        seedRustRuntime(bundle, cache, identity);
        const rustEnvironment: NodeJS.ProcessEnv = {
            ...environment,
            BUNDLED_CLI_CACHE_DIR: cache,
            COPILOT_CLI_EXTRACT_DIR: join(work, "rust-runtime"),
            CARGO_TARGET_DIR: join(rust, "target"),
        };
        assert.equal(
            rustEnvironment.COPILOT_SKIP_CLI_DOWNLOAD,
            undefined,
            "Rust acquisition verification cannot skip downloads"
        );
        assert.equal(
            rustEnvironment.DOCS_RS,
            undefined,
            "Rust acquisition verification cannot use docs.rs mode"
        );
        // Refresh the stamped workspace version without upgrading locked dependencies.
        run("cargo", ["update", "--workspace"], rust, rustEnvironment);
        run("cargo", ["package", "--locked", "--allow-dirty"], rust, rustEnvironment);
        const rustOutput = retained("rust");
        copyPackages(join(rust, "target", "package"), rustOutput, [".crate"]);
        run(
            "tar",
            ["-czf", join(rustOutput, "source.tar.gz"), "--exclude=rust/target", "-C", sdk, "rust"],
            sdk
        );
    }
    if (language === "java") {
        const java = join(sdk, "java");
        const javaOutput = retained("java");
        const release = join(bundle, "github-release");
        const hashes = checksums(readFileSync(join(release, "dist-bin", "SHA256SUMS.txt"), "utf8"));
        const nativeArgs: string[] = [];
        for (const [classifier, property] of Object.entries(JAVA_CLASSIFIERS)) {
            const asset = `github-copilot-${identity.runtimeVersion}-${classifier}.tgz`;
            const staging = join(work, "java-native");
            const jar = join(
                work,
                `copilot-sdk-java-runtime-${identity.sdkVersion}-${classifier}.jar`
            );
            run(
                process.execPath,
                ["copilot-native/scripts/fetch-native.mjs", sdk, staging, classifier],
                java,
                {
                    ...environment,
                    COPILOT_CLI_RELEASE_TARBALL: join(release, "dist-pkg-tarballs", asset),
                    COPILOT_CLI_RELEASE_SHA256: hashes.get(asset)!,
                }
            );
            run(
                "jar",
                ["--create", "--file", jar, "-C", join(staging, classifier), "native"],
                java
            );
            node(
                [
                    "copilot-native/scripts/validate-native-artifact.mjs",
                    "classifier",
                    classifier,
                    jar,
                    basename(jar),
                    sdk,
                ],
                java
            );
            nativeArgs.push(`-Dcopilot.native.external.${property}.classifier.path=${jar}`);
        }
        const linuxAsset = `github-copilot-${identity.runtimeVersion}-linux-x64.tgz`;
        const mavenRepository = join(work, "maven-repository");
        // Retain packaging outputs and source; native Maven deployment rebuilds and signs from that source.
        run(
            "bash",
            [
                "mvnw",
                "-B",
                "install",
                "-Prelease",
                "-DskipTests",
                "-DskipITs",
                `-Drevision=${identity.sdkVersion}`,
                "-Dcopilot.native.libc=glibc",
                `-Dmaven.repo.local=${mavenRepository}`,
                ...nativeArgs,
            ],
            java,
            {
                ...environment,
                COPILOT_CLI_RELEASE_TARBALL: join(release, "dist-pkg-tarballs", linuxAsset),
                COPILOT_CLI_RELEASE_SHA256: hashes.get(linuxAsset)!,
            }
        );
        for (const artifact of ["copilot-sdk-java", "copilot-sdk-java-runtime"]) {
            const coordinate = join("com", "github", artifact, identity.sdkVersion);
            const destination = join(javaOutput, artifact);
            mkdirSync(destination, { recursive: true });
            for (const file of readdirSync(join(mavenRepository, coordinate))) {
                if (!/\.(jar|pom|asc)$/.test(file)) continue;
                const path = join(destination, file);
                cpSync(join(mavenRepository, coordinate, file), path);
            }
            for (const file of readdirSync(destination).filter((name) =>
                /\.(jar|pom)$/.test(name)
            )) {
                assert(
                    readFileSync(join(destination, `${file}.asc`)).length > 0,
                    `Missing signature: ${file}`
                );
            }
        }
        cpSync(join(release, "dist-pkg-tarballs", linuxAsset), join(javaOutput, linuxAsset));
        cpSync(join(nodeRoot, "package.json"), join(javaOutput, "nodejs-package.json"));
        run(
            "tar",
            [
                "-czf",
                join(javaOutput, "source.tar.gz"),
                "--exclude=*/target",
                "--exclude=*/node_modules",
                "-C",
                sdk,
                "java",
            ],
            sdk
        );
    }
    sealPackages(output, identity, language);
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
    const args = process.argv.slice(2);
    if (args[0] === "--help") {
        console.log(`Usage: package-release.ts <language> <product-checkout> <runtime-bundle> <staged-runtime> <output>
Builds and verifies one selected SDK without registry/source publication.
Run unified-release.ts stage-runtime first. Required environment:
SOURCE_SHA, RUNTIME_VERSION, SDK_VERSION, CHANNEL, VISIBILITY.
Normal Java packages require an imported GPG key and JAVA_GPG_PASSPHRASE.`);
    } else {
        assert.equal(
            args.length,
            5,
            "Expected language, product, runtime bundle, staged runtime and output paths; use --help"
        );
        await packageRelease(
            args[0],
            ...(args.slice(1).map((value) => resolve(value)) as [string, string, string, string])
        );
    }
}
