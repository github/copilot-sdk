import { createHash } from "node:crypto";
import { copyFileSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { basename, join } from "node:path";
import { c as createTar } from "tar";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
    acquireRuntimePackages,
    getSourceRuntimePackageName,
    validateRuntimePackageRoot,
} from "../scripts/runtime-package-acquisition.js";
import { RUNTIME_PLATFORMS } from "../src/runtimeArtifacts.js";

const roots: string[] = [];
const runtimeVersion = "1.0.83-5.unstable.123.gabcdef0";
const runtimeSha = "abcdef0123456789abcdef0123456789abcdef01";

function temporaryRoot(prefix: string): string {
    const root = mkdtempSync(join(tmpdir(), prefix));
    roots.push(root);
    return root;
}

afterEach(() => {
    for (const root of roots.splice(0)) {
        rmSync(root, { recursive: true, force: true });
    }
});

async function createRuntimePackage(root: string, platform: string): Promise<string> {
    const packageRoot = join(root, platform, "package");
    const windows = platform.startsWith("win32");
    const [osName, cpu] = platform.replace("linuxmusl", "linux").split("-");
    mkdirSync(join(packageRoot, "prebuilds", platform), { recursive: true });
    mkdirSync(join(packageRoot, "copilot-sdk"), { recursive: true });
    mkdirSync(join(packageRoot, "preloads"), { recursive: true });
    mkdirSync(join(packageRoot, "sdk"), { recursive: true });
    writeFileSync(
        join(packageRoot, "package.json"),
        JSON.stringify({
            name: getSourceRuntimePackageName(platform),
            version: runtimeVersion,
            repository: "https://github.com/github/copilot-agent-runtime.git",
            os: [osName],
            cpu: [cpu],
            ...(platform.startsWith("linux")
                ? { libc: [platform.startsWith("linuxmusl") ? "musl" : "glibc"] }
                : {}),
            copilotRuntime: {
                sourceRepository: "github/copilot-agent-runtime",
                sourceSha: runtimeSha,
            },
        })
    );
    for (const path of [
        "LICENSE.md",
        windows ? "copilot.exe" : "copilot",
        join("prebuilds", platform, windows ? "copilot-runtime.exe" : "copilot-runtime"),
        join("prebuilds", platform, "runtime.node"),
        join("copilot-sdk", "extension.js"),
        join("preloads", "extension_bootstrap.mjs"),
        join("sdk", "index.js"),
    ]) {
        writeFileSync(join(packageRoot, path), path);
    }
    const archive = join(root, `${platform}.tgz`);
    await createTar({ cwd: join(root, platform), file: archive, gzip: true }, ["package"]);
    return archive;
}

describe("runtime npm package acquisition", () => {
    it("downloads and validates all eight exact runtime platform packages", async () => {
        const root = temporaryRoot("copilot-runtime-acquisition-");
        const output = join(root, "output");
        const archives = new Map<string, { integrity: string; path: string }>();
        for (const platform of RUNTIME_PLATFORMS) {
            const path = await createRuntimePackage(root, platform);
            archives.set(platform, {
                path,
                integrity: `sha512-${createHash("sha512")
                    .update(readFileSync(path))
                    .digest("base64")}`,
            });
        }
        const runner = vi.fn(async (_command: string, args: string[]) => {
            const spec = args[1];
            const platform = RUNTIME_PLATFORMS.find((candidate) =>
                spec.startsWith(`${getSourceRuntimePackageName(candidate)}@`)
            );
            expect(platform).toBeDefined();
            const archive = archives.get(platform!)!;
            if (args[0] === "view") {
                return { status: 0, stdout: JSON.stringify(archive.integrity), stderr: "" };
            }
            const destination = args[args.indexOf("--pack-destination") + 1];
            const filename = basename(archive.path);
            mkdirSync(destination, { recursive: true });
            copyFileSync(archive.path, join(destination, filename));
            return {
                status: 0,
                stdout: JSON.stringify([{ filename, integrity: archive.integrity }]),
                stderr: "",
            };
        });

        await acquireRuntimePackages(
            {
                outputDirectory: output,
                registry: "https://npm.pkg.github.com",
                runtimeSha,
                runtimeVersion,
            },
            runner
        );

        expect(runner).toHaveBeenCalledTimes(16);
        const acquisition = JSON.parse(readFileSync(join(output, "runtime-packages.json"), "utf8"));
        expect(acquisition.packages).toHaveLength(8);
        for (const platform of RUNTIME_PLATFORMS) {
            validateRuntimePackageRoot(
                join(output, platform),
                platform,
                runtimeVersion,
                runtimeSha
            );
        }
    });

    it("rejects mismatched source identity metadata", async () => {
        const root = temporaryRoot("copilot-runtime-identity-");
        await createRuntimePackage(root, "linux-x64");
        const packageRoot = join(root, "linux-x64", "package");
        const manifestPath = join(packageRoot, "package.json");
        const manifest = JSON.parse(readFileSync(manifestPath, "utf8"));
        manifest.copilotRuntime.sourceSha = "0".repeat(40);
        writeFileSync(manifestPath, JSON.stringify(manifest));

        expect(() =>
            validateRuntimePackageRoot(packageRoot, "linux-x64", runtimeVersion, runtimeSha)
        ).toThrow();
    });
});
