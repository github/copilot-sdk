import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { spawn } from "node:child_process";
import { mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { basename, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { x as extractTar } from "tar";
import { RUNTIME_PLATFORMS, validateFile } from "../src/runtimeArtifacts.js";

interface CommandResult {
    status: number;
    stderr: string;
    stdout: string;
}

interface RuntimePackageManifest {
    copilotRuntime?: {
        sourceRepository?: string;
        sourceSha?: string;
    };
    cpu?: string[];
    libc?: string[];
    name?: string;
    os?: string[];
    repository?: string | { url?: string };
    version?: string;
}

export interface AcquireRuntimePackagesOptions {
    outputDirectory: string;
    registry: string;
    runtimeSha: string;
    runtimeVersion: string;
}

export type CommandRunner = (
    command: string,
    args: string[],
    options?: { cwd?: string }
) => Promise<CommandResult>;

export function getSourceRuntimePackageName(platform: string): string {
    return `@github/copilot-${platform}`;
}

export function runCommand(
    command: string,
    args: string[],
    options: { cwd?: string } = {}
): Promise<CommandResult> {
    return new Promise((resolveResult, reject) => {
        const child = spawn(command, args, {
            cwd: options.cwd,
            shell: false,
        });
        let stdout = "";
        let stderr = "";
        child.stdout.on("data", (chunk) => (stdout += chunk));
        child.stderr.on("data", (chunk) => (stderr += chunk));
        child.on("error", reject);
        child.on("close", (status) => resolveResult({ status: status ?? 1, stdout, stderr }));
    });
}

function parseJsonOutput<T>(result: CommandResult, description: string): T {
    if (result.status !== 0) {
        throw new Error(
            `${description} failed with exit code ${result.status}: ${result.stderr || result.stdout}`
        );
    }
    try {
        return JSON.parse(result.stdout) as T;
    } catch {
        throw new Error(`${description} returned invalid JSON: ${result.stdout}`);
    }
}

function validatePlatformMetadata(manifest: RuntimePackageManifest, platform: string): void {
    const [osName, cpu] = platform.replace("linuxmusl", "linux").split("-");
    assert.deepEqual(manifest.os, [osName], `Invalid os metadata for ${platform}`);
    assert.deepEqual(manifest.cpu, [cpu], `Invalid cpu metadata for ${platform}`);
    if (platform.startsWith("linux")) {
        assert.deepEqual(
            manifest.libc,
            [platform.startsWith("linuxmusl") ? "musl" : "glibc"],
            `Invalid libc metadata for ${platform}`
        );
    } else {
        assert.equal(manifest.libc, undefined, `Unexpected libc metadata for ${platform}`);
    }
}

function repositoryUrl(repository: RuntimePackageManifest["repository"]): string {
    return typeof repository === "string" ? repository : (repository?.url ?? "");
}

export function validateRuntimePackageRoot(
    packageRoot: string,
    platform: string,
    runtimeVersion: string,
    runtimeSha: string
): void {
    const manifestPath = join(packageRoot, "package.json");
    validateFile(manifestPath, `${platform} runtime package manifest`);
    const manifest = JSON.parse(readFileSync(manifestPath, "utf8")) as RuntimePackageManifest;
    assert.equal(manifest.name, getSourceRuntimePackageName(platform));
    assert.equal(manifest.version, runtimeVersion);
    assert.equal(manifest.copilotRuntime?.sourceRepository, "github/copilot-agent-runtime");
    assert.equal(manifest.copilotRuntime?.sourceSha, runtimeSha.toLowerCase());
    assert(
        repositoryUrl(manifest.repository).includes("github/copilot-agent-runtime"),
        `${manifest.name} does not link to github/copilot-agent-runtime`
    );
    validatePlatformMetadata(manifest, platform);

    const windows = platform.startsWith("win32");
    for (const requiredPath of [
        "LICENSE.md",
        windows ? "copilot.exe" : "copilot",
        join("prebuilds", platform, windows ? "copilot-runtime.exe" : "copilot-runtime"),
        join("prebuilds", platform, "runtime.node"),
        join("copilot-sdk", "extension.js"),
        join("preloads", "extension_bootstrap.mjs"),
        join("sdk", "index.js"),
    ]) {
        validateFile(join(packageRoot, requiredPath), `${manifest.name} ${requiredPath}`);
    }
}

function sha512Integrity(path: string): string {
    return `sha512-${createHash("sha512").update(readFileSync(path)).digest("base64")}`;
}

export async function acquireRuntimePackages(
    options: AcquireRuntimePackagesOptions,
    runner: CommandRunner = runCommand
): Promise<void> {
    assert.match(options.runtimeSha, /^[0-9a-f]{40}$/, "Runtime SHA must be lowercase full SHA");
    assert.match(options.registry, /^https:\/\//, "Runtime registry must use HTTPS");
    const outputDirectory = resolve(options.outputDirectory);
    const tarballDirectory = join(outputDirectory, "tarballs");
    mkdirSync(tarballDirectory, { recursive: true });
    const acquired: {
        filename: string;
        integrity: string;
        name: string;
        platform: string;
        version: string;
    }[] = [];

    for (const platform of RUNTIME_PLATFORMS) {
        const packageName = getSourceRuntimePackageName(platform);
        const spec = `${packageName}@${options.runtimeVersion}`;
        const viewResult = await runner("npm", [
            "view",
            spec,
            "dist.integrity",
            "--json",
            "--registry",
            options.registry,
        ]);
        const registryIntegrity = parseJsonOutput<string>(
            viewResult,
            `Reading registry integrity for ${spec}`
        );
        assert.match(
            registryIntegrity,
            /^sha512-[A-Za-z0-9+/]+={0,2}$/,
            `Invalid registry integrity for ${spec}`
        );
        const packResult = await runner("npm", [
            "pack",
            spec,
            "--json",
            "--pack-destination",
            tarballDirectory,
            "--registry",
            options.registry,
        ]);
        const packed = parseJsonOutput<{ filename: string; integrity?: string }[]>(
            packResult,
            `Downloading ${spec}`
        );
        assert.equal(packed.length, 1, `npm pack returned an unexpected result for ${spec}`);
        const tarball = join(tarballDirectory, basename(packed[0].filename));
        validateFile(tarball, `${spec} tarball`);
        assert.equal(sha512Integrity(tarball), registryIntegrity, `Integrity mismatch for ${spec}`);
        if (packed[0].integrity) {
            assert.equal(
                packed[0].integrity,
                registryIntegrity,
                `npm pack integrity mismatch for ${spec}`
            );
        }

        const extractionRoot = join(outputDirectory, `.extract-${platform}`);
        const packageRoot = join(extractionRoot, "package");
        rmSync(extractionRoot, { recursive: true, force: true });
        mkdirSync(extractionRoot, { recursive: true });
        try {
            await extractTar({ cwd: extractionRoot, file: tarball, strict: true });
            validateRuntimePackageRoot(
                packageRoot,
                platform,
                options.runtimeVersion,
                options.runtimeSha
            );
            const destination = join(outputDirectory, platform);
            rmSync(destination, { recursive: true, force: true });
            renameSync(packageRoot, destination);
        } finally {
            rmSync(extractionRoot, { recursive: true, force: true });
        }
        acquired.push({
            filename: basename(tarball),
            integrity: registryIntegrity,
            name: packageName,
            platform,
            version: options.runtimeVersion,
        });
    }

    assert.equal(acquired.length, 8);
    writeFileSync(
        join(outputDirectory, "runtime-packages.json"),
        `${JSON.stringify(
            {
                runtimeVersion: options.runtimeVersion,
                runtimeSha: options.runtimeSha,
                registry: options.registry,
                packages: acquired,
            },
            null,
            2
        )}\n`
    );
}

function parseArguments(args: string[]): AcquireRuntimePackagesOptions {
    const values = new Map<string, string>();
    for (let index = 0; index < args.length; index += 2) {
        const key = args[index];
        const value = args[index + 1];
        if (!key?.startsWith("--") || !value) {
            throw new Error(
                "Usage: runtime-package-acquisition.ts --version <version> --sha <sha> --registry <url> --output <directory>"
            );
        }
        values.set(key, value);
    }
    return {
        runtimeVersion: values.get("--version") ?? "",
        runtimeSha: values.get("--sha") ?? "",
        registry: values.get("--registry") ?? "",
        outputDirectory: values.get("--output") ?? "",
    };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
    acquireRuntimePackages(parseArguments(process.argv.slice(2))).catch((error) => {
        console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    });
}
