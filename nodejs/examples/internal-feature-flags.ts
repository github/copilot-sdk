/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { once } from "node:events";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { createMessageConnection } from "vscode-jsonrpc/node";
import { createInternalServerRpc } from "../src/generated/rpc.js";

async function showFeatureFlags(rpc: ReturnType<typeof createInternalServerRpc>) {
    await rpc.connect({});
    const { flags } = await rpc.featureFlags.listMetadata();
    assert.equal(
        flags.DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP.name,
        "DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP"
    );
    assert.equal(
        flags.DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP.expFlagEnvVar,
        "COPILOT_EXP_COPILOT_CLI_DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP"
    );

    const population = { isStaff: false, isExperimental: false, isTeam: false };
    const experimentOnly = await rpc.featureFlags.resolve({
        ...population,
        environment: { COPILOT_EXP_COPILOT_CLI_DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP: "true" },
    });
    assert.equal(experimentOnly.DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP, false);

    const request = {
        ...population,
        baseFlags: { DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP: false },
        environment: { DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP: "true" },
    };
    const environment = await rpc.featureFlags.resolve(request);
    const config = { enabledFeatureFlags: { DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP: false } };
    const settings = await rpc.featureFlags.resolve({ ...request, config });
    const explicit = await rpc.featureFlags.resolve({
        ...request,
        config,
        flagOverrides: { DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP: true },
    });
    assert.deepEqual(environment, { DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP: true });
    assert.deepEqual(settings, { DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP: false });
    assert.deepEqual(explicit, { DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP: true });

    console.log(
        JSON.stringify({
            declaredFlags: Object.keys(flags).length,
            metadata: flags.DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP,
            staticResolution: {
                experimentOnly: experimentOnly.DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP,
                environment: environment.DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP,
                settings: settings.DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP,
                explicit: explicit.DYNAMIC_INSTRUCTIONS_RETRIEVAL_MCP,
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
