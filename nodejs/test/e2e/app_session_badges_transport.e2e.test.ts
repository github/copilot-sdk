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
import type { AppSessionBadgesSnapshot } from "../../src/appSessionBadges.js";
import { getSdkProtocolVersion } from "../../src/sdkProtocolVersion.js";
import { retry } from "./harness/sdkTestHelper.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const FIXTURE = join(__dirname, "fixtures", "app-session-badges-extension.mjs");
const DIST_DIR = resolve(__dirname, "..", "..", "dist");

function encodeNotification(snapshot: AppSessionBadgesSnapshot): Buffer {
    const body = Buffer.from(
        JSON.stringify({
            jsonrpc: "2.0",
            method: "appSessionBadges.snapshot",
            params: snapshot,
        })
    );
    return Buffer.concat([Buffer.from(`Content-Length: ${body.byteLength}\r\n\r\n`), body]);
}

function readSnapshots(path: string): AppSessionBadgesSnapshot[] {
    if (!existsSync(path)) {
        return [];
    }
    return readFileSync(path, "utf8")
        .trim()
        .split("\n")
        .filter(Boolean)
        .map((line) => JSON.parse(line) as AppSessionBadgesSnapshot);
}

function largeSnapshot(revision: number): AppSessionBadgesSnapshot {
    return {
        protocolVersion: 1,
        revision,
        sessions: Array.from({ length: 40 }, (_, index) => ({
            workspaceId: `workspace-${revision}-${index}`,
            sessionId: `visible-session-${revision}-${index}`,
            repositoryPath: `C:\\src\\repository-${index}-${"r".repeat(80)}`,
            worktreePath: `C:\\src\\worktree-${index}-${"w".repeat(80)}`,
            branch: `feature/${revision}/${index}/${"b".repeat(40)}`,
        })),
    };
}

function snakeCaseSnapshot(revision: number): object {
    return {
        protocolVersion: 1,
        revision,
        sessions: [
            {
                workspace_id: "workspace-incorrect",
                session_id: "visible-session-incorrect",
                repository_path: "C:\\src\\repo",
                worktree_path: "C:\\src\\worktree",
            },
        ],
    };
}

it("delivers sequential, coalesced, and fragmented large snapshots over extension stdio", async () => {
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

    const initialSnapshot: AppSessionBadgesSnapshot = {
        protocolVersion: 1,
        revision: 4,
        sessions: [],
    };
    const snapshots = [largeSnapshot(5), largeSnapshot(6), largeSnapshot(7)];

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

        await connection.sendNotification("appSessionBadges.snapshot", initialSnapshot);
        await connection.sendNotification("appSessionBadges.snapshot", snakeCaseSnapshot(5));

        const coalesced = Buffer.concat([
            encodeNotification(snapshots[0]!),
            encodeNotification(snapshots[1]!),
        ]);
        child.stdin!.write(coalesced);

        const fragmented = encodeNotification(snapshots[2]!);
        for (let offset = 0; offset < fragmented.byteLength; offset += 97) {
            child.stdin!.write(fragmented.subarray(offset, offset + 97));
        }

        await retry(
            "wait for every later snapshot callback",
            async () => {
                expect(
                    readSnapshots(snapshotFile),
                    `not every snapshot callback fired; stderr: ${stderr.join("")}`
                ).toHaveLength(4);
            },
            100,
            50
        );
        expect(readSnapshots(snapshotFile)).toEqual([initialSnapshot, ...snapshots]);
        expect(encodeNotification(snapshots[0]!).byteLength).toBeGreaterThan(11_000);
        expect(stderr.join("")).toContain("Invalid app session badges snapshot ignored");
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
