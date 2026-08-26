/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync, spawn } from "node:child_process";
import { existsSync, mkdtempSync, readFileSync } from "node:fs";
import { copyFile, mkdir, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { expect, it } from "vitest";
import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { approveAll } from "../../src/index.js";
import { getSdkProtocolVersion } from "../../src/sdkProtocolVersion.js";
import { createSdkTestContext, isInProcessTransport } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const FIXTURE = join(__dirname, "fixtures", "env-access-extension.mjs");
const DIST_DIR = resolve(__dirname, "..", "..", "dist");

interface ExtensionRun {
    resumeParams: Record<string, unknown>;
    /** Sample taken before the join, so an inherited value is distinguishable from a granted one. */
    prejoin: string;
    postjoin: string;
    result: string;
}

/**
 * Run the fixture extension as a real child process against a stub host that
 * speaks the extension side of the wire.
 *
 * The extension connection is plain JSON-RPC over the extension process's own
 * stdio, so a stub host observes exactly what the CLI observes. That is the only
 * way to cover this feature end to end today: the released CLI predates the host
 * half (github/copilot-agent-runtime#15144), so it ignores the request and grants
 * nothing. Once the `@github/copilot` dependency carries the host half, the
 * real-CLI case below can assert the grant instead.
 */
async function runExtensionAgainstStubHost(options: {
    requested: string[];
    /** Pass an explicit empty list rather than omitting the option. */
    requestEmptyList?: boolean;
    /** Names the extension samples but never asks for. */
    observe?: string[];
    /** Values the host resolves for an approved request, or undefined to deny it. */
    granted?: Record<string, string>;
}): Promise<ExtensionRun> {
    if (!existsSync(join(DIST_DIR, "extension.js"))) {
        throw new Error(`Built SDK not found at ${DIST_DIR}. Run \`npm run build\` first.`);
    }

    const dir = mkdtempSync(join(tmpdir(), "copilot-env-access-"));
    const prejoinFile = join(dir, "prejoin");
    const postjoinFile = join(dir, "postjoin");
    const resultFile = join(dir, "result");

    const child = spawn(process.execPath, [FIXTURE], {
        stdio: ["pipe", "pipe", "pipe"],
        env: {
            ...process.env,
            SESSION_ID: "stub-host-session",
            EXTENSION_SDK_MODULE: pathToFileURL(join(DIST_DIR, "extension.js")).href,
            EXTENSION_ENV_REQUEST: options.requested.join(","),
            EXTENSION_ENV_REQUEST_EMPTY: options.requestEmptyList ? "1" : "",
            EXTENSION_OBSERVE_ENV_NAMES: (options.observe ?? []).join(","),
            EXTENSION_PREJOIN_FILE: prejoinFile,
            EXTENSION_POSTJOIN_FILE: postjoinFile,
            EXTENSION_RESULT_FILE: resultFile,
        },
    });

    const stderr: string[] = [];
    child.stderr!.on("data", (chunk) => stderr.push(String(chunk)));

    let capturedResumeParams: Record<string, unknown> = {};
    const connection = createMessageConnection(
        new StreamMessageReader(child.stdout!),
        new StreamMessageWriter(child.stdin!)
    );
    connection.onRequest("connect", () => ({ protocolVersion: getSdkProtocolVersion() }));
    connection.onRequest("session.resume", (params: Record<string, unknown>) => {
        capturedResumeParams = params;
        if (!options.granted) {
            throw new Error(
                'Extension "env-access" was denied access to sensitive environment variables'
            );
        }
        return { sessionId: params.sessionId, grantedEnvironmentVariables: options.granted };
    });
    // Everything else the SDK issues while joining is irrelevant here, and an
    // unanswered request would hang the join.
    connection.onRequest(() => ({}));
    connection.onNotification(() => {});
    connection.listen();

    try {
        await retry(
            "wait for the fixture extension to report its join result",
            async () => {
                expect(
                    existsSync(resultFile),
                    `extension never reported; stderr: ${stderr.join("")}`
                ).toBe(true);
            },
            300,
            100
        );

        return {
            resumeParams: capturedResumeParams,
            prejoin: readFileSync(prejoinFile, "utf-8"),
            postjoin: readFileSync(postjoinFile, "utf-8"),
            result: readFileSync(resultFile, "utf-8"),
        };
    } finally {
        connection.dispose();
        child.kill();
        // Windows keeps the directory locked until the child is gone.
        await new Promise<void>((resolveExit) => {
            if (child.exitCode !== null || child.signalCode !== null) {
                resolveExit();
                return;
            }
            child.once("exit", () => resolveExit());
        });
        await rm(dir, { recursive: true, force: true, maxRetries: 20, retryDelay: 100 });
    }
}

it("puts an extension's environment request on the wire and applies the grant", async () => {
    const run = await runExtensionAgainstStubHost({
        requested: ["E2E_SDK_TOKEN", "E2E_SDK_OTHER"],
        granted: { E2E_SDK_TOKEN: "granted-token", E2E_SDK_OTHER: "granted-other" },
    });

    expect(run.resumeParams.requestedEnvironmentVariables).toEqual([
        "E2E_SDK_TOKEN",
        "E2E_SDK_OTHER",
    ]);
    // The values crossed the process boundary rather than being inherited.
    expect(run.prejoin).toBe("E2E_SDK_TOKEN=\nE2E_SDK_OTHER=");
    expect(run.postjoin).toBe("E2E_SDK_TOKEN=granted-token\nE2E_SDK_OTHER=granted-other");
    expect(run.result).toBe("joined");
});

it("grants nothing to an extension whose request the host denies", async () => {
    const run = await runExtensionAgainstStubHost({ requested: ["E2E_SDK_TOKEN"] });

    expect(run.resumeParams.requestedEnvironmentVariables).toEqual(["E2E_SDK_TOKEN"]);
    expect(run.result).toContain("denied access to sensitive environment variables");
    expect(run.postjoin).toBe("E2E_SDK_TOKEN=");
});

it("leaves the wire payload alone when an extension asks for nothing", async () => {
    const omitted = await runExtensionAgainstStubHost({ requested: [], granted: {} });
    const emptyList = await runExtensionAgainstStubHost({
        requested: [],
        requestEmptyList: true,
        granted: {},
    });

    expect(omitted.resumeParams).not.toHaveProperty("requestedEnvironmentVariables");
    // An empty list is the other public way to ask for nothing.
    expect(emptyList.resumeParams).not.toHaveProperty("requestedEnvironmentVariables");
    expect(omitted.result).toBe("joined");
    expect(emptyList.result).toBe("joined");
});

it("ignores a granted variable the extension never requested", async () => {
    const run = await runExtensionAgainstStubHost({
        requested: ["E2E_SDK_TOKEN"],
        observe: ["E2E_SDK_SMUGGLED"],
        granted: { E2E_SDK_TOKEN: "granted-token", E2E_SDK_SMUGGLED: "not-approved" },
    });

    // The user approved one name, so a host answering with a second one cannot
    // widen the grant.
    expect(run.postjoin).toBe("E2E_SDK_TOKEN=granted-token\nE2E_SDK_SMUGGLED=");
});

// TODO(PR #2395): Temporarily disable the real-host extension case because this PR transitions
// managed out-of-process SDK launches to the Rust-only flow, which does not yet provide the Node
// extension subprocess lifecycle required for the fixture extension to join.
const extensionHostTestDisabledForRustOnlyFlow = true;
const cliObservations =
    isInProcessTransport || extensionHostTestDisabledForRustOnlyFlow
        ? ""
        : mkdtempSync(join(tmpdir(), "copilot-env-access-cli-"));
const cliResultFile = join(cliObservations, "result");
const cliContext =
    isInProcessTransport || extensionHostTestDisabledForRustOnlyFlow
        ? undefined
        : await createSdkTestContext({
              copilotClientOptions: {
                  env: {
                      COPILOT_CLI_ENABLED_FEATURE_FLAGS: "EXTENSIONS",
                      EXTENSION_ENV_REQUEST: "E2E_SDK_TOKEN",
                      EXTENSION_RESULT_FILE: cliResultFile,
                      EXTENSION_PREJOIN_FILE: join(cliObservations, "prejoin"),
                      EXTENSION_POSTJOIN_FILE: join(cliObservations, "postjoin"),
                  },
              },
          });

// The released CLI ignores `requestedEnvironmentVariables`, so this covers the
// half a real CLI can prove today: asking for variables does not break the join.
// It becomes the grant test once `@github/copilot` carries the host half.
it.skipIf(isInProcessTransport || extensionHostTestDisabledForRustOnlyFlow)(
    "joins a real CLI that does not support environment requests",
    async () => {
        if (!cliContext) {
            throw new Error("Extension E2E requires an out-of-process transport");
        }
        const { workDir, copilotClient } = cliContext;
        const extensionDir = join(workDir, ".github", "extensions", "env-access");
        await rm(join(workDir, ".github"), { recursive: true, force: true });
        await rm(cliResultFile, { force: true });
        await mkdir(extensionDir, { recursive: true });
        await copyFile(FIXTURE, join(extensionDir, "extension.mjs"));
        execFileSync("git", ["init", "--quiet"], { cwd: workDir });

        await using _session = await copilotClient.createSession({
            requestExtensions: true,
            extensionSdkPath: DIST_DIR,
            onPermissionRequest: approveAll,
        });

        await retry(
            "wait for the env-access extension to join the session",
            async () => {
                expect(existsSync(cliResultFile)).toBe(true);
            },
            300,
            100
        );

        expect(readFileSync(cliResultFile, "utf-8")).toBe("joined");
    }
);
