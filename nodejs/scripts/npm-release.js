import { spawn } from "node:child_process";
import { pathToFileURL } from "node:url";

const PUBLIC_CONFLICT =
    /^(?:npm (?:error|ERR!) code EPUBLISHCONFLICT|npm (?:error|ERR!) (?:403 [^\r\n]* - )?(?:You )?cannot publish over (?:the )?previously published versions(?:: [^\r\n]+)?\.?)\r?$/im;
const AZURE_CONFLICT =
    /^npm (?:error|ERR!) (?:403 [^\r\n]* - )?(?:The feed '[^'\r\n]+' )?already contains file '[^'\r\n]+\.tgz' in package '[^'\r\n]+'\.?\r?$/im;

export function runCommand(command, args, { stream = false } = {}) {
    return new Promise((resolve, reject) => {
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
        child.on("close", (status) => resolve({ status: status ?? 1, stdout, stderr }));
    });
}

export async function assertVersionAbsent(packageName, version, registry, runner = runCommand) {
    const result = await runner("npm", [
        "view",
        `${packageName}@${version}`,
        "version",
        "--json",
        "--registry",
        registry,
    ]);

    if (result.status === 0) {
        throw new Error(`${packageName}@${version} already exists on public npm.`);
    }

    try {
        if (JSON.parse(result.stdout)?.error?.code === "E404") return;
    } catch {
        // The failure below includes npm's output for diagnosis.
    }

    const output = `${result.stdout}\n${result.stderr}`.trim();
    throw new Error(
        `Could not confirm that ${packageName}@${version} is absent from public npm (npm exited ${result.status}).${output ? `\n${output}` : ""}`
    );
}

export async function publishTarball(tarball, tag, registry, mode, runner = runCommand) {
    const args = ["publish", tarball, "--tag", tag, "--registry", registry];
    if (mode === "public") args.push("--access", "public");
    if (mode !== "public" && mode !== "azure") throw new Error(`Unknown publish mode: ${mode}`);

    const result = await runner("npm", args, { stream: true });
    if (result.status === 0) return;

    const output = `${result.stdout}\n${result.stderr}`;
    if (PUBLIC_CONFLICT.test(output) || (mode === "azure" && AZURE_CONFLICT.test(output))) {
        console.log(
            "Version already published; treating the immutable-version conflict as success."
        );
        return;
    }

    throw new Error(`npm publish failed with exit code ${result.status}.`);
}

async function main() {
    const [command, ...args] = process.argv.slice(2);
    if (command === "preflight" && args.length === 3) {
        await assertVersionAbsent(...args);
        console.log(`${args[0]}@${args[1]} is available on public npm.`);
    } else if (command === "publish" && args.length === 4) {
        await publishTarball(...args);
    } else {
        throw new Error(
            "Usage: npm-release.js preflight <package> <version> <registry> | publish <tarball> <tag> <registry> <public|azure>"
        );
    }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    main().catch((error) => {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    });
}
