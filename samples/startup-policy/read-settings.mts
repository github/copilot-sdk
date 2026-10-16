/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { FfiRuntimeHost } from "../../nodejs/src/ffiRuntimeHost.js";
import { createInternalServerRpc, createServerRpc } from "../../nodejs/src/generated/rpc.js";

const library = process.argv[2];
assert(library, "Pass the same-checkout runtime.node path as the first argument");
const home = await mkdtemp(join(tmpdir(), "copilot-startup-policy-"));
let host: FfiRuntimeHost | undefined;
let connection: ReturnType<typeof createMessageConnection> | undefined;
const errors: unknown[] = [];
try {
    const configDir = join(home, ".copilot");
    const repo = join(home, "project");
    await mkdir(configDir);
    await mkdir(join(repo, ".github", "copilot"), { recursive: true });
    await writeFile(join(configDir, "settings.json"), JSON.stringify({ model: "user-model" }));
    await writeFile(join(configDir, "config.json"), JSON.stringify({ model: "obsolete-model" }));
    await writeFile(
        join(repo, ".github", "copilot", "settings.json"),
        JSON.stringify({ model: "repo-model" })
    );
    host = FfiRuntimeHost.create(
        resolve(library),
        undefined,
        {
            HOME: home,
            USERPROFILE: home,
            COPILOT_HOME: configDir,
            COPILOT_CACHE_HOME: join(home, "cache"),
            COPILOT_OFFLINE: "true",
            COPILOT_USE_KEYCHAIN: "false",
        },
        ["--no-auto-login"]
    );
    await host.start();
    connection = createMessageConnection(
        new StreamMessageReader(host.receiveStream),
        new StreamMessageWriter(host.sendStream)
    );
    connection.listen();
    const internal = createInternalServerRpc(connection);
    const rpc = createServerRpc(connection);
    const startupGates = await internal.featureFlags.resolveStartupGates({
        flags: ["NATIVE_WEB_SEARCH"],
        isStaff: false,
        isExperimental: false,
        isTeam: false,
        streamerMode: false,
        refreshWithoutSession: false,
        environment: {},
        config: { featureFlags: { NATIVE_WEB_SEARCH: false } },
    });
    assert.equal(startupGates.NATIVE_WEB_SEARCH, false);
    const loaded = await internal.user.settings.loadWithWarning();
    assert.equal(loaded.settings.model, "user-model");
    const merged = await internal.user.settings.loadAndMergeRepo({
        repoRoot: repo,
        userSettings: loaded.settings,
    });
    assert.equal(merged.settings.model, "repo-model");
    const edit = await internal.repo.settings.edit({
        repoRoot: repo,
        scope: "local",
        key: "deniedUrls",
        operation: "add-item",
        value: "https://example.com",
    });
    assert.equal(edit.status, "changed");
    const controls = await internal.managedSettings.applyControls({
        settings: { enabledPlugins: { required: false } },
        managed: { enabledPlugins: { required: true } },
    });
    assert.deepEqual(controls.settingsJson, { enabledPlugins: { required: true } });
    const permissions = await internal.permissions.evaluateManagedPolicy({
        permissions: { disableBypassPermissionsMode: "disable" },
    });
    assert.equal(permissions.bypassPermissionsDisabled, true);
    const ingestion = await internal.managedSettings.ingest({
        selfFetch: false,
        userAgent: "startup-policy-example",
        environment: {},
        platform: process.platform,
        homeDirectory: home,
    });
    assert.equal(ingestion.serverFetchFailed, false);
    const sandbox = await internal.sandbox.resolveFlooredConfig({
        userSandbox: { enabled: false },
        managedSandbox: { enabled: true, failIfUnavailable: true },
    });
    const relevant = await rpc.sandbox.proxyCa.checkRelevance({
        sandboxConfig: { enabled: true, auth: { gh: true } },
    });
    assert.equal(relevant, true);
    console.log(
        JSON.stringify(
            {
                userModel: loaded.settings.model,
                repoModel: merged.settings.model,
                repoEdit: edit.status,
                startupGates,
                managedControls: controls.settingsJson,
                permissions,
                ingestion: ingestion.serverResolution,
                sandbox: sandbox.sandboxJson,
                proxyCaRelevant: relevant,
                proxyCaInstallationSupported: await rpc.sandbox.proxyCa.checkInstallationSupport(),
            },
            null,
            2
        )
    );
} catch (error) {
    errors.push(error);
} finally {
    connection?.dispose();
    try {
        await host?.dispose();
    } catch (error) {
        errors.push(error);
    }
    try {
        await rm(home, { recursive: true, force: true });
    } catch (error) {
        errors.push(error);
    }
}
if (errors.length > 0) {
    throw new AggregateError(errors, "Startup policy example failed");
}
