/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { once } from "node:events";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { createMessageConnection } from "vscode-jsonrpc/node.js";
import { createInternalServerRpc } from "../src/generated/rpc.js";

async function showFeatureFlags(rpc: ReturnType<typeof createInternalServerRpc>) {
    await rpc.connect({});
    const { flags } = await rpc.featureFlags.listMetadata();
    assert.equal(flags.TASK_ANALYTICS.name, "TASK_ANALYTICS");
    assert.equal(flags.TASK_ANALYTICS.expFlagEnvVar, "COPILOT_EXP_COPILOT_CLI_TASK_ANALYTICS");

    const population = { isStaff: false, isExperimental: false, isTeam: false };
    const experimentOnly = await rpc.featureFlags.resolve({
        ...population,
        environment: { COPILOT_EXP_COPILOT_CLI_TASK_ANALYTICS: "true" },
    });
    assert.equal(experimentOnly.TASK_ANALYTICS, false);

    const request = {
        ...population,
        baseFlags: { TASK_ANALYTICS: false },
        environment: { TASK_ANALYTICS: "true" },
    };
    const environment = await rpc.featureFlags.resolve(request);
    const config = { enabledFeatureFlags: { TASK_ANALYTICS: false } };
    const settings = await rpc.featureFlags.resolve({ ...request, config });
    const explicit = await rpc.featureFlags.resolve({
        ...request,
        config,
        flagOverrides: { TASK_ANALYTICS: true },
    });
    assert.deepEqual(environment, { TASK_ANALYTICS: true });
    assert.deepEqual(settings, { TASK_ANALYTICS: false });
    assert.deepEqual(explicit, { TASK_ANALYTICS: true });

    console.log(
        JSON.stringify({
            declaredFlags: Object.keys(flags).length,
            metadata: flags.TASK_ANALYTICS,
            staticResolution: {
                experimentOnly: experimentOnly.TASK_ANALYTICS,
                environment: environment.TASK_ANALYTICS,
                settings: settings.TASK_ANALYTICS,
                explicit: explicit.TASK_ANALYTICS,
            },
        })
    );
}

const runtimePath = process.argv[2];
if (!runtimePath) {
    throw new Error("Usage: tsx examples/internal-feature-flags.ts <copilot-runtime-path>");
}

const home = await mkdtemp(join(tmpdir(), "copilot-feature-flags-"));
const child = spawn(
    resolve(runtimePath),
    ["--server", "--stdio", "--no-auto-login", "--no-auto-update", "--no-remote"],
    {
        cwd: home,
        env: { ...process.env, COPILOT_HOME: home, COPILOT_CONNECTION_TOKEN: "" },
        stdio: ["pipe", "pipe", "inherit"],
        windowsHide: true,
    }
);
const closed = once(child, "close");
const connection = createMessageConnection(child.stdout, child.stdin);
connection.listen();
try {
    await Promise.race([
        showFeatureFlags(createInternalServerRpc(connection)),
        closed.then(([code, signal]) => {
            throw new Error(
                `Runtime exited before the example completed: code=${code}, signal=${signal}`
            );
        }),
    ]);
} finally {
    connection.dispose();
    child.stdin.end();
    try {
        const [code, signal] = await closed;
        assert.equal(code, 0, `Runtime exit signal: ${signal}`);
    } finally {
        await rm(home, { recursive: true, force: true });
    }
}
