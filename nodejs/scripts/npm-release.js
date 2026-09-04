import { createHash } from "node:crypto";
import { spawn } from "node:child_process";
import { readFileSync } from "node:fs";
import { basename, dirname, resolve } from "node:path";
import { pathToFileURL } from "node:url";

export function runCommand(command, args, { stream = false } = {}) {
    return new Promise((resolveResult, reject) => {
        const child = spawn(command, args, { shell: false });
        let stdout = "";
        let stderr = "";

        child.stdout.on("data", (chunk) => {
            stdout += chunk;
            if (stream) process.stdout.write(chunk);
        });
        child.stderr.on("data", (chunk) => {
            stderr += chunk;
            if (stream) process.stderr.write(chunk);
        });
        child.on("error", reject);
        child.on("close", (status) => resolveResult({ status: status ?? 1, stdout, stderr }));
    });
}

function parseNpmJson(result) {
    for (const output of [result.stdout, result.stderr]) {
        try {
            return JSON.parse(output);
        } catch {
            // The caller reports the complete npm output if neither stream is JSON.
        }
    }
    return undefined;
}

export async function getRegistryIntegrity(packageName, version, registry, runner = runCommand) {
    const result = await runner("npm", [
        "view",
        `${packageName}@${version}`,
        "dist.integrity",
        "--json",
        "--registry",
        registry,
    ]);
    const parsed = parseNpmJson(result);
    if (result.status === 0 && typeof parsed === "string") {
        return parsed;
    }
    if (result.status !== 0 && parsed?.error?.code === "E404") {
        return undefined;
    }
    const output = `${result.stdout}\n${result.stderr}`.trim();
    throw new Error(
        `Could not read ${packageName}@${version} integrity from ${registry} (npm exited ${result.status}).${output ? `\n${output}` : ""}`
    );
}

export async function getRegistryTagVersion(packageName, tag, registry, runner = runCommand) {
    const result = await runner("npm", [
        "view",
        `${packageName}@${tag}`,
        "version",
        "--json",
        "--registry",
        registry,
    ]);
    const parsed = parseNpmJson(result);
    if (result.status === 0 && typeof parsed === "string") {
        return parsed;
    }
    if (result.status !== 0 && parsed?.error?.code === "E404") {
        return undefined;
    }
    const output = `${result.stdout}\n${result.stderr}`.trim();
    throw new Error(
        `Could not read ${packageName}@${tag} from ${registry} (npm exited ${result.status}).${output ? `\n${output}` : ""}`
    );
}

export async function assertVersionAbsent(packageName, version, registry, runner = runCommand) {
    const existing = await getRegistryIntegrity(packageName, version, registry, runner);
    if (existing !== undefined) {
        throw new Error(`${packageName}@${version} already exists on ${registry}.`);
    }
}

export async function assertPublishedIntegrity(
    packageName,
    version,
    expectedIntegrity,
    registry,
    runner = runCommand
) {
    const existing = await getRegistryIntegrity(packageName, version, registry, runner);
    if (existing === undefined) {
        return "missing";
    }
    if (existing !== expectedIntegrity) {
        throw new Error(
            `${packageName}@${version} on ${registry} has integrity ${existing}, expected ${expectedIntegrity}.`
        );
    }
    return "matching";
}

export async function publishTarball(tarball, tag, registry, mode, identity, runner = runCommand) {
    if (!identity?.name || !identity?.version || !identity?.integrity) {
        throw new Error("Publishing requires an expected package name, version, and integrity.");
    }
    const args = ["publish", tarball, "--tag", tag, "--registry", registry];
    if (mode === "public") args.push("--access", "public");
    if (mode !== "public" && mode !== "azure") throw new Error(`Unknown publish mode: ${mode}`);

    const result = await runner("npm", args, { stream: true });
    if (result.status !== 0) {
        const state = await assertPublishedIntegrity(
            identity.name,
            identity.version,
            identity.integrity,
            registry,
            runner
        );
        if (state !== "matching") {
            throw new Error(`npm publish failed with exit code ${result.status}.`);
        }
        console.log(`${identity.name}@${identity.version} already exists with matching integrity.`);
        return;
    }
    const state = await assertPublishedIntegrity(
        identity.name,
        identity.version,
        identity.integrity,
        registry,
        runner
    );
    if (state !== "matching") {
        throw new Error(
            `${identity.name}@${identity.version} was not readable with matching integrity after publication.`
        );
    }
}

function readReleaseManifest(manifestPath, packageDirectory) {
    const manifest = JSON.parse(readFileSync(manifestPath, "utf8"));
    if (manifest.schemaVersion !== 1 || !Array.isArray(manifest.packages)) {
        throw new Error("Unsupported release manifest.");
    }
    if (manifest.packages.length !== 9) {
        throw new Error(`Expected nine release packages, found ${manifest.packages.length}.`);
    }
    const expectedNames = new Set([
        "@github/copilot-sdk",
        "@github/copilot-sdk-darwin-arm64",
        "@github/copilot-sdk-darwin-x64",
        "@github/copilot-sdk-linux-arm64",
        "@github/copilot-sdk-linux-x64",
        "@github/copilot-sdk-linuxmusl-arm64",
        "@github/copilot-sdk-linuxmusl-x64",
        "@github/copilot-sdk-win32-arm64",
        "@github/copilot-sdk-win32-x64",
    ]);
    const names = new Set();
    for (const packed of manifest.packages) {
        if (
            typeof packed.name !== "string" ||
            typeof packed.filename !== "string" ||
            typeof packed.integrity !== "string" ||
            typeof packed.size !== "number"
        ) {
            throw new Error("Release manifest contains an invalid package entry.");
        }
        if (names.has(packed.name)) {
            throw new Error(`Duplicate package in release manifest: ${packed.name}`);
        }
        if (!expectedNames.has(packed.name)) {
            throw new Error(`Unexpected package in release manifest: ${packed.name}`);
        }
        names.add(packed.name);
        const tarball = resolve(packageDirectory, packed.filename);
        if (
            dirname(tarball) !== resolve(packageDirectory) ||
            basename(tarball) !== packed.filename
        ) {
            throw new Error(`Unsafe release package filename: ${packed.filename}`);
        }
        const bytes = readFileSync(tarball);
        const localIntegrity = `sha512-${createHash("sha512").update(bytes).digest("base64")}`;
        if (bytes.length !== packed.size || localIntegrity !== packed.integrity) {
            throw new Error(`Local release package does not match manifest: ${packed.filename}`);
        }
    }
    if (names.size !== expectedNames.size) {
        throw new Error("Release manifest does not contain the exact Node SDK package set.");
    }
    return manifest;
}

export async function publishManifest(
    manifestPath,
    packageDirectory,
    tag,
    registry,
    mode,
    runner = runCommand
) {
    const manifest = readReleaseManifest(manifestPath, packageDirectory);
    const packages = manifest.packages
        .map((packed) => ({
            ...packed,
            version: manifest.sdk.version,
            tarball: resolve(packageDirectory, packed.filename),
        }))
        .sort((left, right) => {
            if (left.name === "@github/copilot-sdk") return 1;
            if (right.name === "@github/copilot-sdk") return -1;
            return left.name.localeCompare(right.name);
        });

    const states = new Map();
    for (const packed of packages) {
        states.set(
            packed.name,
            await assertPublishedIntegrity(
                packed.name,
                packed.version,
                packed.integrity,
                registry,
                runner
            )
        );
    }
    const semver = await import("semver");
    for (const packed of packages) {
        const taggedVersion = await getRegistryTagVersion(packed.name, tag, registry, runner);
        if (taggedVersion !== undefined && semver.gt(taggedVersion, packed.version)) {
            throw new Error(
                `${packed.name}@${tag} already points to newer version ${taggedVersion}; refusing to rewind it to ${packed.version}.`
            );
        }
        if (
            mode === "public" &&
            states.get(packed.name) === "matching" &&
            taggedVersion !== packed.version
        ) {
            throw new Error(
                `${packed.name}@${tag} resolves to ${taggedVersion ?? "no version"}, expected ${packed.version}. Public trusted publishing cannot repair dist-tags.`
            );
        }
    }
    for (const packed of packages) {
        if (states.get(packed.name) === "missing") {
            await publishTarball(packed.tarball, tag, registry, mode, packed, runner);
        }
    }
    for (const packed of packages) {
        const taggedVersion = await getRegistryTagVersion(packed.name, tag, registry, runner);
        if (taggedVersion === packed.version) {
            continue;
        }
        if (mode === "public") {
            throw new Error(
                `${packed.name}@${tag} resolves to ${taggedVersion ?? "no version"}, expected ${packed.version}. Public trusted publishing cannot repair dist-tags.`
            );
        }
        if (taggedVersion !== undefined && semver.gt(taggedVersion, packed.version)) {
            throw new Error(
                `${packed.name}@${tag} advanced to newer version ${taggedVersion}; refusing to rewind it to ${packed.version}.`
            );
        }
        const result = await runner(
            "npm",
            ["dist-tag", "add", `${packed.name}@${packed.version}`, tag, "--registry", registry],
            { stream: true }
        );
        if (result.status !== 0) {
            throw new Error(`Failed to set ${packed.name}@${packed.version} dist-tag ${tag}.`);
        }
    }
}

async function main() {
    const [command, ...args] = process.argv.slice(2);
    if (command === "preflight" && args.length === 3) {
        await assertVersionAbsent(...args);
        console.log(`${args[0]}@${args[1]} is available on ${args[2]}.`);
    } else if (command === "publish" && args.length === 7) {
        const [tarball, name, version, tag, registry, mode, expectedIntegrity] = args;
        const localIntegrity = `sha512-${createHash("sha512")
            .update(readFileSync(tarball))
            .digest("base64")}`;
        if (expectedIntegrity !== localIntegrity) {
            throw new Error(`Expected integrity does not match ${tarball}.`);
        }
        await publishTarball(tarball, tag, registry, mode, {
            name,
            version,
            integrity: localIntegrity,
        });
    } else if (command === "publish-manifest" && args.length === 5) {
        await publishManifest(...args);
    } else {
        throw new Error(
            "Usage: npm-release.js preflight <package> <version> <registry> | publish <tarball> <name> <version> <tag> <registry> <public|azure> <sha512-integrity> | publish-manifest <manifest> <package-directory> <tag> <registry> <public|azure>"
        );
    }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    main().catch((error) => {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    });
}
