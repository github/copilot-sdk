#!/usr/bin/env node

import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { spawn } from "node:child_process";
import { pathToFileURL } from "node:url";

const PUBLIC_CONFLICT =
    /^(?:npm (?:error|ERR!) code EPUBLISHCONFLICT|npm (?:error|ERR!) (?:403 [^\r\n]* - )?(?:You )?cannot publish over (?:the )?previously published versions(?:: [^\r\n]+)?\.?)\r?$/im;
const AZURE_TARBALL_CONFLICT =
    /^npm (?:error|ERR!) (?:403 [^\r\n]* - )?already contains file '[^'\r\n]+\.tgz' in package '[^'\r\n]+'\.?\r?$/im;
const DEFAULT_RETRY_DELAYS_MS = [0, 1000, 2000, 4000, 8000];

export async function runCommand(command, args, { stream = false } = {}) {
    return await new Promise((resolve, reject) => {
        const child = spawn(command, args, { shell: false });
        let stdout = "";
        let stderr = "";

        child.stdout.on("data", (chunk) => {
            const text = chunk.toString();
            stdout += text;
            if (stream) {
                process.stdout.write(chunk);
            }
        });
        child.stderr.on("data", (chunk) => {
            const text = chunk.toString();
            stderr += text;
            if (stream) {
                process.stderr.write(chunk);
            }
        });
        child.on("error", reject);
        child.on("close", (status) => resolve({ status: status ?? 1, stdout, stderr }));
    });
}

export async function assertVersionAbsent({ packageName, version, registry, runner = runCommand }) {
    const result = await runner("npm", [
        "view",
        `${packageName}@${version}`,
        "version",
        "--json",
        "--registry",
        registry,
    ]);
    const output = `${result.stdout}\n${result.stderr}`;

    if (result.status === 0) {
        let publishedVersion;
        try {
            publishedVersion = JSON.parse(result.stdout);
        } catch {
            throw new Error(
                `Public npm returned malformed version metadata for ${packageName}@${version}.`
            );
        }
        if (publishedVersion !== version) {
            throw new Error(
                `Public npm returned unexpected version metadata for ${packageName}@${version}: ${result.stdout.trim()}`
            );
        }
        throw new Error(`${packageName}@${version} already exists on public npm.`);
    }

    try {
        const errorResult = JSON.parse(result.stdout);
        if (errorResult?.error?.code === "E404") {
            return;
        }
    } catch {
        // The failure below includes npm's output for diagnosis.
    }

    throw new Error(
        `Could not confirm that ${packageName}@${version} is absent from public npm (npm exited ${result.status}).\n${output.trim()}`
    );
}

export async function inspectTarball(tarball, runner = runCommand) {
    const result = await runner("tar", ["-xOf", tarball, "package/package.json"]);
    if (result.status !== 0) {
        throw new Error(
            `Could not read package/package.json from ${tarball}.\n${result.stderr.trim()}`
        );
    }

    let manifest;
    try {
        manifest = JSON.parse(result.stdout);
    } catch {
        throw new Error(`Tarball ${tarball} contains malformed package metadata.`);
    }
    if (typeof manifest.name !== "string" || typeof manifest.version !== "string") {
        throw new Error(`Tarball ${tarball} does not contain a valid package name and version.`);
    }

    const bytes = await readFile(tarball);
    const integrity = `sha512-${createHash("sha512").update(bytes).digest("base64")}`;
    return { name: manifest.name, version: manifest.version, integrity };
}

function parseIntegrity(stdout) {
    try {
        const integrity = JSON.parse(stdout);
        if (typeof integrity !== "string") {
            return null;
        }
        const match = /^sha512-([A-Za-z0-9+/]+={0,2})$/.exec(integrity);
        return match && Buffer.from(match[1], "base64").length === 64 ? integrity : null;
    } catch {
        return null;
    }
}

async function verifyPublishedIntegrity({
    packageName,
    version,
    localIntegrity,
    registry,
    runner,
    sleep,
    retryDelaysMs,
}) {
    let lastResult;
    for (const delay of retryDelaysMs) {
        if (delay > 0) {
            await sleep(delay);
        }
        lastResult = await runner("npm", [
            "view",
            `${packageName}@${version}`,
            "dist.integrity",
            "--json",
            "--registry",
            registry,
        ]);
        if (lastResult.status === 0) {
            const registryIntegrity = parseIntegrity(lastResult.stdout);
            if (registryIntegrity === localIntegrity) {
                return;
            }
            if (registryIntegrity && registryIntegrity !== localIntegrity) {
                throw new Error(
                    `Published integrity mismatch for ${packageName}@${version}: expected ${localIntegrity}, got ${registryIntegrity}.`
                );
            }
        }
    }

    const output = lastResult ? `${lastResult.stdout}\n${lastResult.stderr}`.trim() : "";
    throw new Error(
        `Could not verify published integrity for ${packageName}@${version} after ${retryDelaysMs.length} attempts.${output ? `\n${output}` : ""}`
    );
}

export async function publishTarball({
    tarball,
    registry,
    tag,
    access,
    azure = false,
    runner = runCommand,
    sleep = (delay) => new Promise((resolve) => setTimeout(resolve, delay)),
    retryDelaysMs = DEFAULT_RETRY_DELAYS_MS,
    inspect = inspectTarball,
}) {
    const packageInfo = await inspect(tarball, runner);
    const args = ["publish", tarball, "--tag", tag, "--registry", registry];
    if (access) {
        args.push("--access", access);
    }

    const result = await runner("npm", args, { stream: true });
    if (result.status === 0) {
        return { recoveredConflict: false };
    }

    const output = `${result.stdout}\n${result.stderr}`;
    const recognizedConflict =
        PUBLIC_CONFLICT.test(output) || (azure && AZURE_TARBALL_CONFLICT.test(output));
    if (!recognizedConflict) {
        throw new Error(`npm publish failed with exit code ${result.status}.`);
    }

    await verifyPublishedIntegrity({
        packageName: packageInfo.name,
        version: packageInfo.version,
        localIntegrity: packageInfo.integrity,
        registry,
        runner,
        sleep,
        retryDelaysMs,
    });
    console.log(
        `${packageInfo.name}@${packageInfo.version} already exists with matching integrity; treating the publish conflict as success.`
    );
    return { recoveredConflict: true };
}

function parseOptions(args) {
    const options = {};
    for (let index = 0; index < args.length; index += 2) {
        const key = args[index];
        const value = args[index + 1];
        if (!key?.startsWith("--") || value === undefined) {
            throw new Error(`Invalid argument: ${key ?? ""}`);
        }
        options[key.slice(2)] = value;
    }
    return options;
}

async function main() {
    const [command, ...args] = process.argv.slice(2);
    const options = parseOptions(args);

    if (command === "preflight") {
        await assertVersionAbsent({
            packageName: options.package,
            version: options.version,
            registry: options.registry,
        });
        console.log(`${options.package}@${options.version} is available on public npm.`);
        return;
    }

    if (command === "publish") {
        await publishTarball({
            tarball: options.tarball,
            registry: options.registry,
            tag: options.tag,
            access: options.access,
            azure: options.azure === "true",
        });
        return;
    }

    throw new Error(`Unknown command: ${command ?? ""}`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    main().catch((error) => {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    });
}
