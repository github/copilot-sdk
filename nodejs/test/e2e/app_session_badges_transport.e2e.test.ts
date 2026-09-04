/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawn } from "node:child_process";
import { existsSync, mkdtempSync, readFileSync } from "node:fs";
import { rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { expect, it } from "vitest";
import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { getSdkProtocolVersion } from "../../src/sdkProtocolVersion.js";
import { retry } from "./harness/sdkTestHelper.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const FIXTURE = join(__dirname, "fixtures", "app-session-badges-extension.mjs");
const DIST_DIR = resolve(__dirname, "..", "..", "dist");

it("delivers a later app-session badge snapshot over extension stdio", async () => {
    if (!existsSync(join(DIST_DIR, "extension.js"))) {
        throw new Error(`Built SDK not found at ${DIST_DIR}. Run \`npm run build\` first.`);
    }

    const dir = mkdtempSync(join(tmpdir(), "copilot-app-session-badges-"));
    const readyFile = join(dir, "ready");
    const snapshotFile = join(dir, "snapshot");
    const errorFile = join(dir, "error");
    const child = spawn(process.execPath, [FIXTURE], {
        stdio: ["pipe", "pipe", "pipe"],
        env: {
            ...process.env,
            SESSION_ID: "hidden-app-session",
            EXTENSION_SDK_MODULE: pathToFileURL(join(DIST_DIR, "extension.js")).href,
            EXTENSION_READY_FILE: readyFile,
            EXTENSION_SNAPSHOT_FILE: snapshotFile,
            EXTENSION_ERROR_FILE: errorFile,
        },
    });
    const stderr: string[] = [];
    child.stderr!.on("data", (chunk) => stderr.push(String(chunk)));

    const connection = createMessageConnection(
        new StreamMessageReader(child.stdout!),
        new StreamMessageWriter(child.stdin!)
    );
    let registered = false;
    connection.onRequest("connect", () => ({ protocolVersion: getSdkProtocolVersion() }));
    connection.onRequest("session.resume", (params: Record<string, unknown>) => ({
        sessionId: params.sessionId,
    }));
    connection.onRequest("extensions.appSessionBadges.register", () => {
        registered = true;
        return null;
    });
    connection.onRequest(() => ({}));
    connection.onNotification(() => {});
    connection.listen();

    const snapshot = {
        protocolVersion: 1,
        revision: 4,
        sessions: [
            {
                workspaceId: "workspace-1",
                sessionId: "visible-session-1",
                repositoryPath: "C:\\src\\repo",
                worktreePath: "C:\\src\\worktree",
                branch: "feature",
            },
        ],
    };

    try {
        await retry(
            "wait for the app-session badge extension to register",
            async () => {
                expect(
                    existsSync(readyFile),
                    `extension did not become ready; error: ${
                        existsSync(errorFile) ? readFileSync(errorFile, "utf8") : ""
                    }; stderr: ${stderr.join("")}`
                ).toBe(true);
                expect(registered).toBe(true);
            },
            100,
            50
        );

        await connection.sendNotification("appSessionBadges.snapshot", snapshot);

        await retry(
            "wait for the later snapshot callback",
            async () => {
                expect(
                    existsSync(snapshotFile),
                    `snapshot callback did not fire; stderr: ${stderr.join("")}`
                ).toBe(true);
            },
            100,
            50
        );
        expect(JSON.parse(readFileSync(snapshotFile, "utf8"))).toEqual(snapshot);
    } finally {
        connection.dispose();
        child.kill();
        await new Promise<void>((resolveExit) => {
            if (child.exitCode !== null || child.signalCode !== null) {
                resolveExit();
                return;
            }
            child.once("exit", () => resolveExit());
        });
        await rm(dir, { recursive: true, force: true, maxRetries: 20, retryDelay: 100 });
    }
});
