/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { POLICY_FILE, verifyIdentity, verifyIsolation } from "./device-policy-fixture.mjs";

function start() {
    const plan = JSON.parse(process.env.COPILOT_CI_DEVICE_POLICY_PLAN);
    if (process.platform !== "linux" || typeof process.execve !== "function")
        throw new Error("Unsupported device-policy container runtime");
    if (process.argv[2] === "--runtime") {
        const policy = fs.lstatSync(POLICY_FILE);
        verifyIdentity(plan, {
            uid: process.getuid(),
            gid: process.getgid(),
            groups: process.getgroups(),
            policy: {
                isFile: policy.isFile(),
                isSymbolicLink: policy.isSymbolicLink(),
                uid: policy.uid,
                gid: policy.gid,
                mode: policy.mode,
            },
        });
        process.execve(plan.executable, plan.argv, plan.env);
        throw new Error("Runtime exec unexpectedly returned");
    }
    verifyIsolation({
        originalNamespace: plan.originalNamespace,
        namespace: fs.readlinkSync("/proc/self/ns/mnt"),
        mountinfo: fs.readFileSync("/proc/self/mountinfo", "utf8"),
        uid: process.getuid(),
        gid: process.getgid(),
    });
    if (
        !Number.isSafeInteger(plan.uid) ||
        plan.uid <= 0 ||
        !Number.isSafeInteger(plan.gid) ||
        plan.gid < 0 ||
        !plan.groups.every((group) => Number.isSafeInteger(group) && group >= 0)
    )
        throw new Error("Invalid runner identity");
    const directory = path.dirname(POLICY_FILE);
    if (fs.existsSync(directory)) throw new Error("The container already has a device-policy directory");
    fs.mkdirSync(directory, { mode: 0o755 });
    fs.writeFileSync(POLICY_FILE, plan.policy, { mode: 0o644, flag: "wx" });
    fs.chmodSync(POLICY_FILE, 0o644);

    const passwd = fs.readFileSync("/etc/passwd", "utf8");
    if (!passwd.split("\n").some((line) => Number(line.split(":")[2]) === plan.uid)) {
        if (/[:\r\n]/.test(plan.home)) throw new Error("Invalid runner home");
        fs.appendFileSync("/etc/passwd", `copilot-sdk-ci:x:${plan.uid}:${plan.gid}::${plan.home}:/bin/sh\n`);
    }
    const group = fs.readFileSync("/etc/group", "utf8");
    if (!group.split("\n").some((line) => Number(line.split(":")[2]) === plan.gid)) {
        fs.appendFileSync("/etc/group", `copilot-sdk-ci:x:${plan.gid}:\n`);
    }
    if (!fs.existsSync(plan.home)) {
        fs.mkdirSync(plan.home, { recursive: true, mode: 0o700 });
    }
    fs.chownSync(plan.home, plan.uid, plan.gid);
    const args = [
        "/usr/bin/setpriv",
        `--reuid=${plan.uid}`,
        `--regid=${plan.gid}`,
        ...(plan.groups.length ? [`--groups=${plan.groups.join(",")}`] : ["--clear-groups"]),
        plan.nodeExecutable,
        fileURLToPath(import.meta.url),
        "--runtime",
    ];
    process.execve("/usr/bin/setpriv", args, process.env);
    throw new Error("Identity transition unexpectedly returned");
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try {
        start();
    } catch (error) {
        console.error(error.message);
        process.exitCode = 1;
    }
}
