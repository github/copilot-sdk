import * as semver from "semver";

const validCommands = ["current", "current-prerelease", "latest", "prerelease", "unstable"];

export function calculateVersion(command, { latest, prerelease, unstable }) {
    if (!validCommands.includes(command)) {
        throw new Error(
            `Invalid argument, must be one of: ${validCommands.join(", ")}, got: "${command}"`
        );
    }

    if (!latest) {
        throw new Error("No latest version found. Publish an initial version first.");
    }

    // Output the current latest version to stdout
    if (command === "current") {
        return latest;
    }

    // Use latest if no prerelease exists, or compare to find higher
    let higherVersion;
    if (!prerelease) {
        higherVersion = latest;
    } else {
        try {
            higherVersion = semver.gt(latest, prerelease) ? latest : prerelease;
        } catch (err) {
            throw new Error(
                `Failed to compare versions "${latest}" and "${prerelease}": ${err.message}`
            );
        }
    }

    // Output the most recent version including prerelease versions to stdout
    if (command === "current-prerelease") {
        return higherVersion;
    }

    if (command === "unstable") {
        if (unstable && semver.gt(unstable, higherVersion)) {
            higherVersion = unstable;
        }
    }

    const increment = command === "latest" ? "patch" : "prerelease";
    const isIncrementingExistingPrerelease = semver.prerelease(higherVersion) !== null;
    const prereleaseIdentifier =
        command === "prerelease"
            ? isIncrementingExistingPrerelease
                ? undefined
                : "preview"
            : command === "unstable"
              ? "unstable"
              : undefined;
    const nextVersion = semver.inc(higherVersion, increment, prereleaseIdentifier);
    if (!nextVersion) {
        throw new Error(`Failed to increment version "${higherVersion}" with "${increment}"`);
    }

    return nextVersion;
}
