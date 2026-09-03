import { execFileSync } from "node:child_process";
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { COPILOT_CLI_VERSION } from "../src/cliVersion.js";
import {
    getRuntimePackageName,
    materializeRuntimeBundle,
    RUNTIME_PLATFORMS,
} from "../src/runtimeArtifacts.js";
import { ensureCopilotPackage } from "./releaseArtifacts.js";

const nodeRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
const packagePath = join(nodeRoot, "package.json");
const originalPackage = readFileSync(packagePath, "utf8");
const packageJson = JSON.parse(originalPackage);
const sdkVersion = packageJson.version;
const npmCliPath = process.env.npm_execpath;
if (!npmCliPath) {
    throw new Error("package-sdk.ts must be run through an npm script");
}
const requestedPlatforms = process.env.COPILOT_SDK_RUNTIME_PLATFORMS?.split(",").filter(Boolean);
const platforms = requestedPlatforms ?? [...RUNTIME_PLATFORMS];
const stagingRoot = mkdtempSync(join(tmpdir(), "copilot-sdk-platform-packages-"));

try {
    const optionalDependencies: Record<string, string> = {};
    for (const platform of platforms) {
        if (!(RUNTIME_PLATFORMS as readonly string[]).includes(platform)) {
            throw new Error(`Unsupported runtime platform: ${platform}`);
        }
        const releasePackage = await ensureCopilotPackage(COPILOT_CLI_VERSION, { platform });
        const runtimeWrapper = materializeRuntimeBundle(
            { packageRoot: releasePackage, platform },
            stagingRoot,
            platform
        );
        const runtimeRoot = resolve(dirname(runtimeWrapper), "..", "..");
        const packageName = getRuntimePackageName(platform);
        const [osName, cpu] = platform.replace("linuxmusl", "linux").split("-");
        const runtimePackage = {
            name: packageName,
            version: sdkVersion,
            description: `Platform runtime for @github/copilot-sdk (${platform})`,
            license: "MIT",
            os: [osName],
            cpu: [cpu],
            ...(platform.startsWith("linux")
                ? { libc: [platform.startsWith("linuxmusl") ? "musl" : "glibc"] }
                : {}),
        };
        writeFileSync(
            join(runtimeRoot, "package.json"),
            `${JSON.stringify(runtimePackage, null, 4)}\n`
        );
        execFileSync(process.execPath, [npmCliPath, "pack", runtimeRoot, "--pack-destination", nodeRoot], {
            stdio: "inherit",
        });
        optionalDependencies[packageName] = sdkVersion;
    }

    packageJson.optionalDependencies = optionalDependencies;
    writeFileSync(packagePath, `${JSON.stringify(packageJson, null, 4)}\n`);
    execFileSync(
        process.execPath,
        [npmCliPath, "pack", nodeRoot, "--pack-destination", nodeRoot],
        { stdio: "inherit" }
    );
} finally {
    writeFileSync(packagePath, originalPackage);
    rmSync(stagingRoot, { recursive: true, force: true });
}
