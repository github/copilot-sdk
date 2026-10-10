/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import test from "node:test";
import "./device-policy-bootstrap.mjs";
import {
    CLIENT_LABEL,
    containerId,
    dockerArguments,
    fixtureDirectories,
    OWNER_LABEL,
    pendingLaunches,
    POLICY_FILE,
    portableTestPattern,
    runtimeInvocation,
    PORTABLE_PATTERN,
    REQUIRED_CASES,
    REQUIRED_PATTERN,
    verifyIdentity,
    verifyIsolation,
    verifyOwnership,
    verifyRequiredResults,
} from "./device-policy-fixture.mjs";

const report = () => ({
    testResults: REQUIRED_CASES.map(({ file, title }) => ({
        name: `/workspace/nodejs/test/e2e/${file}`,
        assertionResults: [{ title, status: "passed" }],
    })),
});

test("requires both unchanged strict controls to pass", () => {
    verifyRequiredResults(report());
    for (const index of [0, 1]) {
        for (const status of ["pending", "skipped", "failed", "todo"]) {
            const result = report();
            result.testResults[index].assertionResults[0].status = status;
            assert.throws(() => verifyRequiredResults(result), /did not pass/);
        }
        const missing = report();
        missing.testResults.splice(index, 1);
        assert.throws(() => verifyRequiredResults(missing), /did not pass/);
        const duplicate = report();
        duplicate.testResults.push(duplicate.testResults[index]);
        assert.throws(() => verifyRequiredResults(duplicate), /did not pass/);
    }
    assert.throws(() => verifyRequiredResults({}), /did not pass/);
});

test("assigns only the two device-fixture cases to the mandatory Linux gate", () => {
    assert.equal(portableTestPattern("checkout"), "");
    assert.equal(portableTestPattern("published"), PORTABLE_PATTERN);
    assert.throws(() => portableTestPattern(""), /Unknown/);
    const portable = new RegExp(PORTABLE_PATTERN);
    const required = new RegExp(REQUIRED_PATTERN);
    for (const { title } of REQUIRED_CASES) {
        assert.equal(portable.test(`Suite ${title}`), false);
        assert.equal(required.test(title), true);
    }
    for (const title of [
        "should clear the managed settings cache",
        "should expose the managed settings schema",
        "should list server sessions",
    ]) {
        assert.equal(portable.test(title), true);
        assert.equal(required.test(title), false);
    }
});

test("preserves both runtime entry points' original argv and environment", () => {
    const args = ["--stdio", "--argument-with-spaces=one two", "--literal=;$()"];
    const env = { TOKEN: "private-value", COPILOT_HOME: "/tmp/copilot-test-home-one" };
    const native = runtimeInvocation("native", "/package/copilot-runtime", "/node/bin/node", args, env);
    assert.equal(native.executable, "/package/copilot-runtime");
    assert.deepEqual(native.argv, ["/package/copilot-runtime", ...args]);
    const legacy = runtimeInvocation("legacy", "/package/app.js", "/node/bin/node", args, env);
    assert.equal(legacy.executable, "/node/bin/node");
    assert.deepEqual(legacy.argv, ["/node/bin/node", "/package/app.js", ...args]);
    assert.deepEqual(native.env, env);
    assert.notEqual(native.env, env);
    assert.throws(() => runtimeInvocation("unknown", "", "", [], {}), /entry point/);
});

test("binds only explicitly owned E2E fixture directories for writing", () => {
    const roots = fixtureDirectories({
        cwd: "/tmp/copilot-test-work-one",
        env: {
            COPILOT_HOME: "/tmp/copilot-test-home-one",
            GH_CONFIG_DIR: "/tmp/copilot-test-config-one",
            XDG_CONFIG_HOME: "/tmp/copilot-test-config-one",
            XDG_STATE_HOME: "/tmp/copilot-test-work-one/local-home",
        },
        temporaryDirectory: "/tmp",
    });
    assert.deepEqual(roots, [
        "/tmp/copilot-test-work-one",
        "/tmp/copilot-test-home-one",
        "/tmp/copilot-test-config-one",
    ]);
    for (const cwd of ["/etc", "/home/runner", "/tmp", "/tmp/../../etc", "/tmp/not-a-fixture"]) {
        assert.throws(() => fixtureDirectories({ cwd, env: {}, temporaryDirectory: "/tmp" }), /fixture/);
    }
});

test("keeps policy and secret environment contents out of Docker arguments", () => {
    const plan = {
        owner: "job-one",
        client: "client-one",
        sdkRoot: "/workspace",
        cwd: "/tmp/copilot-test-work-one",
        readonly: ["/workspace", "/artifacts/package"],
        writable: ["/tmp/copilot-test-work-one"],
        env: { SECRET: "must-not-appear" },
        policy: '{"model":"secret-model"}',
    };
    const args = dockerArguments(plan, "/registry/client-one.cid");
    assert.deepEqual(args.slice(0, 7), ["run", "--rm", "--init", "--interactive", "--network", "host", "--cidfile"]);
    assert.equal(args.includes(`${OWNER_LABEL}=job-one`), true);
    assert.equal(args.includes(`${CLIENT_LABEL}=client-one`), true);
    assert.equal(args.includes("COPILOT_CI_DEVICE_POLICY_PLAN"), true);
    assert.equal(args.includes("/artifacts/package:/artifacts/package:ro"), true);
    assert.equal(args.includes("/tmp/copilot-test-work-one:/tmp/copilot-test-work-one:rw"), true);
    assert.equal(args.join(" ").includes("must-not-appear"), false);
    assert.equal(args.join(" ").includes("secret-model"), false);
    const other = dockerArguments({ ...plan, client: "client-two" }, "/registry/client-two.cid");
    assert.notDeepEqual(args, other);
    for (const directory of ["/", "/etc", "/etc/github-copilot", "/proc", "/sys", "/dev", "/path:ambiguous"]) {
        assert.throws(() => dockerArguments({ ...plan, readonly: [directory] }, "/registry/client.cid"));
    }
    assert.throws(() => dockerArguments({ ...plan, readonly: plan.writable }, "/registry/client.cid"), /both/);
});

test("requires exact job ownership and full container IDs before cleanup", () => {
    verifyOwnership("job-one\n", "job-one");
    for (const [label, owner] of [
        ["job-two", "job-one"],
        ["", ""],
        ["<no value>", "job-one"],
    ]) {
        assert.throws(() => verifyOwnership(label, owner), /another job/);
    }
    assert.equal(containerId(`${"a".repeat(64)}\n`), "a".repeat(64));
    for (const id of ["", "a".repeat(12), `${"a".repeat(64)} extra`, "G".repeat(64)]) {
        assert.throws(() => containerId(id), /container ID/);
    }
});

test("exposes interrupted or unknown launch receipts instead of claiming cleanup", () => {
    const client = "12345678-1234-1234-1234-123456789abc";
    assert.deepEqual(pendingLaunches([]), []);
    assert.deepEqual(pendingLaunches([`${client}.pending`, `${client}.cid`]), [`${client}.pending`]);
    assert.deepEqual(pendingLaunches([`${client}.pending`]), [`${client}.pending`]);
    assert.throws(() => pendingLaunches([`${client}.cid`]), /no launch receipt/);
    assert.throws(() => pendingLaunches(["unknown"]), /Unknown/);
});

const isolated = () => ({
    originalNamespace: "mnt:[100]",
    namespace: "mnt:[200]",
    uid: 0,
    gid: 0,
    mountinfo: "1 0 0:1 / / rw - overlay overlay rw\n2 1 0:2 / /workspace ro - ext4 disk ro",
});

test("requires private container backing before any policy mutation", () => {
    verifyIsolation(isolated());
    for (const change of [
        { namespace: "mnt:[100]" },
        { uid: 1001 },
        { gid: 1001 },
        { originalNamespace: "" },
        { mountinfo: "1 0 0:1 / / rw shared:1 - overlay overlay rw" },
        { mountinfo: "1 0 0:1 / / rw - ext4 disk rw" },
        { mountinfo: `${isolated().mountinfo}\n3 1 0:3 / /etc rw - ext4 host rw` },
        { mountinfo: `${isolated().mountinfo}\n3 1 0:3 / /etc/github-copilot rw - ext4 host rw` },
    ])
        assert.throws(() => verifyIsolation({ ...isolated(), ...change }));
});

test("retains runner uid, gid and groups with an ordinary root-owned device file", () => {
    const plan = { uid: 1001, gid: 1001, groups: [1001, 118] };
    const runtime = {
        uid: 1001,
        gid: 1001,
        groups: [118, 1001],
        policy: { isFile: true, isSymbolicLink: false, uid: 0, gid: 0, mode: 0o100644 },
    };
    verifyIdentity(plan, runtime);
    for (const change of [{ uid: 0 }, { gid: 0 }, { groups: [] }]) {
        assert.throws(() => verifyIdentity(plan, { ...runtime, ...change }), /identity/);
    }
    for (const change of [
        { uid: 1001 },
        { gid: 1001 },
        { mode: 0o100666 },
        { isSymbolicLink: true },
        { isFile: false },
    ]) {
        assert.throws(
            () => verifyIdentity(plan, { ...runtime, policy: { ...runtime.policy, ...change } }),
            /root-owned/,
        );
    }
    assert.equal(POLICY_FILE, "/etc/github-copilot/managed-settings.json");
});
