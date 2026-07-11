#!/usr/bin/env node
/**
 * Outputs the next or current version of the SDK package based on the latest
 * published version and provided version increment type.
 *
 * Usage:
 *
 *     node scripts/get-version.js [current|current-prerelease|latest|prerelease|unstable]
 *
 * Outputs the version to stdout.
 */
import { execSync } from "child_process";
import * as semver from "semver";
import { calculateVersion } from "./calculate-version.js";

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

const command = process.argv[2];
const latest = await getLatestVersion("latest");
const prerelease = await getLatestVersion("prerelease");
const unstable = command === "unstable" ? await getLatestVersion("unstable") : undefined;
console.log(calculateVersion(command, { latest, prerelease, unstable }));
