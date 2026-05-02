/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { MemoryProvider } from "@platformatic/vfs";
import { describe, expect, it } from "vitest";
import { createSessionFsAdapter, type SessionFsProvider } from "../src/index.js";

describe("SessionFsAdapter", () => {
    it("should map all sessionFs handler operations", async () => {
        const memoryProvider = new MemoryProvider();
        const sessionId = "handler-session";
        const sp = (path: string) => `/${sessionId}${path.startsWith("/") ? path : "/" + path}`;

        const provider: SessionFsProvider = {
            async readFile(path) {
                return (await memoryProvider.readFile(sp(path), "utf8")) as string;
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

        const unknownProvider = createSessionFsAdapter(makeThrowingProvider(makeError("bad path")));
        const unknownError = await unknownProvider.writeFile({
            sessionId,
            path: "bad.txt",
            content: "content",
        });
        expect(unknownError).toBeDefined();
        expect(unknownError!.code).toBe("UNKNOWN");
    });
});
