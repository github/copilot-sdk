/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// This offline example must exit without an explicit runtime stop or process.exit.
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdir, realpath } from "node:fs/promises";
import { createRequire } from "node:module";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";

const { values } = parseArgs({
    options: {
        runtime: { type: "string" },
        "cli-native": { type: "string" },
        root: { type: "string" },
        "verify-exit": { type: "boolean" },
        help: { type: "boolean" },
    },
});

if (values.help || !values.root || !values.runtime || !values["cli-native"]) {
    console.log(
        "Usage: node src/sdk/rust/examples/idle_auth_owner.mjs " +
            "--root <isolated-directory> --runtime <runtime.node> --cli-native <cli-native.node>\n" +
            "Add --verify-exit to check natural exit in a child process.\n" +
            "The idle host must not keep Node alive. This example uses synthetic credentials only.",
    );
} else if (values["verify-exit"]) {
    const child = spawnSync(
        process.execPath,
        [fileURLToPath(import.meta.url), ...process.argv.slice(2).filter((value) => value !== "--verify-exit")],
        { encoding: "utf8", timeout: 10_000 },
    );
    assert.ifError(child.error);
    assert.equal(child.status, 0, child.stderr);
    assert.match(child.stdout, /the idle process must exit naturally/);
    console.log("Idle SDK-owned host: natural child exit verified");
} else {
    await mkdir(resolve(values.root), { recursive: true });
    const root = await realpath(resolve(values.root));
    for (const key of Object.keys(process.env)) {
        if (/^(COPILOT_|GITHUB_|GH_|AGENT_|WS1_|XDG_|AZURE_|ENTRA_|MSAL_)/i.test(key)) {
            delete process.env[key];
        }
    }
    Object.assign(process.env, {
        HOME: root,
        USERPROFILE: root,
        COPILOT_HOME: root,
        COPILOT_OFFLINE: "true",
        COPILOT_DISABLE_KEYTAR: "1",
        COPILOT_CLI_PATH: process.execPath,
    });
    const require = createRequire(import.meta.url);
    const runtime = require(await realpath(resolve(values.runtime)));
    const cli = require(await realpath(resolve(values["cli-native"])));
    const owner = new runtime.ProcessAuthHandle({
        configDir: root,
        disableAutoLogin: true,
        authTokenEnvVar: "AGENT_GITHUB_TOKEN",
        startupEnvironment: {
            AGENT_GITHUB_TOKEN: "ghs_synthetic_idle_owner",
            COPILOT_GH_HOST: "https://idle-owner.example",
            COPILOT_OFFLINE: "false",
        },
    });
    assert.equal((await owner.current())?.authInfo.host, "https://idle-owner.example");
    await cli.cliRuntimeStart(
        process.execPath,
        {
            configDir: root,
            disableAutoLogin: true,
            runtimeLibraryPath: await realpath(resolve(values.runtime)),
        },
        owner,
    );
    const current = JSON.parse(await cli.cliRuntimeCall("account.getCurrentAuth", "{}"));
    assert.equal(current.authInfo.host, "https://idle-owner.example");
    console.log("SDK-owned host: authenticated; the idle process must exit naturally");
}
