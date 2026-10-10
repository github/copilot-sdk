/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawn, spawnSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const SDK_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
export const IMAGE = "node:22-bookworm";
export const POLICY_FILE = "/etc/github-copilot/managed-settings.json";
export const OWNER_LABEL = "com.github.copilot-sdk.device-policy";
export const CLIENT_LABEL = `${OWNER_LABEL}.client`;
export const REQUIRED_CASES = [
    {
        file: "managed_plugin_progress.e2e.test.ts",
        title: "emits presentation-neutral completion after installing required plugins",
    },
    {
        file: "rpc_server.e2e.test.ts",
        title: "should round trip sessionless managed settings",
    },
];

const escapeRegex = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
export const REQUIRED_PATTERN = REQUIRED_CASES.map(({ title }) => escapeRegex(title)).join("|");
export const PORTABLE_PATTERN = `^(?!.*(?:${REQUIRED_PATTERN})).*$`;

export function portableTestPattern(source) {
    if (source === "checkout") return "";
    if (source !== "published") throw new Error("Unknown runtime artifact source");
    return PORTABLE_PATTERN;
}

export function runtimeInvocation(kind, runtime, nodeExecutable, args, env) {
    if (!["native", "legacy"].includes(kind)) throw new Error("Unknown device-policy entry point");
    return {
        executable: kind === "native" ? runtime : nodeExecutable,
        argv: kind === "native" ? [runtime, ...args] : [nodeExecutable, runtime, ...args],
        env: { ...env },
    };
}

export function verifyRequiredResults(report) {
    for (const expected of REQUIRED_CASES) {
        const matches = (report.testResults ?? [])
            .filter((suite) => suite.name.replaceAll("\\", "/").endsWith(`/${expected.file}`))
            .flatMap((suite) => suite.assertionResults ?? [])
            .filter((test) => test.title === expected.title);
        if (matches.length !== 1 || matches[0].status !== "passed") {
            throw new Error(`Required device-policy control did not pass: ${expected.title}`);
        }
    }
}

function inside(parent, child) {
    const relative = path.posix.relative(parent, child);
    return relative === "" || (relative !== ".." && !relative.startsWith("../") && !path.posix.isAbsolute(relative));
}

function safeBind(directory) {
    if (!path.posix.isAbsolute(directory) || /[:,\r\n]/.test(directory)) {
        throw new Error("Device-policy binds require unambiguous absolute Linux paths");
    }
    if (
        ["/", "/etc", "/proc", "/sys", "/dev"].some((root) =>
            root === "/" ? directory === root : inside(root, directory),
        )
    ) {
        throw new Error("A device-policy container cannot bind host system or policy paths");
    }
    return directory;
}

export function fixtureDirectories({ cwd, env, temporaryDirectory }) {
    const candidates = [
        cwd,
        ...["COPILOT_HOME", "GH_CONFIG_DIR", "XDG_CONFIG_HOME", "XDG_STATE_HOME"]
            .map((name) => env[name])
            .filter(Boolean),
    ];
    const roots = new Set();
    for (const candidate of candidates) {
        const relative = path.posix.relative(temporaryDirectory, candidate);
        const first = relative.split("/")[0];
        if (!inside(temporaryDirectory, candidate) || !/^copilot-test-(work|home|config)-[^/]+$/.test(first)) {
            throw new Error("Writable device-policy binds must belong to an SDK E2E fixture");
        }
        roots.add(safeBind(path.posix.join(temporaryDirectory, first)));
    }
    return [...roots];
}

export function dockerArguments(plan, cidfile) {
    const args = [
        "run",
        "--rm",
        "--init",
        "--interactive",
        "--network",
        "host",
        "--cidfile",
        cidfile,
        "--label",
        `${OWNER_LABEL}=${plan.owner}`,
        "--label",
        `${CLIENT_LABEL}=${plan.client}`,
        "--workdir",
        plan.cwd,
        "--env",
        "COPILOT_CI_DEVICE_POLICY_PLAN",
    ];
    const mounts = new Map();
    for (const directory of plan.readonly) mounts.set(safeBind(directory), "ro");
    for (const directory of plan.writable) {
        safeBind(directory);
        if (mounts.has(directory)) throw new Error("A device-policy bind cannot be both read-only and writable");
        mounts.set(directory, "rw");
    }
    for (const [directory, mode] of mounts) args.push("--volume", `${directory}:${directory}:${mode}`);
    args.push(IMAGE, "node", path.posix.join(plan.sdkRoot, "scripts/ci/device-policy-bootstrap.mjs"));
    return args;
}

export function verifyIsolation({ originalNamespace, namespace, mountinfo, uid, gid }) {
    if (uid !== 0 || gid !== 0 || namespace === originalNamespace || !namespace || !originalNamespace) {
        throw new Error("Device policy must be provisioned inside a separate root container");
    }
    const mounts = mountinfo
        .trim()
        .split("\n")
        .map((line) => {
            const fields = line.split(" ");
            const separator = fields.indexOf("-");
            return { target: fields[4], type: fields[separator + 1], propagation: fields.slice(6, separator) };
        });
    const policyMount = mounts
        .filter(({ target }) => inside(target, POLICY_FILE))
        .sort((a, b) => b.target.length - a.target.length)[0];
    if (
        !policyMount ||
        policyMount.target !== "/" ||
        policyMount.type !== "overlay" ||
        policyMount.propagation.some((field) => /^(shared|master|propagate_from):/.test(field))
    ) {
        throw new Error("Device policy requires a private container root, not a host bind");
    }
}

export function verifyIdentity(plan, { uid, gid, groups, policy }) {
    if (
        uid !== plan.uid ||
        gid !== plan.gid ||
        JSON.stringify([...groups].sort((a, b) => a - b)) !== JSON.stringify([...plan.groups].sort((a, b) => a - b))
    ) {
        throw new Error("The device-policy runtime must retain the original runner identity");
    }
    if (
        !policy.isFile ||
        policy.isSymbolicLink ||
        policy.uid !== 0 ||
        policy.gid !== 0 ||
        (policy.mode & 0o777) !== 0o644
    ) {
        throw new Error("The device policy must be a regular root-owned mode-0644 file");
    }
}

function command(command, args, options = {}) {
    const result = spawnSync(command, args, { encoding: "utf8", ...options });
    if (result.error) throw result.error;
    if (result.status !== 0) throw new Error(`${command} failed (${result.status}): ${result.stderr ?? ""}`);
    return result.stdout;
}

export function containerId(value) {
    const id = value.trim();
    if (!/^[a-f0-9]{64}$/.test(id)) throw new Error("Invalid owned device-policy container ID");
    return id;
}

export function verifyOwnership(label, owner) {
    if (!owner || label.trim() !== owner) throw new Error("Refusing to mutate a container owned by another job");
}

export function pendingLaunches(entries) {
    for (const entry of entries) {
        if (!/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}\.(pending|cid)$/.test(entry))
            throw new Error("Unknown device-policy cleanup registry entry");
        const pending = entry.replace(/\.cid$/, ".pending");
        if (!entries.includes(pending)) throw new Error("Device-policy container identity has no launch receipt");
    }
    return entries.filter((entry) => entry.endsWith(".pending"));
}

function inspectOwned(id, owner) {
    const inspected = spawnSync("docker", ["inspect", "--format", `{{index .Config.Labels "${OWNER_LABEL}"}}`, id], {
        encoding: "utf8",
    });
    if (inspected.status !== 0 && /No such (object|container)/i.test(inspected.stderr)) return false;
    if (inspected.error || inspected.status !== 0)
        throw inspected.error ?? new Error(`Cannot inspect device-policy container: ${inspected.stderr}`);
    verifyOwnership(inspected.stdout, owner);
    return true;
}

function removeOwned(id, owner) {
    if (!inspectOwned(id, owner)) return;
    const removed = spawnSync("docker", ["rm", "--force", id], { encoding: "utf8" });
    if (removed.status !== 0 && /No such (object|container)/i.test(removed.stderr)) return;
    if (removed.error || removed.status !== 0)
        throw removed.error ?? new Error(`Cannot remove device-policy container: ${removed.stderr}`);
}

function stopOwned(id, owner, signal) {
    if (!inspectOwned(id, owner)) return;
    const stopped = spawnSync("docker", ["stop", "--signal", signal, "--timeout", "5", id], { encoding: "utf8" });
    if (stopped.status !== 0 && /No such (object|container)/i.test(stopped.stderr)) return;
    if (stopped.error || stopped.status !== 0)
        throw stopped.error ?? new Error(`Cannot stop device-policy container: ${stopped.stderr}`);
}

function ownedRegistry() {
    const registry = process.env.COPILOT_CI_DEVICE_POLICY_REGISTRY;
    if (!registry || !process.env.RUNNER_TEMP) throw new Error("Missing owned device-policy container registry");
    const info = fs.lstatSync(registry);
    if (
        !inside(process.env.RUNNER_TEMP, registry) ||
        !path.basename(registry).startsWith("sdk-device-policy-") ||
        fs.realpathSync(registry) !== registry ||
        !info.isDirectory() ||
        info.isSymbolicLink() ||
        info.uid !== process.getuid()
    ) {
        throw new Error("Device-policy cidfiles must belong to this runner's temporary registry");
    }
    return registry;
}

export async function launchRuntime(kind) {
    if (process.platform !== "linux" || typeof process.execve !== "function") {
        throw new Error("The device-policy launcher requires Linux and Node.js 22.15 or newer");
    }
    const runtime =
        process.env[
            kind === "native" ? "COPILOT_CI_DEVICE_POLICY_NATIVE_PATH" : "COPILOT_CI_DEVICE_POLICY_LEGACY_PATH"
        ];
    if (!runtime) throw new Error("Missing original device-policy runtime path");
    const invocation = runtimeInvocation(kind, runtime, process.execPath, process.argv.slice(2), process.env);
    const originalEnv = invocation.env;
    const fixture = process.env.COPILOT_TEST_MANAGED_SETTINGS_FILE_PATH;
    if (!fixture) {
        process.execve(invocation.executable, invocation.argv, originalEnv);
        throw new Error("Original runtime exec unexpectedly returned");
    }

    const fixtureInfo = fs.lstatSync(fixture);
    if (
        !inside(process.cwd(), fixture) ||
        fs.realpathSync(fixture) !== fixture ||
        !fixtureInfo.isFile() ||
        fixtureInfo.isSymbolicLink() ||
        fixtureInfo.uid !== process.getuid()
    )
        throw new Error("Device policy must be a regular file inside the test workspace");
    const policy = fs.readFileSync(fixture, "utf8");
    const parsed = JSON.parse(policy);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed))
        throw new Error("Device policy must be a JSON object");
    // The launcher consumes the test-only path; the runtime must discover the production file.
    delete originalEnv.COPILOT_TEST_MANAGED_SETTINGS_FILE_PATH;
    const owner = process.env.COPILOT_CI_DEVICE_POLICY_OWNER;
    if (!owner) throw new Error("Missing device-policy job ownership");
    const registry = ownedRegistry();
    const writable = fixtureDirectories({ cwd: process.cwd(), env: originalEnv, temporaryDirectory: os.tmpdir() });
    for (const directory of writable) {
        const info = fs.lstatSync(directory);
        if (fs.realpathSync(directory) !== directory || !info.isDirectory() || info.uid !== process.getuid())
            throw new Error("A writable fixture bind must be a real directory owned by the runner");
    }
    const readonly = [
        SDK_ROOT,
        path.dirname(process.env.COPILOT_CI_DEVICE_POLICY_LEGACY_PATH),
        path.dirname(process.execPath),
    ];
    for (const name of [
        "NODE_EXTRA_CA_CERTS",
        "SSL_CERT_FILE",
        "REQUESTS_CA_BUNDLE",
        "CURL_CA_BUNDLE",
        "GIT_SSL_CAINFO",
    ]) {
        const certificate = originalEnv[name];
        if (certificate) {
            if (!inside(os.tmpdir(), certificate) || !fs.statSync(certificate).isFile())
                throw new Error("Replay certificates must belong to the test temporary directory");
            readonly.push(certificate);
        }
    }
    const plan = {
        owner,
        client: randomUUID(),
        sdkRoot: SDK_ROOT,
        readonly: [...new Set(readonly)],
        writable,
        policy,
        ...invocation,
        cwd: process.cwd(),
        nodeExecutable: process.execPath,
        uid: process.getuid(),
        gid: process.getgid(),
        groups: process.getgroups(),
        home: os.homedir(),
        originalNamespace: fs.readlinkSync("/proc/self/ns/mnt"),
    };
    const cidfile = path.join(registry, `${plan.client}.cid`);
    const pending = path.join(registry, `${plan.client}.pending`);
    fs.writeFileSync(pending, "", { flag: "wx", mode: 0o600 });
    const child = spawn("docker", dockerArguments(plan, cidfile), {
        stdio: "inherit",
        env: { ...originalEnv, COPILOT_CI_DEVICE_POLICY_PLAN: JSON.stringify(plan) },
    });
    let stopping;
    const stop = (signal) => {
        if (stopping) return;
        stopping = signal;
        try {
            if (fs.existsSync(cidfile)) stopOwned(containerId(fs.readFileSync(cidfile, "utf8")), owner, signal);
            else child.kill(signal);
        } catch (error) {
            console.error(error.message);
            process.exitCode = 1;
            child.kill("SIGTERM");
        }
    };
    const onTerm = () => stop("SIGTERM");
    const onInt = () => stop("SIGINT");
    process.once("SIGTERM", onTerm);
    process.once("SIGINT", onInt);
    let result;
    try {
        result = await new Promise((resolve, reject) => {
            child.once("error", reject);
            child.once("close", (code, signal) => resolve({ code, signal }));
        });
    } finally {
        process.removeListener("SIGTERM", onTerm);
        process.removeListener("SIGINT", onInt);
        // A cancelled Docker attach can close before its cidfile is written.
        const ids = command("docker", [
            "ps",
            "--all",
            "--quiet",
            "--no-trunc",
            "--filter",
            `label=${OWNER_LABEL}=${owner}`,
            "--filter",
            `label=${CLIENT_LABEL}=${plan.client}`,
        ]).trim();
        for (const id of ids ? ids.split("\n") : []) removeOwned(containerId(id), owner);
        if (fs.existsSync(cidfile)) {
            removeOwned(containerId(fs.readFileSync(cidfile, "utf8")), owner);
            fs.unlinkSync(cidfile);
            fs.unlinkSync(pending);
        } else {
            throw new Error("Docker launch has no container identity; cleanup cannot prove settlement");
        }
    }
    if (process.exitCode) return;
    if (stopping || result.signal) process.kill(process.pid, stopping ?? result.signal);
    else process.exitCode = result.code ?? 1;
}

function prepare() {
    if (
        process.platform !== "linux" ||
        !process.env.GITHUB_ENV ||
        !process.env.GITHUB_OUTPUT ||
        !process.env.RUNNER_TEMP
    ) {
        throw new Error("Device-policy containers can only be prepared in Linux CI");
    }
    const native = fs.realpathSync(process.env.COPILOT_CLI_PATH);
    const legacy = fs.realpathSync(process.env.COPILOT_LEGACY_CLI_PATH);
    const packageRoot = path.dirname(legacy);
    if (
        native !== path.join(packageRoot, "prebuilds/linux-x64/copilot-runtime") ||
        path.basename(legacy) !== "app.js"
    ) {
        throw new Error("Device-policy fixtures require the staged GNU package's two original entry points");
    }
    const expected = JSON.parse(fs.readFileSync(path.join(SDK_ROOT, "nodejs/package.json"), "utf8")).copilotCliVersion;
    const actual = JSON.parse(fs.readFileSync(path.join(packageRoot, "package.json"), "utf8")).version;
    if (expected !== actual) throw new Error("Device-policy package differs from the pinned CLI version");
    if (fs.existsSync(POLICY_FILE)) throw new Error("The CI host already has a device policy");
    command("docker", ["pull", IMAGE], { stdio: "inherit" });
    const registry = fs.mkdtempSync(path.join(process.env.RUNNER_TEMP, "sdk-device-policy-"));
    const owner = randomUUID();
    fs.appendFileSync(
        process.env.GITHUB_ENV,
        `COPILOT_CI_DEVICE_POLICY_NATIVE_PATH=${native}\nCOPILOT_CI_DEVICE_POLICY_LEGACY_PATH=${legacy}\nCOPILOT_CI_DEVICE_POLICY_OWNER=${owner}\nCOPILOT_CI_DEVICE_POLICY_REGISTRY=${registry}\n`,
    );
    fs.appendFileSync(
        process.env.GITHUB_OUTPUT,
        `native-launcher=${path.join(SDK_ROOT, "scripts/ci/device-policy-native.js")}\nlegacy-launcher=${path.join(SDK_ROOT, "scripts/ci/device-policy-legacy.js")}\n`,
    );
}

function cleanup() {
    const owner = process.env.COPILOT_CI_DEVICE_POLICY_OWNER;
    if (!owner) throw new Error("Missing device-policy job ownership");
    const ids = command("docker", [
        "ps",
        "--all",
        "--quiet",
        "--no-trunc",
        "--filter",
        `label=${OWNER_LABEL}=${owner}`,
    ]).trim();
    const errors = [];
    for (const id of ids ? ids.split("\n") : []) {
        try {
            removeOwned(containerId(id), owner);
        } catch (error) {
            errors.push(error.message);
        }
    }
    const registry = ownedRegistry();
    for (const entry of pendingLaunches(fs.readdirSync(registry))) {
        const pending = path.join(registry, entry);
        const cidfile = path.join(registry, entry.replace(/\.pending$/, ".cid"));
        try {
            if (!fs.existsSync(cidfile)) throw new Error("Unsettled Docker launch has no container identity");
            removeOwned(containerId(fs.readFileSync(cidfile, "utf8")), owner);
            fs.unlinkSync(cidfile);
            fs.unlinkSync(pending);
        } catch (error) {
            errors.push(error.message);
        }
    }
    if (errors.length) throw new Error(`Device-policy cleanup did not settle: ${errors.join("; ")}`);
    if (fs.existsSync(POLICY_FILE)) throw new Error("The device fixture modified the CI host");
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try {
        const [action, argument] = process.argv.slice(2);
        if (action === "prepare") prepare();
        else if (action === "cleanup") cleanup();
        else if (action === "portable-pattern") console.log(portableTestPattern(argument));
        else if (action === "required-pattern") console.log(REQUIRED_PATTERN);
        else if (action === "verify" && argument) {
            verifyRequiredResults(JSON.parse(fs.readFileSync(argument, "utf8")));
            if (fs.existsSync(POLICY_FILE)) throw new Error("The device fixture modified the CI host");
        } else
            throw new Error(
                "Usage: device-policy-fixture.mjs <prepare|cleanup|portable-pattern|required-pattern|verify RESULTS>",
            );
    } catch (error) {
        console.error(error.message);
        process.exitCode = 1;
    }
}
