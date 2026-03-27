/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFinished, vi } from "vitest";
import { CopilotClient } from "../../src/client.js";
import { approveAll, type SessionFsConfig } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

/**
 * In-memory session filesystem for testing.
 * Implements the SessionFs handler interface by storing file contents
 * in a nested Map structure (sessionId → path → content).
 * Tracks call counts per operation for test assertions.
 */
class InMemorySessionFs {
    // sessionId → path → content
    private files = new Map<string, Map<string, string>>();
    // sessionId → Set<dirPath>
    private dirs = new Map<string, Set<string>>();
    readonly calls = {
        readFile: 0,
        writeFile: 0,
        appendFile: 0,
        exists: 0,
        stat: 0,
        mkdir: 0,
        readdir: 0,
        rm: 0,
        rename: 0,
    };

    private getSessionFiles(sessionId: string): Map<string, string> {
        let m = this.files.get(sessionId);
        if (!m) {
            m = new Map();
            this.files.set(sessionId, m);
        }
        return m;
    }

    private getSessionDirs(sessionId: string): Set<string> {
        let s = this.dirs.get(sessionId);
        if (!s) {
            s = new Set();
            this.dirs.set(sessionId, s);
        }
        return s;
    }

    /** Derive parent directory from a path (using linux conventions). */
    private parentDir(p: string): string {
        const i = p.lastIndexOf("/");
        return i > 0 ? p.substring(0, i) : "/";
    }

    /** List all entry names directly under a directory path. */
    private entriesUnder(sessionId: string, dirPath: string): string[] {
        const prefix = dirPath.endsWith("/") ? dirPath : dirPath + "/";
        const entries = new Set<string>();

        for (const p of this.getSessionFiles(sessionId).keys()) {
            if (p.startsWith(prefix)) {
                const rest = p.substring(prefix.length);
                const name = rest.split("/")[0];
                if (name) entries.add(name);
            }
        }
        for (const d of this.getSessionDirs(sessionId)) {
            if (d.startsWith(prefix)) {
                const rest = d.substring(prefix.length);
                const name = rest.split("/")[0];
                if (name) entries.add(name);
            }
        }
        return [...entries];
    }

    toConfig(initialCwd: string, sessionStatePath: string): SessionFsConfig {
        return {
            initialCwd,
            sessionStatePath,
            conventions: "linux",
            readFile: async ({ sessionId, path }) => {
                this.calls.readFile++;
                const content = this.getSessionFiles(sessionId).get(path);
                if (content === undefined) {
                    throw new Error(`ENOENT: ${path}`);
                }
                return { content };
            },
            writeFile: async ({ sessionId, path, content }) => {
                this.calls.writeFile++;
                this.getSessionFiles(sessionId).set(path, content);
            },
            appendFile: async ({ sessionId, path, content }) => {
                this.calls.appendFile++;
                const files = this.getSessionFiles(sessionId);
                files.set(path, (files.get(path) ?? "") + content);
            },
            exists: async ({ sessionId, path }) => {
                this.calls.exists++;
                const files = this.getSessionFiles(sessionId);
                const dirs = this.getSessionDirs(sessionId);
                return { exists: files.has(path) || dirs.has(path) };
            },
            stat: async ({ sessionId, path }) => {
                this.calls.stat++;
                const files = this.getSessionFiles(sessionId);
                const dirs = this.getSessionDirs(sessionId);
                const now = new Date().toISOString();

                if (files.has(path)) {
                    return {
                        isFile: true,
                        isDirectory: false,
                        size: Buffer.byteLength(files.get(path)!),
                        mtime: now,
                        birthtime: now,
                    };
                }
                if (dirs.has(path)) {
                    return {
                        isFile: false,
                        isDirectory: true,
                        size: 0,
                        mtime: now,
                        birthtime: now,
                    };
                }
                throw new Error(`ENOENT: ${path}`);
            },
            mkdir: async ({ sessionId, path, recursive }) => {
                this.calls.mkdir++;
                const dirs = this.getSessionDirs(sessionId);
                if (recursive) {
                    // Create all ancestors
                    let current = path;
                    while (current && current !== "/") {
                        dirs.add(current);
                        current = this.parentDir(current);
                    }
                } else {
                    dirs.add(path);
                }
            },
            readdir: async ({ sessionId, path }) => {
                this.calls.readdir++;
                return { entries: this.entriesUnder(sessionId, path) };
            },
            rm: async ({ sessionId, path, recursive }) => {
                this.calls.rm++;
                const files = this.getSessionFiles(sessionId);
                const dirs = this.getSessionDirs(sessionId);
                if (recursive) {
                    const prefix = path.endsWith("/") ? path : path + "/";
                    for (const p of [...files.keys()]) {
                        if (p === path || p.startsWith(prefix)) files.delete(p);
                    }
                    for (const d of [...dirs]) {
                        if (d === path || d.startsWith(prefix)) dirs.delete(d);
                    }
                } else {
                    files.delete(path);
                    dirs.delete(path);
                }
            },
            rename: async ({ sessionId, src, dest }) => {
                this.calls.rename++;
                const files = this.getSessionFiles(sessionId);
                const content = files.get(src);
                if (content !== undefined) {
                    files.delete(src);
                    files.set(dest, content);
                }
            },
        };
    }

    /** Get all file paths for a session. */
    getFilePaths(sessionId: string): string[] {
        return [...(this.files.get(sessionId)?.keys() ?? [])];
    }

    /** Get content of a specific file. */
    getFileContent(sessionId: string, path: string): string | undefined {
        return this.files.get(sessionId)?.get(path);
    }

    /** Check whether any files exist for a given session. */
    hasSession(sessionId: string): boolean {
        const files = this.files.get(sessionId);
        return files !== undefined && files.size > 0;
    }

    /** Get the number of sessions with files. */
    get sessionCount(): number {
        let count = 0;
        for (const files of this.files.values()) {
            if (files.size > 0) count++;
        }
        return count;
    }
}

// These tests require a runtime built with SessionFs support.
// Skip when COPILOT_CLI_PATH is not set (CI uses the published CLI which
// doesn't include this feature yet).
const runTests = process.env.COPILOT_CLI_PATH ? describe : describe.skip;

runTests("Session Fs", async () => {
    const { env } = await createSdkTestContext();

    it("should route file operations through the session fs provider", async () => {
        const fs = new InMemorySessionFs();
        const client1 = new CopilotClient({
            env,
            logLevel: "error",
            cliPath: process.env.COPILOT_CLI_PATH,
            sessionFs: fs.toConfig("/projects/test", "/session-state"),
        });
        onTestFinished(() => client1.forceStop());

        const session = await client1.createSession({
            onPermissionRequest: approveAll,
        });

        // Send a message and wait for the response
        const msg = await session.sendAndWait({ prompt: "What is 100 + 200?" });
        expect(msg?.data.content).toContain("300");

        // Verify file operations were routed through our fs provider.
        // The runtime writes events as JSONL through appendFile/writeFile.
        await vi.waitFor(
            () => {
                const paths = fs.getFilePaths(session.sessionId);
                const hasEvents = paths.some((p) => p.includes("events"));
                expect(hasEvents).toBe(true);
            },
            { timeout: 10_000, interval: 200 },
        );
        expect(fs.calls.writeFile + fs.calls.appendFile).toBeGreaterThan(0);
        expect(fs.calls.mkdir).toBeGreaterThan(0);
    });

    it("should load session data from fs provider on resume", async () => {
        const sessionFs = new InMemorySessionFs();

        const client2 = new CopilotClient({
            env,
            logLevel: "error",
            cliPath: process.env.COPILOT_CLI_PATH,
            sessionFs: sessionFs.toConfig("/projects/test", "/session-state"),
        });
        onTestFinished(() => client2.forceStop());

        // Create a session and send a message
        const session1 = await client2.createSession({
            onPermissionRequest: approveAll,
        });
        const sessionId = session1.sessionId;

        const msg1 = await session1.sendAndWait({ prompt: "What is 50 + 50?" });
        expect(msg1?.data.content).toContain("100");
        await session1.disconnect();

        // Verify readFile is called when resuming (to load events)
        const readCountBefore = sessionFs.calls.readFile;
        const session2 = await client2.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
        });

        expect(sessionFs.calls.readFile).toBeGreaterThan(readCountBefore);

        // Send another message to verify the session is functional
        const msg2 = await session2.sendAndWait({ prompt: "What is that times 3?" });
        expect(msg2?.data.content).toContain("300");
    });

    it("should reject setProvider when sessions already exist", async () => {
        // First client uses TCP so a second client can connect to the same runtime
        const client5 = new CopilotClient({
            env,
            logLevel: "error",
            cliPath: process.env.COPILOT_CLI_PATH,
            useStdio: false,
        });
        onTestFinished(() => client5.forceStop());

        const session = await client5.createSession({
            onPermissionRequest: approveAll,
        });
        await session.sendAndWait({ prompt: "Hello" });

        // Get the port the first client's runtime is listening on
        const port = (client5 as unknown as { actualPort: number }).actualPort;

        // Second client tries to connect with a session fs — should fail
        // because sessions already exist on the runtime.
        const sessionFs = new InMemorySessionFs();
        const client6 = new CopilotClient({
            env,
            logLevel: "error",
            cliUrl: `localhost:${port}`,
            sessionFs: sessionFs.toConfig("/projects/test", "/session-state"),
        });
        onTestFinished(() => client6.forceStop());

        await expect(client6.start()).rejects.toThrow();
    });
});
