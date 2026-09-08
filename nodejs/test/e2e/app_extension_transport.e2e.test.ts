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
const FIXTURE = join(__dirname, "fixtures", "app-extension.mjs");
const DIST_DIR = resolve(__dirname, "..", "..", "dist");

function largeSnapshot(): AppSessionBadgesSnapshot {
    return {
        protocolVersion: 1,
        revision: 8,
        sessions: Array.from({ length: 40 }, (_, index) => ({
            workspaceId: `workspace-${index}`,
            sessionId: `visible-session-${index}`,
            repositoryPath: `C:\\src\\repository-${index}-${"r".repeat(80)}`,
            worktreePath: `C:\\src\\worktree-${index}-${"w".repeat(80)}`,
            branch: `feature/${index}/${"b".repeat(40)}`,
        })),
    };
}

it("authenticates a private app extension and preserves badge notifications after malformed input", async () => {
    const privateModule = join(DIST_DIR, "appExtension.js");
    if (!existsSync(privateModule)) {
        throw new Error(`Built SDK not found at ${DIST_DIR}. Run \`npm run build\` first.`);
    }

    const dir = mkdtempSync(join(tmpdir(), "copilot-app-extension-"));
    const readyFile = join(dir, "ready");
    const snapshotFile = join(dir, "snapshot");
    const errorFile = join(dir, "error");
    const batchSentFile = join(dir, "batch-sent");
    const badgeUpdates = [
        {
            workspaceId: "workspace-0",
            sessionId: "visible-session-0",
            badge: { state: "open", label: "Open" },
        },
        {
            workspaceId: "workspace-1",
            sessionId: "visible-session-1",
            badge: null,
        },
    ];
    const child = spawn(process.execPath, [FIXTURE], {
        stdio: ["pipe", "pipe", "pipe"],
        env: {
            ...process.env,
            SESSION_ID: "hidden-app-session",
            APP_EXTENSION_SDK_MODULE: pathToFileURL(privateModule).href,
            APP_EXTENSION_READY_FILE: readyFile,
            APP_EXTENSION_SNAPSHOT_FILE: snapshotFile,
            APP_EXTENSION_ERROR_FILE: errorFile,
            APP_EXTENSION_BATCH_SENT_FILE: batchSentFile,
            APP_EXTENSION_BADGE_UPDATES: JSON.stringify(badgeUpdates),
        },
    });
    const stderr: string[] = [];
    child.stderr!.on("data", (chunk) => stderr.push(String(chunk)));

    const connection = createMessageConnection(
        new StreamMessageReader(child.stdout!),
        new StreamMessageWriter(child.stdin!)
    );
    const requests: Array<{ method: string; params: unknown }> = [];
    connection.onRequest("connect", () => ({ protocolVersion: getSdkProtocolVersion() }));
    connection.onRequest("session.resume", (params: Record<string, unknown>) => ({
        sessionId: params.sessionId,
    }));
    connection.onRequest("extensions.appExtension.register", (params: unknown) => {
        requests.push({ method: "extensions.appExtension.register", params });
        return {
            protocolVersion: 1,
            principal: {
                packageId: "bundled:github-app:badges",
                activationId: "activation-stdio",
            },
            capabilities: { sessionBadges: true },
        };
    });
    connection.onRequest("extensions.appSessionBadges.register", () => {
        requests.push({ method: "extensions.appSessionBadges.register", params: undefined });
        return null;
    });
    connection.onRequest("extensions.appSessionBadges.setBadges", (params: unknown) => {
        requests.push({ method: "extensions.appSessionBadges.setBadges", params });
        return null;
    });
    connection.onRequest(() => ({}));
    connection.onNotification(() => {});
    connection.listen();

    const snapshot = largeSnapshot();
    try {
        await retry(
            "wait for private app extension registration",
            async () => {
                expect(
                    existsSync(readyFile),
                    `extension did not become ready; error: ${
                        existsSync(errorFile) ? readFileSync(errorFile, "utf8") : ""
                    }; stderr: ${stderr.join("")}`
                ).toBe(true);
                expect(requests[0]).toEqual({
                    method: "extensions.appExtension.register",
                    params: { protocolVersion: 1 },
                });
                expect(requests[1]?.method).toBe("extensions.appSessionBadges.register");
                expect(requests[1]?.params ?? null).toBeNull();
            },
            100,
            50
        );

        await connection.sendNotification("appSessionBadges.snapshot", {
            protocolVersion: 1,
            revision: 7,
            sessions: [
                {
                    workspace_id: "incorrect",
                    session_id: "incorrect",
                    repository_path: "C:\\src\\repository",
                    worktree_path: "C:\\src\\worktree",
                },
            ],
        });
        await connection.sendNotification("appSessionBadges.snapshot", snapshot);

        await retry(
            "wait for post-notification badge batch",
            async () => {
                expect(existsSync(batchSentFile), `stderr: ${stderr.join("")}`).toBe(true);
                expect(requests.at(-1)).toEqual({
                    method: "extensions.appSessionBadges.setBadges",
                    params: { protocolVersion: 1, updates: badgeUpdates },
                });
            },
            100,
            50
        );

        const activation = JSON.parse(readFileSync(readyFile, "utf8")) as {
            principal: object;
            hostKeys: string[];
            contributionKeys: string[];
        };
        expect(activation).toEqual({
            principal: {
                packageId: "bundled:github-app:badges",
                activationId: "activation-stdio",
            },
            hostKeys: ["capabilities", "principal", "sessionBadges", "signal"],
            contributionKeys: ["identity"],
        });
        const delivered = readFileSync(snapshotFile, "utf8")
            .trim()
            .split("\n")
            .map((line) => JSON.parse(line) as { snapshot: AppSessionBadgesSnapshot });
        expect(delivered).toHaveLength(1);
        expect(delivered[0]!.snapshot).toEqual(snapshot);
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
