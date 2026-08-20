import { createHash } from "node:crypto";
import {
    chmodSync,
    copyFileSync,
    existsSync,
    mkdirSync,
    mkdtempSync,
    renameSync,
    rmSync,
    statSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { basename, join } from "node:path";

export interface RuntimeArtifactSources {
    wrapper: string;
    runtimeNode: string;
    cli: string;
    platform: string;
}

function validateFile(path: string, label: string): void {
    if (!existsSync(path)) {
        throw new Error(`${label} not found at ${path}.`);
    }
    if (statSync(path).size === 0) {
        throw new Error(`${label} at ${path} is empty.`);
    }
}

function validateRuntimeBundle(wrapper: string, runtimeNode: string, cli: string): void {
    validateFile(wrapper, "Copilot runtime wrapper");
    validateFile(runtimeNode, "Copilot runtime.node");
    validateFile(cli, "Copilot CLI host");
}

function sourceFingerprint(sources: RuntimeArtifactSources): string {
    const hash = createHash("sha256");
    for (const path of [sources.wrapper, sources.runtimeNode, sources.cli]) {
        const stat = statSync(path);
        hash.update(path).update("\0");
        hash.update(`${stat.size}:${stat.mtimeMs}`).update("\0");
    }
    return hash.digest("hex").slice(0, 20);
}

function makeExecutable(path: string): void {
    if (process.platform === "win32") {
        return;
    }
    const mode = statSync(path).mode;
    if ((mode & 0o111) === 0) {
        chmodSync(path, mode | 0o111);
    }
}

export function materializeRuntimeBundle(
    sources: RuntimeArtifactSources,
    cacheRoot = join(tmpdir(), "github-copilot-sdk", "runtime")
): string {
    validateRuntimeBundle(sources.wrapper, sources.runtimeNode, sources.cli);

    const installDir = join(cacheRoot, `${sources.platform}-${sourceFingerprint(sources)}`);
    const installedWrapper = join(installDir, basename(sources.wrapper));
    const installedRuntimeNode = join(installDir, "runtime.node");
    const installedCli = join(installDir, basename(sources.cli));
    if (existsSync(installDir)) {
        validateRuntimeBundle(installedWrapper, installedRuntimeNode, installedCli);
        makeExecutable(installedWrapper);
        makeExecutable(installedCli);
        return installedWrapper;
    }

    mkdirSync(cacheRoot, { recursive: true });
    const stagingDir = mkdtempSync(join(cacheRoot, ".runtime-"));
    try {
        const stagedWrapper = join(stagingDir, basename(sources.wrapper));
        const stagedRuntimeNode = join(stagingDir, "runtime.node");
        const stagedCli = join(stagingDir, basename(sources.cli));
        copyFileSync(sources.wrapper, stagedWrapper);
        copyFileSync(sources.runtimeNode, stagedRuntimeNode);
        copyFileSync(sources.cli, stagedCli);
        makeExecutable(stagedWrapper);
        makeExecutable(stagedCli);
        renameSync(stagingDir, installDir);
    } catch (error) {
        if (!existsSync(installDir)) {
            throw error;
        }
        validateRuntimeBundle(installedWrapper, installedRuntimeNode, installedCli);
    } finally {
        rmSync(stagingDir, { recursive: true, force: true });
    }

    return installedWrapper;
}
