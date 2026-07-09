/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Hosts the Copilot runtime in-process by loading the native `runtime.node` cdylib
 * and speaking JSON-RPC over its C ABI (FFI) instead of spawning a CLI child process
 * and communicating over stdio/TCP.
 *
 * The native `host_start` export spawns the CLI worker itself
 * (`node <entrypoint> --embedded-host` for a `.js` entrypoint, or `<entrypoint>
 * --embedded-host` for a packaged binary), so the SDK never launches the worker
 * directly. LSP `Content-Length:`-framed JSON-RPC bytes are pumped across the ABI:
 * writes go to `connection_write`; inbound frames arrive on a native callback that
 * feeds {@link FfiRuntimeHost.receiveStream}. The existing `vscode-jsonrpc`
 * `StreamMessageReader`/`StreamMessageWriter` handle framing unchanged — this is a
 * transport swap, not a new protocol.
 */

import { existsSync } from "node:fs";
import koffi from "koffi";
import { dirname, join, resolve } from "node:path";
import { PassThrough, Writable } from "node:stream";

const SYMBOL_PREFIX = "copilot_runtime_";

// A long, referenced no-op timer keeps the Node event loop alive while the in-process
// connection is open (see start()); the exact interval is irrelevant.
const KEEP_ALIVE_INTERVAL_MS = 1 << 30;

type KoffiFunction = ReturnType<ReturnType<typeof koffi.load>["func"]>;
type KoffiType = ReturnType<typeof koffi.pointer>;
type KoffiRegisteredCallback = ReturnType<typeof koffi.register>;

interface FfiLibrary {
    hostStart: KoffiFunction;
    hostShutdown: KoffiFunction;
    connectionOpen: KoffiFunction;
    connectionWrite: KoffiFunction;
    connectionClose: KoffiFunction;
    outboundCallbackType: KoffiType;
}

let loadedLibraryPath: string | undefined;
let loadedLibrary: FfiLibrary | undefined;

/**
 * Loads the cdylib once per process and binds the C ABI exports. Loading a
 * different library path in the same process is unsupported.
 */
function loadLibrary(libraryPath: string): FfiLibrary {
    if (loadedLibrary) {
        if (loadedLibraryPath !== libraryPath) {
            throw new Error(
                `An in-process FFI runtime library is already loaded from '${loadedLibraryPath}'; ` +
                    `loading a different library from '${libraryPath}' in the same process is not supported.`
            );
        }
        return loadedLibrary;
    }

    const lib = koffi.load(libraryPath);
    const outboundCallbackType = koffi.pointer(
        koffi.proto(
            `void ${SYMBOL_PREFIX}outbound(void *userData, uint8 *bytesPtr, size_t bytesLen)`
        )
    );

    loadedLibrary = {
        hostStart: lib.func(`${SYMBOL_PREFIX}host_start`, "uint32", [
            "uint8*",
            "size_t",
            "uint8*",
            "size_t",
        ]),
        hostShutdown: lib.func(`${SYMBOL_PREFIX}host_shutdown`, "bool", ["uint32"]),
        connectionOpen: lib.func(`${SYMBOL_PREFIX}connection_open`, "uint32", [
            "uint32",
            outboundCallbackType,
            "void*",
            "uint8*",
            "size_t",
            "uint8*",
            "size_t",
            "uint8*",
            "size_t",
        ]),
        connectionWrite: lib.func(`${SYMBOL_PREFIX}connection_write`, "bool", [
            "uint32",
            "uint8*",
            "size_t",
        ]),
        connectionClose: lib.func(`${SYMBOL_PREFIX}connection_close`, "bool", ["uint32"]),
        outboundCallbackType,
    };
    loadedLibraryPath = libraryPath;
    return loadedLibrary;
}

function buildArgvJson(cliEntrypoint: string): Buffer {
    // A `.js` entrypoint is launched via node; the packaged single-file CLI binary
    // embeds its own Node and is invoked directly. `--no-auto-update` pins the worker
    // to the bundled pkg matching the loaded cdylib, instead of drifting to a newer
    // version installed under the user's `~/.copilot/pkg` (which would cause ABI skew).
    const argv = cliEntrypoint.toLowerCase().endsWith(".js")
        ? ["node", cliEntrypoint, "--embedded-host", "--no-auto-update"]
        : [cliEntrypoint, "--embedded-host", "--no-auto-update"];
    return Buffer.from(JSON.stringify(argv), "utf8");
}

function buildEnvJson(environment?: Record<string, string | undefined>): Buffer | null {
    if (!environment) {
        return null;
    }
    const obj: Record<string, string> = {};
    for (const [key, value] of Object.entries(environment)) {
        if (value !== undefined) {
            obj[key] = value;
        }
    }
    if (Object.keys(obj).length === 0) {
        return null;
    }
    return Buffer.from(JSON.stringify(obj), "utf8");
}

export class FfiRuntimeHost {
    private readonly lib: FfiLibrary;
    private serverId = 0;
    private connectionId = 0;
    private disposed = false;
    private outboundCallback: KoffiRegisteredCallback | undefined;
    private keepAliveTimer: ReturnType<typeof setInterval> | undefined;

    /** The stream JSON-RPC reads server→client frames from. */
    readonly receiveStream: PassThrough;
    /** The stream JSON-RPC writes client→server frames to. */
    readonly sendStream: Writable;

    private constructor(
        private readonly libraryPath: string,
        private readonly cliEntrypoint: string,
        private readonly environment?: Record<string, string | undefined>
    ) {
        this.lib = loadLibrary(libraryPath);
        this.receiveStream = new PassThrough();
        this.sendStream = new Writable({
            // connection_write enqueues the frame into the runtime's inbound channel and
            // returns immediately, so a synchronous FFI call is sufficient here.
            write: (chunk: Buffer, _encoding, callback) => {
                try {
                    this.writeFrame(chunk);
                    callback();
                } catch (error) {
                    callback(error as Error);
                }
            },
        });
    }

    /**
     * Resolves the cdylib next to the given CLI entrypoint and prepares the FFI host.
     * The cdylib is resolved as `prebuilds/<prebuildsFolder>/runtime.node` relative to
     * the entrypoint directory (the napi-rs `<node-platform>-<arch>` layout, e.g.
     * `linux-x64`). Throws if it cannot be found.
     */
    static create(
        cliEntrypoint: string,
        prebuildsFolder: string,
        environment?: Record<string, string | undefined>
    ): FfiRuntimeHost {
        const fullEntrypoint = resolve(cliEntrypoint);
        const distDir = dirname(fullEntrypoint);
        const libraryPath = join(distDir, "prebuilds", prebuildsFolder, "runtime.node");
        if (!existsSync(libraryPath)) {
            throw new Error(`FFI runtime library not found. Looked for '${libraryPath}'.`);
        }
        return new FfiRuntimeHost(libraryPath, fullEntrypoint, environment);
    }

    /**
     * Starts the in-process runtime: spawns the CLI worker via the native host,
     * waits for readiness, and opens the FFI JSON-RPC connection.
     */
    async start(): Promise<void> {
        const argvJson = buildArgvJson(this.cliEntrypoint);
        const envJson = buildEnvJson(this.environment);

        // The native host spawns the CLI worker itself and has no cwd parameter, so the
        // worker inherits this process's cwd. A custom working directory is intentionally
        // unsupported for the in-process transport (rejected by the client constructor)
        // rather than mutating the shared process-global cwd here.

        // host_start blocks until the worker connects back and signals readiness
        // (up to ~30s); run it as an async FFI call so the Node event loop isn't blocked.
        this.serverId = await new Promise<number>((resolvePromise, rejectPromise) => {
            this.lib.hostStart.async(
                argvJson,
                argvJson.length,
                envJson,
                envJson ? envJson.length : 0,
                (error: Error | null, result: number) => {
                    if (error) {
                        rejectPromise(error);
                    } else {
                        resolvePromise(result);
                    }
                }
            );
        });
        if (!this.serverId) {
            throw new Error(
                `copilot_runtime_host_start failed (library '${this.libraryPath}', entrypoint '${this.cliEntrypoint}').`
            );
        }

        this.outboundCallback = koffi.register(
            (_userData: unknown, bytesPtr: unknown, bytesLen: number | bigint) =>
                this.feedInbound(bytesPtr, bytesLen),
            this.lib.outboundCallbackType
        );

        this.connectionId = this.lib.connectionOpen(
            this.serverId,
            this.outboundCallback,
            null,
            null,
            0,
            null,
            0,
            null,
            0
        );
        if (!this.connectionId) {
            this.unregisterCallback();
            this.lib.hostShutdown(this.serverId);
            this.serverId = 0;
            throw new Error("copilot_runtime_connection_open failed.");
        }

        // The in-process transport has no socket/pipe handle to keep the Node event loop
        // alive while the SDK is idle awaiting a server→client frame. koffi delivers the
        // outbound callback on the loop but does not reference it, so hold one referenced
        // timer for the lifetime of the connection.
        this.keepAliveTimer = setInterval(() => {}, KEEP_ALIVE_INTERVAL_MS);
    }

    private writeFrame(frame: Buffer): void {
        if (this.disposed || !this.connectionId) {
            throw new Error("The in-process runtime connection is closed.");
        }
        const ok = this.lib.connectionWrite(this.connectionId, frame, frame.length);
        if (!ok) {
            throw new Error("Failed to write a frame to the in-process runtime connection.");
        }
    }

    /**
     * Native outbound (server→client) callback. koffi delivers it on the JS event loop
     * via a threadsafe function, so the frame is decoded and written straight to
     * {@link receiveStream}. The native pointer is only valid for this call, so the
     * bytes are copied out before returning.
     */
    private feedInbound(bytesPtr: unknown, bytesLen: number | bigint): void {
        // An exception thrown across the native→JS (Node-API) boundary cannot propagate
        // and would surface only as a DEP0168 "uncaught Node-API callback exception"
        // warning, so catch and log it here instead of letting it escape.
        try {
            // A native outbound callback can still be delivered on the event loop after
            // dispose() has ended receiveStream; writing then would throw
            // ERR_STREAM_WRITE_AFTER_END. Drop late frames instead — the connection is
            // gone and nothing is reading them.
            if (this.disposed || this.receiveStream.writableEnded) {
                return;
            }
            const length = Number(bytesLen);
            if (!bytesPtr || length <= 0) {
                return;
            }
            const bytes = koffi.decode(
                bytesPtr,
                koffi.array("uint8", length, "Typed")
            ) as Uint8Array;
            this.receiveStream.write(Buffer.from(bytes));
        } catch (error) {
            console.error(
                `In-process FFI inbound callback failed: ${error instanceof Error ? (error.stack ?? error.message) : String(error)}`
            );
        }
    }

    private unregisterCallback(): void {
        if (this.outboundCallback === undefined) {
            return;
        }
        const callback = this.outboundCallback;
        this.outboundCallback = undefined;
        try {
            koffi.unregister(callback);
        } catch {
            // Ignore teardown failures.
        }
    }

    /** Closes the FFI connection, shuts down the native host, and releases resources. */
    dispose(): void {
        if (this.disposed) {
            return;
        }
        this.disposed = true;

        if (this.keepAliveTimer !== undefined) {
            clearInterval(this.keepAliveTimer);
            this.keepAliveTimer = undefined;
        }

        try {
            if (this.connectionId) {
                this.lib.connectionClose(this.connectionId);
                this.connectionId = 0;
            }
        } catch {
            // Ignore teardown failures.
        }

        try {
            if (this.serverId) {
                this.lib.hostShutdown(this.serverId);
                this.serverId = 0;
            }
        } catch {
            // Ignore teardown failures.
        }

        this.receiveStream.end();
        this.unregisterCallback();
    }
}
