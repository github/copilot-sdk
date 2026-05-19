/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { DatabaseSync } from "node:sqlite";
import { MemoryProvider, VirtualProvider } from "@platformatic/vfs";
import { mkdtempSync, realpathSync } from "fs";
import { tmpdir } from "os";
import { join } from "path";
import { describe, expect, it } from "vitest";
import type { SessionFsReaddirWithTypesEntry } from "../../src/generated/rpc.js";
import {
    approveAll,
    CopilotSession,
    SessionEvent,
    type SessionFsConfig,
    type SessionFsProvider,
    type SessionFsFileInfo,
    type SessionFsSqliteQueryResult,
    type SessionFsSqliteQueryType,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const sessionStatePath =
    process.platform === "win32"
        ? "/session-state"
        : join(
              realpathSync(mkdtempSync(join(tmpdir(), "copilot-sqlite-state-"))),
              "session-state"
          ).replace(/\\/g, "/");

const sessionFsConfig: SessionFsConfig = {
    initialCwd: "/",
    sessionStatePath,
    conventions: "posix",
    capabilities: { sqlite: true },
};

describe("Session Fs SQLite", async () => {
    const provider = new MemoryProvider();
    /** Track which queries were received, per session */
    const sqliteCalls: { sessionId: string; queryType: string; query: string }[] = [];
    /** Per-session SQLite databases, keyed by session ID.
     *  Stored at describe scope so the database survives if the CLI
     *  re-creates the handler (e.g., on reconnect). */
    const sessionDbs = new Map<string, DatabaseSync>();

    const createSessionFsHandler = (session: CopilotSession) =>
        createTestSessionFsHandlerWithSqlite(session, provider, sqliteCalls, sessionDbs);

    // Helpers to build session-namespaced paths for direct provider assertions
    const p = (sessionId: string, path: string) =>
        `/${sessionId}${path.startsWith("/") ? path : "/" + path}`;

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: { sessionFs: sessionFsConfig },
    });

    it(
        "should route SQL queries through the sessionFs sqlite handler",
        { timeout: 60000 },
        async () => {
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                createSessionFsHandler,
            });

            // Ask the agent to create a table and insert data using the SQL tool
            await session.sendAndWait({
                prompt:
                    'Use the sql tool to create a table called "items" with columns id (TEXT PRIMARY KEY) and name (TEXT). ' +
                    'Then insert a row with id "a1" and name "Widget".',
            });

            // Verify the sqlite handler was called with the right operations
            const sessionCalls = sqliteCalls.filter((c) => c.sessionId === session.sessionId);
            expect(sessionCalls.length).toBeGreaterThan(0);
            expect(sessionCalls.some((c) => c.query.toUpperCase().includes("CREATE TABLE"))).toBe(
                true
            );
            expect(sessionCalls.some((c) => c.query.toUpperCase().includes("INSERT"))).toBe(true);

            // Verify queryType is set correctly
            expect(sessionCalls.some((c) => c.queryType === "exec")).toBe(true);
            expect(sessionCalls.some((c) => c.queryType === "run")).toBe(true);

            await session.disconnect();
        }
    );

    it(
        "should allow subagents to use SQL tool via inherited sessionFs",
        { timeout: 60000 },
        async () => {
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                createSessionFsHandler,
            });

            const events: SessionEvent[] = [];
            session.on((event) => {
                events.push(event);
            });

            // Ask the agent to use the task tool to spawn a subagent that uses SQL
            await session.sendAndWait({
                prompt:
                    "Use the task tool to ask a task agent to do the following: " +
                    "Use the sql tool to run this query: INSERT INTO todos (id, title, status) VALUES ('subagent-test', 'Created by subagent', 'done')",
            });

            await session.disconnect();

            // Verify that the subagent's SQL queries were routed through the sessionFs sqlite handler
            const sessionCalls = sqliteCalls.filter((c) => c.sessionId === session.sessionId);
            const insertCalls = sessionCalls.filter((c) =>
                c.query.toUpperCase().includes("INSERT")
            );
            expect(insertCalls.length).toBeGreaterThan(0);

            // Verify that the sql tool execution in events.jsonl came from the subagent (has agentId)
            const buf = await provider.readFile(
                p(session.sessionId, `${sessionStatePath}/events.jsonl`)
            );
            const content = buf.toString("utf8");
            const lines = content.split("\n").filter(Boolean);
            const parsed = lines.map((line) => JSON.parse(line));
            const sqlToolEvents = parsed.filter(
                (e: { type?: string; data?: { toolName?: string } }) =>
                    e.type === "tool.execution_start" && e.data?.toolName === "sql"
            );
            expect(sqlToolEvents.length).toBeGreaterThan(0);
            expect(sqlToolEvents.every((e: { agentId?: string }) => !!e.agentId)).toBe(true);
        }
    );
});

function createTestSessionFsHandlerWithSqlite(
    session: CopilotSession,
    provider: VirtualProvider,
    sqliteCalls: { sessionId: string; queryType: string; query: string }[],
    sessionDbs: Map<string, DatabaseSync>
): SessionFsProvider {
    const sp = (path: string) => `/${session.sessionId}${path.startsWith("/") ? path : "/" + path}`;

    function getOrCreateDb(): DatabaseSync {
        let db = sessionDbs.get(session.sessionId);
        if (!db) {
            db = new DatabaseSync(":memory:");
            db.exec("PRAGMA busy_timeout = 5000");
            sessionDbs.set(session.sessionId, db);
        }
        return db;
    }

    return {
        async readFile(path: string): Promise<string> {
            return (await provider.readFile(sp(path), "utf8")) as string;
        },
        async writeFile(path: string, content: string): Promise<void> {
            await provider.writeFile(sp(path), content);
        },
        async appendFile(path: string, content: string): Promise<void> {
            await provider.appendFile(sp(path), content);
        },
        async exists(path: string): Promise<boolean> {
            return provider.exists(sp(path));
        },
        async stat(path: string): Promise<SessionFsFileInfo> {
            const st = await provider.stat(sp(path));
            return {
                isFile: st.isFile(),
                isDirectory: st.isDirectory(),
                size: st.size,
                mtime: new Date(st.mtimeMs).toISOString(),
                birthtime: new Date(st.birthtimeMs).toISOString(),
            };
        },
        async mkdir(path: string, recursive: boolean, mode?: number): Promise<void> {
            await provider.mkdir(sp(path), { recursive, mode });
        },
        async readdir(path: string): Promise<string[]> {
            return (await provider.readdir(sp(path))) as string[];
        },
        async readdirWithTypes(path: string): Promise<SessionFsReaddirWithTypesEntry[]> {
            const names = (await provider.readdir(sp(path))) as string[];
            return Promise.all(
                names.map(async (name) => {
                    const st = await provider.stat(sp(`${path}/${name}`));
                    return {
                        name,
                        type: st.isDirectory() ? ("directory" as const) : ("file" as const),
                    };
                })
            );
        },
        async rm(path: string): Promise<void> {
            await provider.unlink(sp(path));
        },
        async rename(src: string, dest: string): Promise<void> {
            await provider.rename(sp(src), sp(dest));
        },
        sqlite: {
            async query(
                queryType: SessionFsSqliteQueryType,
                query: string,
                params?: Record<string, string | number | null>
            ): Promise<SessionFsSqliteQueryResult | undefined> {
                sqliteCalls.push({ sessionId: session.sessionId, queryType, query });

                const database = getOrCreateDb();
                const trimmed = query.trim();
                if (trimmed.length === 0) {
                    return undefined;
                }

                switch (queryType) {
                    case "exec":
                        database.exec(trimmed);
                        return undefined;

                    case "query": {
                        const stmt = database.prepare(trimmed);
                        const rows = (params ? stmt.all(params) : stmt.all()) as Record<
                            string,
                            unknown
                        >[];
                        const columns = rows.length > 0 ? Object.keys(rows[0]) : [];
                        return { rows, columns, rowsAffected: 0 };
                    }

                    case "run": {
                        const stmt = database.prepare(trimmed);
                        const result = params ? stmt.run(params) : stmt.run();
                        return {
                            rows: [],
                            columns: [],
                            rowsAffected: Number(result.changes),
                            lastInsertRowid:
                                result.lastInsertRowid !== undefined
                                    ? Number(result.lastInsertRowid)
                                    : undefined,
                        };
                    }
                }
            },
            async exists(): Promise<boolean> {
                return sessionDbs.has(session.sessionId);
            },
        },
    };
}
