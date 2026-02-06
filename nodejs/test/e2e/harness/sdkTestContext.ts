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
import { CopilotClient } from "../../../src";
import { CapiProxy } from "./CapiProxy";
import { retry } from "./sdkTestHelper";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const SNAPSHOTS_DIR = resolve(__dirname, "../../../../test/snapshots");

export async function createSdkTestContext({
    logLevel,
}: { logLevel?: "error" | "none" | "warning" | "info" | "debug" | "all" } = {}) {
    const homeDir = realpathSync(fs.mkdtempSync(join(os.tmpdir(), "copilot-test-config-")));
    const workDir = realpathSync(fs.mkdtempSync(join(os.tmpdir(), "copilot-test-work-")));

    const openAiEndpoint = new CapiProxy();
    const proxyUrl = await openAiEndpoint.start();
    const env = {
        ...process.env,
        COPILOT_API_URL: proxyUrl,

        // TODO: I'm not convinced the SDK should default to using whatever config you happen to have in your homedir.
        // The SDK config should be independent of the regular CLI app. Likewise it shouldn't mix sessions from the
        // SDK with those from the CLI app, at least not by default.
        XDG_CONFIG_HOME: homeDir,
        XDG_STATE_HOME: homeDir,
    };

    const copilotClient = new CopilotClient({
        cwd: workDir,
        env,
        logLevel: logLevel || "error",
        // Use fake token in CI to allow cached responses without real auth
        githubToken: process.env.CI === "true" ? "fake-token-for-e2e-tests" : undefined,
    });

    const harness = { homeDir, workDir, openAiEndpoint, copilotClient, env };

    // Track if any test fails to avoid writing corrupted snapshots
    let anyTestFailed = false;

    // Wire up to Vitest lifecycle
    beforeEach(async (testContext) => {
        // Must be inside beforeEach - vitest requires test context
        onTestFailed(() => {
            anyTestFailed = true;
        });

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
        // Empty directories but leave them in place for next test
        await rimraf([join(homeDir, "*"), join(workDir, "*")], { glob: true });
    });

    afterAll(async () => {
        await copilotClient.stop();
        await openAiEndpoint.stop(anyTestFailed);
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
    const testFileName = basename(testFilePath, suffix).replace(/-/g, "_");
    const taskNameAsFilename = testContext.task.name.replace(/[^a-z0-9]/gi, "_").toLowerCase();
    return join(SNAPSHOTS_DIR, testFileName, `${taskNameAsFilename}.yaml`);
}

function rmDir(message: string, path: string): Promise<void> {
    return retry(message, () => rm(path, { recursive: true, force: true }), 5, 2000);
}
