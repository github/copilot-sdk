/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs, { realpathSync } from "fs";
import { rm } from "fs/promises";
import os from "os";
import { basename, dirname, join, resolve } from "path";
import { rimraf } from "rimraf";
import { fileURLToPath } from "url";
import { afterAll, afterEach, beforeEach, onTestFailed, TestContext } from "vitest";
import { CopilotClient, CopilotClientOptions, RuntimeConnection } from "../../../src";
import { CapiProxy } from "./CapiProxy";
import { formatError, retry } from "./sdkTestHelper";

export const isCI = process.env.GITHUB_ACTIONS === "true";
export const DEFAULT_GITHUB_TOKEN = "fake-token-for-e2e-tests";

/**
 * True when the E2E suite is running over the in-process (FFI) transport
 * (COPILOT_SDK_DEFAULT_CONNECTION=inprocess). Use with `it.skipIf` / `describe.skipIf`
 * to skip tests for features that are not supported over the in-process transport (the
 * runtime loads into the shared host process), so the in-process CI cell stays green.
 * Such features are covered by the default (stdio) cell.
 */
export const isInProcessTransport =
    (process.env.COPILOT_SDK_DEFAULT_CONNECTION ?? "").toLowerCase() === "inprocess";

// The in-process (FFI) transport resolves auth host-side, in this test process, and
// ranks HMAC above the GitHub token — so an ambient COPILOT_HMAC_KEY (CI sets one as a
// job-level credential) would be picked over the SDK/Bearer token the replay snapshots
// expect, yielding 401s. Host-side auth can capture the key as early as client
// construction (before any per-test beforeEach runs), so neutralize it at module load —
// the analogue of .NET's InProcessEnvIsolation `[ModuleInitializer]`. Only applied for
// the in-process transport; stdio/tcp children resolve auth in their own process where
// the token already outranks HMAC. See https://github.com/github/copilot-sdk/issues/1934.
if ((process.env.COPILOT_SDK_DEFAULT_CONNECTION ?? "").toLowerCase() === "inprocess") {
    delete process.env.COPILOT_HMAC_KEY;
    delete process.env.CAPI_HMAC_KEY;
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const SNAPSHOTS_DIR = resolve(__dirname, "../../../../test/snapshots");

function getCliPathForTests(): string | undefined {
    if (process.env.COPILOT_CLI_PATH) {
        return process.env.COPILOT_CLI_PATH;
    }
    return undefined;
}

export async function createSdkTestContext({
    logLevel,
    useStdio,
    copilotClientOptions,
}: {
    logLevel?: "error" | "none" | "warning" | "info" | "debug" | "all";
    cliPath?: string;
    useStdio?: boolean;
    copilotClientOptions?: CopilotClientOptions;
} = {}) {
    const homeDir = realpathSync(fs.mkdtempSync(join(os.tmpdir(), "copilot-test-config-")));
    const copilotHomeDir = realpathSync(fs.mkdtempSync(join(os.tmpdir(), "copilot-test-home-")));
    const workDir = realpathSync(fs.mkdtempSync(join(os.tmpdir(), "copilot-test-work-")));

    const openAiEndpoint = new CapiProxy();
    const proxyUrl = await openAiEndpoint.start();
    await openAiEndpoint.setCopilotUserByToken(DEFAULT_GITHUB_TOKEN, {
        login: "e2e-test-user",
        copilot_plan: "individual_pro",
        is_mcp_enabled: true,
        endpoints: {
            api: proxyUrl,
            telemetry: "https://localhost:1/telemetry",
        },
        analytics_tracking_id: "e2e-test-tracking-id",
    });
    const authTokenToUse = isCI
        ? DEFAULT_GITHUB_TOKEN
        : (process.env.GITHUB_TOKEN ?? DEFAULT_GITHUB_TOKEN);

    const env = {
        ...process.env,
        ...openAiEndpoint.getProxyEnv(),
        COPILOT_API_URL: proxyUrl,
        // Route GitHub API calls (e.g. the MCP registry policy check) to the
        // replay proxy so MCP enablement stays hermetic. Without this the CLI
        // reaches the real api.github.com, which is slow/unreachable on macOS
        // CI runners and makes MCP servers time out before reaching connected.
        COPILOT_DEBUG_GITHUB_API_URL: proxyUrl,
        COPILOT_HOME: copilotHomeDir,
        COPILOT_SDK_AUTH_TOKEN: "",
        GH_CONFIG_DIR: homeDir,
        // Use the proxy-recognized token rather than blanking these. Tests that spin up
        // their own client without passing `gitHubToken` (e.g. the stdio/tcp
        // "works without onPermissionRequest" cases) rely on GH_TOKEN/GITHUB_TOKEN to
        // authenticate against the replay proxy. Blanking them only worked on CI, where an
        // ambient COPILOT_HMAC_KEY secret supplies the credential instead; locally there is
        // no HMAC key, so the child CLI had nothing to authenticate with and got a 401.
        GH_TOKEN: authTokenToUse,
        GITHUB_TOKEN: authTokenToUse,

        // TODO: I'm not convinced the SDK should default to using whatever config you happen to have in your homedir.
        // The SDK config should be independent of the regular CLI app. Likewise it shouldn't mix sessions from the
        // SDK with those from the CLI app, at least not by default.
        XDG_CONFIG_HOME: homeDir,
        XDG_STATE_HOME: homeDir,
    };

    const userConn = copilotClientOptions?.connection;
    const cliPath = getCliPathForTests();
    let connection: RuntimeConnection;
    if (userConn) {
        // Caller supplied a RuntimeConnection — merge in the harness-managed
        // CLI path (and stay on the same transport variant). Strip `kind`
        // before forwarding to the factory opts since the factories don't
        // accept it in their argument shape.
        if (userConn.kind === "tcp") {
            const { kind: _k, ...tcp } = userConn;
            connection = RuntimeConnection.forTcp({
                ...tcp,
                path: tcp.path ?? cliPath,
            });
        } else if (userConn.kind === "stdio") {
            const { kind: _k, ...stdio } = userConn;
            connection = RuntimeConnection.forStdio({
                ...stdio,
                path: stdio.path ?? cliPath,
            });
        } else {
            connection = userConn;
        }
    } else if (useStdio === false) {
        connection = RuntimeConnection.forTcp({ path: cliPath });
    } else if (
        useStdio === undefined &&
        (process.env.COPILOT_SDK_DEFAULT_CONNECTION ?? "").toLowerCase() === "inprocess"
    ) {
        // The in-process FFI transport resolves the CLI entrypoint itself
        // (COPILOT_CLI_PATH or the bundled platform package), so no path is passed.
        connection = RuntimeConnection.forInProcess();
    } else {
        connection = RuntimeConnection.forStdio({ path: cliPath });
    }

    const {
        connection: _ignoredConnection,
        env: userEnv,
        ...remainingClientOptions
    } = copilotClientOptions ?? {};

    const mergedEnv = { ...env, ...userEnv };

    // The in-process (FFI) transport loads the runtime into this test host process,
    // and its worker inherits this process's ambient environment rather than a
    // per-client env block (see https://github.com/github/copilot-sdk/issues/1934).
    // So the per-test redirects, isolated home, and credentials must be mirrored onto
    // the real process environment. Node's `process.env` writes reach native `getenv`,
    // so host-side runtime reads (auth resolution, GitHub API redirect) observe them.
    // Auth flows via GH_TOKEN/GITHUB_TOKEN here (the FFI argv omits the stdio
    // `--auth-token-env COPILOT_SDK_AUTH_TOKEN` wiring), and HMAC is disabled so
    // host-side auth resolution picks the SDK/Bearer token the replay snapshots expect.
    const isInProcess = connection.kind === "inprocess";
    const inProcessEnv: Record<string, string> = isInProcess
        ? {
              ...(mergedEnv as Record<string, string>),
              GH_TOKEN: authTokenToUse,
              GITHUB_TOKEN: authTokenToUse,
              COPILOT_HMAC_KEY: "",
              CAPI_HMAC_KEY: "",
          }
        : {};

    const copilotClient = new CopilotClient({
        // The in-process transport rejects a per-client workingDirectory (it would have to
        // mutate the shared host process cwd). Instead the harness changes this process's
        // cwd to workDir around the in-process worker's startup (see beforeEach below), so
        // the worker still spawns with workDir as its cwd. Out-of-process clients get it
        // as a normal per-client option.
        workingDirectory: isInProcess ? undefined : workDir,
        // In-process hosting mirrors the environment onto the real process (per test, in
        // beforeEach below), so the worker inherits it; passing a per-client env here
        // would have no effect.
        env: isInProcess ? undefined : mergedEnv,
        logLevel: logLevel || "error",
        connection,
        gitHubToken: authTokenToUse,
        ...remainingClientOptions,
    });

    const harness = { homeDir, workDir, openAiEndpoint, copilotClient, env };

    // Track if any test fails to avoid writing corrupted snapshots
    let anyTestFailed = false;

    // Holds the process.env entries the current test overwrote, so afterEach restores them.
    let restoreProcessEnv: Array<[string, string | undefined]> = [];

    // Holds the process cwd before an in-process test changed it, so afterEach restores it.
    let restoreCwd: string | undefined;

    // Wire up to Vitest lifecycle
    beforeEach(async (testContext) => {
        // Must be inside beforeEach - vitest requires test context
        onTestFailed(() => {
            anyTestFailed = true;
        });

        // Mirror this context's environment onto the real process for in-process
        // hosting, right before the test runs (see the comment above the client). The
        // client auto-starts on first use inside the test body, so the worker spawns
        // under these values.
        restoreProcessEnv = [];
        for (const [key, value] of Object.entries(inProcessEnv)) {
            restoreProcessEnv.push([key, process.env[key]]);
            process.env[key] = value;
        }

        // The in-process worker inherits this process's cwd at spawn (the client auto-starts
        // on first use inside the test body). Point cwd at workDir here so the worker spawns
        // with the same working directory the out-of-process transport passes explicitly;
        // afterEach restores it.
        if (isInProcess) {
            restoreCwd = process.cwd();
            process.chdir(workDir);
        }

        await openAiEndpoint.updateConfig({
            filePath: getTrafficCapturePath(testContext),
            workDir,
            testInfo: {
                file: testContext.task.file.filepath,
                line: testContext.task.location?.line,
            },
        });
    });

    afterEach(async () => {
        // Undo this test's process.env mirror so it can't leak into the next test/suite.
        for (const [key, previous] of restoreProcessEnv.reverse()) {
            if (previous === undefined) {
                delete process.env[key];
            } else {
                process.env[key] = previous;
            }
        }
        restoreProcessEnv = [];
        // Restore the cwd an in-process test changed for worker startup.
        if (restoreCwd !== undefined) {
            process.chdir(restoreCwd);
            restoreCwd = undefined;
        }
        // Empty directories but leave them in place for next test
        await rimraf([join(homeDir, "*"), join(workDir, "*")], { glob: true });
    });

    afterAll(async () => {
        await copilotClient.stop();
        await openAiEndpoint.stop(anyTestFailed);
        await rmDir("remove e2e test copilotHomeDir", copilotHomeDir);
        await rmDir("remove e2e test homeDir", homeDir);
        await rmDir("remove e2e test workDir", workDir);
    });

    return harness;
}

function getTrafficCapturePath(testContext: TestContext): string {
    const testFilePath = testContext.task.file.filepath;
    const suffix = ".test.ts";
    if (!testFilePath.endsWith(suffix)) {
        throw new Error(
            `Test file path does not end with expected suffix '${suffix}': ${testFilePath}`
        );
    }

    // Convert to snake_case for cross-SDK snapshot compatibility
    // Strip ".e2e" suffix so renamed "xxx.e2e.test.ts" still uses snapshot folder "xxx"
    let testFileName = basename(testFilePath, suffix).replace(/-/g, "_");
    if (testFileName.endsWith(".e2e")) {
        testFileName = testFileName.slice(0, -".e2e".length);
    }
    const taskNameAsFilename = testContext.task.name.replace(/[^a-z0-9]/gi, "_").toLowerCase();
    return join(SNAPSHOTS_DIR, testFileName, `${taskNameAsFilename}.yaml`);
}

async function rmDir(message: string, path: string): Promise<void> {
    // Use longer retries to tolerate Windows holding SQLite session-store.db
    // open briefly after the CLI subprocess exits. If the temp dir still can't
    // be removed (e.g. CLI background writer racing with cleanup), warn and
    // continue rather than failing the whole test run — the OS / CI runner
    // will reclaim the temp dir on shutdown.
    try {
        await retry(message, () => rm(path, { recursive: true, force: true }), 30, 1000);
    } catch (error) {
        console.warn(
            `WARN: ${message} failed; leaving temp dir for OS cleanup: ${formatError(error)}`
        );
    }
}
