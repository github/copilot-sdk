#!/usr/bin/env node
/**
 * Outputs the next or current version of the SDK package based on the latest
 * published version and provided version increment type.
 *
 * Usage:
 *
 *     node scripts/get-version.js [current|current-prerelease|latest|prerelease]
 *
 * Outputs the version to stdout.
 */
import { execSync } from "child_process";
import * as semver from "semver";

async function getLatestVersion(tag) {
    try {
        const result = execSync(
            `npm view @github/copilot-sdk@${tag} version --registry=https://registry.npmjs.org`,
            { encoding: "utf-8", stdio: ["pipe", "pipe", "pipe"] }
        );
        const version = result.trim();
        if (!semver.valid(version)) {
            console.error(`Invalid version returned from npm for tag "${tag}": "${version}"`);
            process.exit(1);
        }
        return version;
    } catch {
        // Tag doesn't exist yet
        return null;
    }
}

async function main() {
    const command = process.argv[2];
    const validCommands = ["current", "current-prerelease", "latest", "prerelease"];
    if (!validCommands.includes(command)) {
        console.error(
            `Invalid argument, must be one of: ${validCommands.join(", ")}, got: "${command}"`
        );
        process.exit(1);
    }

    const latest = await getLatestVersion("latest");
    if (!latest) {
        console.error("No latest version found. Publish an initial version first.");
        process.exit(1);
    }

    // Output the current latest version to stdout
    if (command === "current") {
        console.log(latest);
        return;
    }

    const prerelease = await getLatestVersion("prerelease");

    // Use latest if no prerelease exists, or compare to find higher
    let higherVersion;
    if (!prerelease) {
        higherVersion = latest;
    } else {
        try {
            higherVersion = semver.gt(latest, prerelease) ? latest : prerelease;
        } catch (err) {
            console.error(
                `Failed to compare versions "${latest}" and "${prerelease}": ${err.message}`
            );
            process.exit(1);
        }
    }

    // Output the most recent version including prerelease versions to stdout
    if (command === "current-prerelease") {
        console.log(higherVersion);
        return;
    }

    const increment = command === "latest" ? "patch" : "prerelease";
    const prereleaseIdentifier = command === "prerelease" ? "preview" : undefined;
    const nextVersion = semver.inc(higherVersion, increment, prereleaseIdentifier);
    if (!nextVersion) {
        console.error(`Failed to increment version "${higherVersion}" with "${increment}"`);
        process.exit(1);
    }

    // Output the next version to stdout
    console.log(nextVersion);
}

void main();
