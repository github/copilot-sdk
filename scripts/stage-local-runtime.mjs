#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const runtimeWorktree = process.env.COPILOT_RUNTIME_WORKTREE;
if (!runtimeWorktree) {
    throw new Error("COPILOT_RUNTIME_WORKTREE must point to a copilot-agent-runtime worktree.");
}

const platform = process.env.COPILOT_RUNTIME_PLATFORM ?? process.platform;
const arch = process.env.COPILOT_RUNTIME_ARCH ?? process.arch;
const libc =
    process.env.COPILOT_RUNTIME_LIBC ??
    (platform === "linux" && !process.report?.getReport()?.header?.glibcVersionRuntime ? "musl" : "gnu");
const target = resolveTarget(platform, arch, libc);
const sourceDir = path.join(runtimeWorktree, "src", "native", "runtime");
const outputDir = path.resolve(process.env.COPILOT_RUNTIME_STAGE_DIR ?? ".local-runtime", target.prebuilds);
const wrapperName = platform === "win32" ? "copilot-runtime.exe" : "copilot-runtime";
const sourceWrapper = path.join(sourceDir, `copilot-runtime.${target.triple}${platform === "win32" ? ".exe" : ""}`);
const sourceRuntime = path.join(sourceDir, `runtime.${target.triple}.node`);

requireArtifact(sourceWrapper, "runtime wrapper");
requireArtifact(sourceRuntime, "runtime.node");

fs.mkdirSync(outputDir, { recursive: true });
const wrapper = path.join(outputDir, wrapperName);
const runtime = path.join(outputDir, "runtime.node");
copyAtomically(sourceWrapper, wrapper);
copyAtomically(sourceRuntime, runtime);
if (platform !== "win32") {
    fs.chmodSync(wrapper, 0o755);
}

process.stdout.write(`${wrapper}\n`);

function resolveTarget(targetPlatform, targetArch, targetLibc) {
    const key = `${targetPlatform}-${targetArch}-${targetLibc}`;
    const targets = {
        "win32-x64-gnu": { triple: "win32-x64-msvc", prebuilds: "win32-x64" },
        "win32-arm64-gnu": { triple: "win32-arm64-msvc", prebuilds: "win32-arm64" },
        "darwin-x64-gnu": { triple: "darwin-x64", prebuilds: "darwin-x64" },
        "darwin-arm64-gnu": { triple: "darwin-arm64", prebuilds: "darwin-arm64" },
        "linux-x64-gnu": { triple: "linux-x64-gnu", prebuilds: "linux-x64" },
        "linux-arm64-gnu": { triple: "linux-arm64-gnu", prebuilds: "linux-arm64" },
        "linux-x64-musl": { triple: "linux-x64-musl", prebuilds: "linuxmusl-x64" },
        "linux-arm64-musl": { triple: "linux-arm64-musl", prebuilds: "linuxmusl-arm64" },
    };
    const target = targets[key];
    if (!target) {
        throw new Error(`Unsupported runtime target: ${targetPlatform}/${targetArch}/${targetLibc}`);
    }
    return target;
}

function requireArtifact(file, label) {
    let stat;
    try {
        stat = fs.statSync(file);
    } catch {
        throw new Error(`Local ${label} was not produced at ${file}. Run pnpm run build:runtime in the runtime worktree.`);
    }
    if (!stat.isFile() || stat.size === 0) {
        throw new Error(`Local ${label} is not a non-empty file: ${file}`);
    }
}

function copyAtomically(source, destination) {
    const temporary = `${destination}.${process.pid}.tmp`;
    try {
        fs.copyFileSync(source, temporary);
        fs.renameSync(temporary, destination);
    } finally {
        fs.rmSync(temporary, { force: true });
    }
}
