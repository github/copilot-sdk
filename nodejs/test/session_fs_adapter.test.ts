/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { MemoryProvider } from "@platformatic/vfs";
import { describe, expect, it } from "vitest";
import {
    createSessionFsAdapter,
    SessionFsWriteFailure,
    type SessionFsProvider,
} from "../src/index.js";

describe("SessionFsAdapter", () => {
    it("should map all sessionFs handler operations", async () => {
        const memoryProvider = new MemoryProvider();
        const sessionId = "handler-session";
        const sp = (path: string) => `/${sessionId}${path.startsWith("/") ? path : "/" + path}`;

        const provider: SessionFsProvider = {
            async readFile(path) {
                return (await memoryProvider.readFile(sp(path), "utf8")) as string;
            },
            async readFileBytes(path) {
                return memoryProvider.readFile(sp(path));
            },
            async writeFileBytes(path, content) {
                await memoryProvider.writeFile(sp(path), content);
            },
            async writeFile(path, content) {
                await memoryProvider.writeFile(sp(path), content);
            },
            async appendFile(path, content) {
                await memoryProvider.appendFile(sp(path), content);
            },
            async exists(path) {
                return memoryProvider.exists(sp(path));
            },
            async stat(path) {
                const st = await memoryProvider.stat(sp(path));
                return {
                    isFile: st.isFile(),
                    isDirectory: st.isDirectory(),
                    size: st.size,
                    mtime: new Date(st.mtimeMs).toISOString(),
                    birthtime: new Date(st.birthtimeMs).toISOString(),
                };
            },
            async mkdir(path, recursive, mode) {
                await memoryProvider.mkdir(sp(path), { recursive, mode });
            },
            async readdir(path) {
                return (await memoryProvider.readdir(sp(path))) as string[];
            },
            async readdirWithTypes(path) {
                const names = (await memoryProvider.readdir(sp(path))) as string[];
                return Promise.all(
                    names.map(async (name) => {
                        const st = await memoryProvider.stat(sp(`${path}/${name}`));
                        return {
                            name,
                            type: st.isDirectory() ? ("directory" as const) : ("file" as const),
                        };
                    })
                );
            },
            async rm(path) {
                await memoryProvider.unlink(sp(path));
            },
            async rename(src, dest) {
                await memoryProvider.rename(sp(src), sp(dest));
            },
            sqlite: {
                async query(queryType, query, params) {
                    return {
                        columns: ["sessionId", "query", "queryType", "answer"],
                        rows: [{ sessionId, query, queryType, answer: params?.answer }],
                        rowsAffected: 0,
                    };
                },
                async transaction(statements) {
                    return statements.map((statement) => ({
                        columns: ["sessionId", "query", "queryType", "answer"],
                        rows: [
                            {
                                sessionId,
                                query: statement.query,
                                queryType: statement.queryType,
                                answer: statement.params?.answer,
                            },
                        ],
                        rowsAffected: 0,
                    }));
                },
                async exists() {
                    return true;
                },
            },
        };

        const handler = createSessionFsAdapter(provider);

        const mkdirError = await handler.mkdir({
            sessionId,
            path: "/workspace/nested",
            recursive: true,
        });
        expect(mkdirError).toBeUndefined();

        const writeError = await handler.writeFile({
            sessionId,
            path: "/workspace/nested/file.txt",
            content: "hello",
        });
        expect(writeError).toBeUndefined();

        const appendError = await handler.appendFile({
            sessionId,
            path: "/workspace/nested/file.txt",
            content: " world",
        });
        expect(appendError).toBeUndefined();

        const exists = await handler.exists({ sessionId, path: "/workspace/nested/file.txt" });
        expect(exists.exists).toBe(true);

        const stat = await handler.stat({ sessionId, path: "/workspace/nested/file.txt" });
        expect(stat.isFile).toBe(true);
        expect(stat.isDirectory).toBe(false);
        expect(stat.size).toBe("hello world".length);
        expect(stat.error).toBeUndefined();

        const content = await handler.readFile({
            sessionId,
            path: "/workspace/nested/file.txt",
        });
        expect(content.content).toBe("hello world");
        expect(content.error).toBeUndefined();

        await memoryProvider.writeFile(
            sp("/workspace/nested/photo.png"),
            Buffer.from([0, 255, 254, 1])
        );
        const binary = await handler.readFileBytes({
            sessionId,
            path: "/workspace/nested/photo.png",
        });
        expect(binary.content).toBe("AP/+AQ==");
        expect(binary.error).toBeUndefined();
        const written = await handler.writeFileBytes({
            sessionId,
            path: "/workspace/nested/written.bin",
            content: "AP/+AQ==",
            mode: 0o600,
        });
        expect(written).toBeUndefined();
        expect(await memoryProvider.readFile(sp("/workspace/nested/written.bin"))).toEqual(
            Buffer.from([0, 255, 254, 1])
        );
        expect(
            await handler.writeFileBytes({
                sessionId,
                path: "/workspace/nested/invalid.bin",
                content: "AA==AAAA",
            })
        ).toMatchObject({
            code: "UNKNOWN",
            message: "invalid sessionFs.writeFileBytes base64 content",
        });
        expect(await memoryProvider.exists(sp("/workspace/nested/invalid.bin"))).toBe(false);
        expect(
            await handler.writeFileBytes({
                sessionId,
                path: "/workspace/nested/large.bin",
                content: "A".repeat(64 * 1024 * 1024),
            })
        ).toMatchObject({
            code: "UNKNOWN",
            message: "sessionFs.writeFileBytes content exceeds the binary write limit",
        });
        expect(await memoryProvider.exists(sp("/workspace/nested/large.bin"))).toBe(false);
        const missingBinary = await createSessionFsAdapter({
            ...provider,
            readFileBytes: undefined,
        }).readFileBytes({
            sessionId,
            path: "/workspace/nested/photo.png",
        });
        expect(missingBinary.error?.code).toBe("UNKNOWN");
        expect(
            await createSessionFsAdapter({ ...provider, writeFileBytes: undefined }).writeFileBytes(
                {
                    sessionId,
                    path: "/workspace/nested/missing.bin",
                    content: "AP/+AQ==",
                }
            )
        ).toMatchObject({ code: "UNKNOWN" });

        const oversized = await createSessionFsAdapter({
            ...provider,
            readFileBytes: async () => new Uint8Array(((64 * 1024 * 1024 - 1024) / 4) * 3 + 1),
        }).readFileBytes({ sessionId, path: "/workspace/nested/photo.png" });
        expect(oversized.content).toBe("");
        expect(oversized.error).toEqual({
            code: "UNKNOWN",
            message: "sessionFs.readFileBytes content exceeds the binary read limit",
        });

        const entries = await handler.readdir({ sessionId, path: "/workspace/nested" });
        expect(entries.entries).toContain("file.txt");
        expect(entries.error).toBeUndefined();

        const typedEntries = await handler.readdirWithTypes({
            sessionId,
            path: "/workspace/nested",
        });
        expect(
            typedEntries.entries.some((entry) => entry.name === "file.txt" && entry.type === "file")
        ).toBe(true);
        expect(typedEntries.error).toBeUndefined();

        const renameError = await handler.rename({
            sessionId,
            src: "/workspace/nested/file.txt",
            dest: "/workspace/nested/renamed.txt",
        });
        expect(renameError).toBeUndefined();

        const oldPath = await handler.exists({
            sessionId,
            path: "/workspace/nested/file.txt",
        });
        expect(oldPath.exists).toBe(false);

        const renamedPath = await handler.readFile({
            sessionId,
            path: "/workspace/nested/renamed.txt",
        });
        expect(renamedPath.content).toBe("hello world");

        const rmError = await handler.rm({
            sessionId,
            path: "/workspace/nested/renamed.txt",
        });
        expect(rmError).toBeUndefined();

        const removed = await handler.exists({
            sessionId,
            path: "/workspace/nested/renamed.txt",
        });
        expect(removed.exists).toBe(false);

        const missing = await handler.stat({
            sessionId,
            path: "/workspace/nested/missing.txt",
        });
        expect(missing.error?.code).toBe("ENOENT");

        const sqliteResult = await handler.sqliteQuery({
            sessionId,
            query: "select :answer as answer",
            queryType: "query",
            params: { answer: 42 },
        });
        expect(sqliteResult.columns).toContain("answer");
        expect(sqliteResult.rows[0]).toMatchObject({
            sessionId,
            query: "select :answer as answer",
            queryType: "query",
            answer: 42,
        });
        expect(sqliteResult.rowsAffected).toBe(0);
        expect(sqliteResult.error).toBeUndefined();

        const sqliteExists = await handler.sqliteExists({ sessionId });
        expect(sqliteExists.exists).toBe(true);
    });

    it("converts provider exceptions to rpc errors", async () => {
        function makeError(message: string, code?: string): Error {
            const err = new Error(message) as Error & { code?: string };
            if (code) {
                err.code = code;
            }
            return err;
        }

        function makeThrowingProvider(error: Error): SessionFsProvider {
            return {
                readFile: () => Promise.reject(error),
                writeFile: () => Promise.reject(error),
                appendFile: () => Promise.reject(error),
                exists: () => Promise.reject(error),
                stat: () => Promise.reject(error),
                mkdir: () => Promise.reject(error),
                readdir: () => Promise.reject(error),
                readdirWithTypes: () => Promise.reject(error),
                rm: () => Promise.reject(error),
                rename: () => Promise.reject(error),
                sqlite: {
                    query: () => Promise.reject(error),
                    transaction: () => Promise.reject(error),
                    exists: () => Promise.reject(error),
                },
            };
        }

        const enoent = makeError("missing file", "ENOENT");
        const handler = createSessionFsAdapter(makeThrowingProvider(enoent));
        const sessionId = "throw-session";

        function assertEnoent(error: { code: string; message: string } | undefined) {
            expect(error).toBeDefined();
            expect(error!.code).toBe("ENOENT");
            expect(error!.message.toLowerCase()).toContain("missing");
        }

        assertEnoent((await handler.readFile({ sessionId, path: "missing.txt" })).error);
        assertEnoent(
            await handler.writeFile({ sessionId, path: "missing.txt", content: "content" })
        );
        assertEnoent(
            await handler.appendFile({ sessionId, path: "missing.txt", content: "content" })
        );

        const exists = await handler.exists({ sessionId, path: "missing.txt" });
        expect(exists.exists).toBe(false);

        assertEnoent((await handler.stat({ sessionId, path: "missing.txt" })).error);
        assertEnoent(await handler.mkdir({ sessionId, path: "missing-dir" }));
        assertEnoent((await handler.readdir({ sessionId, path: "missing-dir" })).error);
        assertEnoent((await handler.readdirWithTypes({ sessionId, path: "missing-dir" })).error);
        assertEnoent(await handler.rm({ sessionId, path: "missing.txt" }));
        assertEnoent(await handler.rename({ sessionId, src: "missing.txt", dest: "dest.txt" }));

        // sqlite methods let errors propagate (no try/catch wrapping)
        await expect(
            handler.sqliteQuery({ sessionId, query: "select 1", queryType: "query" })
        ).rejects.toThrow("missing file");
        await expect(handler.sqliteExists({ sessionId })).rejects.toThrow("missing file");

        // sqliteTransaction reports a classified result-level error instead
        const transaction = await handler.sqliteTransaction({
            sessionId,
            statements: [{ query: "select 1", queryType: "query" }],
        });
        expect(transaction.results).toEqual([]);
        expect(transaction.error).toEqual({ errorClass: "fatal", message: "missing file" });

        const unknownProvider = createSessionFsAdapter(makeThrowingProvider(makeError("bad path")));
        const unknownError = await unknownProvider.writeFile({
            sessionId,
            path: "bad.txt",
            content: "content",
        });
        expect(unknownError).toBeDefined();
        expect(unknownError!.code).toBe("UNKNOWN");

        const changedProvider = createSessionFsAdapter(
            makeThrowingProvider(new SessionFsWriteFailure("write truncated the target"))
        );
        expect(
            await changedProvider.writeFile({ sessionId, path: "changed.txt", content: "content" })
        ).toEqual({
            code: "UNKNOWN",
            message: "write truncated the target",
            writeChanged: true,
        });
        expect(
            await changedProvider.appendFile({ sessionId, path: "changed.txt", content: "content" })
        ).not.toHaveProperty("writeChanged");
        expect(
            await handler.writeFile({ sessionId, path: "missing.txt", content: "content" })
        ).not.toHaveProperty("writeChanged");
    });
});
