/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    SessionFsHandler,
    SessionFsError,
    SessionFsStatResult,
    SessionFsReaddirWithTypesEntry,
} from "./generated/rpc.js";

/**
 * File metadata returned by {@link SessionFsProvider.stat}.
 * Same shape as the generated {@link SessionFsStatResult} but without the
 * `error` field, since providers signal errors by throwing.
 */
export type SessionFsFileInfo = Omit<SessionFsStatResult, "error">;

/**
 * Interface for session filesystem providers. Implementors use idiomatic
 * TypeScript patterns: throw on error, return values directly. Use
 * {@link createSessionFsAdapter} to convert a provider into the
 * {@link SessionFsHandler} expected by the SDK.
 *
 * Errors with a `code` property of `"ENOENT"` are mapped to the ENOENT
 * error code; all others map to UNKNOWN.
 */
export interface SessionFsProvider {
    /** Reads the full content of a file. Throw if the file does not exist. */
    readFile(path: string): Promise<string>;

    /** Writes content to a file, creating parent directories if needed. */
    writeFile(path: string, content: string, mode?: number): Promise<void>;

    /** Appends content to a file, creating parent directories if needed. */
    appendFile(path: string, content: string, mode?: number): Promise<void>;

    /** Checks whether a path exists. */
    exists(path: string): Promise<boolean>;

    /** Gets metadata about a file or directory. Throw if it does not exist. */
    stat(path: string): Promise<SessionFsFileInfo>;

    /** Creates a directory. If recursive is true, creates parents as needed. */
    mkdir(path: string, recursive: boolean, mode?: number): Promise<void>;

    /** Lists entry names in a directory. Throw if it does not exist. */
    readdir(path: string): Promise<string[]>;

    /** Lists entries with type info. Throw if the directory does not exist. */
    readdirWithTypes(path: string): Promise<SessionFsReaddirWithTypesEntry[]>;

    /** Removes a file or directory. If force is true, do not throw on ENOENT. */
    rm(path: string, recursive: boolean, force: boolean): Promise<void>;

    /** Renames/moves a file or directory. */
    rename(src: string, dest: string): Promise<void>;
}

/**
 * Wraps a {@link SessionFsProvider} into the {@link SessionFsHandler}
 * interface expected by the SDK, converting thrown errors into
 * {@link SessionFsError} results.
 */
export function createSessionFsAdapter(provider: SessionFsProvider): SessionFsHandler {
    return {
        readFile: async ({ path }) => {
            try {
                const content = await provider.readFile(path);
                return { content };
            } catch (err) {
                return { content: "", error: toSessionFsError(err) };
            }
        },
        writeFile: async ({ path, content, mode }) => {
            try {
                await provider.writeFile(path, content, mode);
                return undefined;
            } catch (err) {
                return toSessionFsError(err);
            }
        },
        appendFile: async ({ path, content, mode }) => {
            try {
                await provider.appendFile(path, content, mode);
                return undefined;
            } catch (err) {
                return toSessionFsError(err);
            }
        },
        exists: async ({ path }) => {
            try {
                return { exists: await provider.exists(path) };
            } catch {
                return { exists: false };
            }
        },
        stat: async ({ path }) => {
            try {
                return await provider.stat(path);
            } catch (err) {
                return {
                    isFile: false,
                    isDirectory: false,
                    size: 0,
                    mtime: new Date().toISOString(),
                    birthtime: new Date().toISOString(),
                    error: toSessionFsError(err),
                };
            }
        },
        mkdir: async ({ path, recursive, mode }) => {
            try {
                await provider.mkdir(path, recursive ?? false, mode);
                return undefined;
            } catch (err) {
                return toSessionFsError(err);
            }
        },
        readdir: async ({ path }) => {
            try {
                const entries = await provider.readdir(path);
                return { entries };
            } catch (err) {
                return { entries: [], error: toSessionFsError(err) };
            }
        },
        readdirWithTypes: async ({ path }) => {
            try {
                const entries = await provider.readdirWithTypes(path);
                return { entries };
            } catch (err) {
                return { entries: [], error: toSessionFsError(err) };
            }
        },
        rm: async ({ path, recursive, force }) => {
            try {
                await provider.rm(path, recursive ?? false, force ?? false);
                return undefined;
            } catch (err) {
                return toSessionFsError(err);
            }
        },
        rename: async ({ src, dest }) => {
            try {
                await provider.rename(src, dest);
                return undefined;
            } catch (err) {
                return toSessionFsError(err);
            }
        },
    };
}

function toSessionFsError(err: unknown): SessionFsError {
    const e = err as NodeJS.ErrnoException;
    const code = e.code === "ENOENT" ? "ENOENT" : "UNKNOWN";
    return { code, message: e.message ?? String(err) };
}
