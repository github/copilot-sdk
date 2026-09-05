/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Hosts the Copilot runtime in-process by loading the native `runtime.node` cdylib
 * and speaking JSON-RPC over its C ABI (FFI) instead of spawning a CLI child process
 * and communicating over stdio/TCP.
 *
 * The native `host_start` export constructs the Rust server synchronously in this
 * process. LSP `Content-Length:`-framed JSON-RPC bytes are pumped across the ABI:
 * writes go to `connection_write`; inbound frames arrive on a native callback that
 * feeds {@link FfiRuntimeHost.receiveStream}. The existing `vscode-jsonrpc`
 * `StreamMessageReader`/`StreamMessageWriter` handle framing unchanged — this is a
 * transport swap, not a new protocol.
 */

import { existsSync } from "node:fs";
import koffi from "koffi";
import { resolve } from "node:path";
import { PassThrough, Writable } from "node:stream";

const SYMBOL_PREFIX = "copilot_runtime_";

// A long, referenced no-op timer keeps the Node event loop alive while the in-process
// connection is open (see start()); the exact interval is irrelevant.
const KEEP_ALIVE_INTERVAL_MS = 1 << 30;

// Upper bound on how long dispose() waits for the native host_shutdown call; see
// shutdownHost() for why this exists.
const HOST_SHUTDOWN_TIMEOUT_MS = 10_000;

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

function buildArgvJson(cliEntrypoint: string | undefined, args: readonly string[]): Buffer {
    const argv = cliEntrypoint
        ? cliEntrypoint.toLowerCase().endsWith(".js")
            ? ["node", cliEntrypoint, "--embedded-host", "--no-auto-update"]
            : [cliEntrypoint, "--embedded-host", "--no-auto-update"]
        : [];
    argv.push(...args);
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
        private readonly cliEntrypoint: string | undefined,
        private readonly environment: Record<string, string | undefined> | undefined,
        private readonly args: readonly string[]
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
     * Loads the runtime cdylib at the given path and prepares the FFI host.
     */
    static create(
        libraryPath: string,
        cliEntrypoint: string | undefined,
        environment: Record<string, string | undefined> | undefined,
        args: readonly string[]
    ): FfiRuntimeHost {
        const fullLibraryPath = resolve(libraryPath);
        if (!existsSync(fullLibraryPath)) {
            throw new Error(`FFI runtime library not found at '${fullLibraryPath}'.`);
        }
        return new FfiRuntimeHost(
            fullLibraryPath,
            cliEntrypoint ? resolve(cliEntrypoint) : undefined,
            environment,
            args
        );
    }

    /** Starts the in-process Rust runtime and opens the FFI JSON-RPC connection. */
    async start(): Promise<void> {
        const argvJson = buildArgvJson(this.cliEntrypoint, this.args);
        const envJson = buildEnvJson(this.environment);

        // The native host has no cwd parameter, so it uses this process's cwd. A custom
        // working directory is intentionally
        // unsupported for the in-process transport (rejected by the client constructor)
        // rather than mutating the shared process-global cwd here.

        // host_start constructs the native engine synchronously; run it as an async FFI
        // call so the Node event loop isn't blocked.
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
            throw new Error(`copilot_runtime_host_start failed (library '${this.libraryPath}').`);
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
            const serverId = this.serverId;
            this.serverId = 0;
            await this.shutdownHost(serverId);
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
    async dispose(): Promise<void> {
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

        this.receiveStream.end();

        const serverId = this.serverId;
        this.serverId = 0;
        if (serverId) {
            await this.shutdownHost(serverId);
        } else {
            this.unregisterCallback();
        }
    }

    /**
     * Calls the native `host_shutdown` export and bounds how long {@link dispose} waits
     * for it.
     *
     * This runs the runtime's own teardown (including closing its SQLite session store)
     * in this process. Calling it synchronously previously blocked the entire Node event
     * loop until it returned, with no way to time out — on Windows in-process, a slow or
     * stuck shutdown could hang the whole process, which is exactly the failure mode this
     * bounds against (see github/copilot-sdk#2525). Using koffi's `.async` variant runs
     * the call on koffi's native thread pool instead of the event loop, so a stuck call
     * cannot freeze the process, and racing it against a timeout keeps `dispose()` (and
     * thus `forceStop()`) from hanging indefinitely even if the native call itself never
     * returns. The callback still unregisters the outbound callback once the native call
     * completes, whether or not this method already timed out waiting for it.
     */
    private async shutdownHost(serverId: number): Promise<void> {
        const shutdownCompleted = new Promise<void>((resolvePromise) => {
            this.lib.hostShutdown.async(serverId, () => {
                this.unregisterCallback();
                resolvePromise();
            });
        });

        const timedOut = Symbol("host_shutdown timeout");
        const result = await Promise.race([
            shutdownCompleted.then(() => "completed" as const),
            new Promise<typeof timedOut>((resolvePromise) =>
                setTimeout(() => resolvePromise(timedOut), HOST_SHUTDOWN_TIMEOUT_MS).unref()
            ),
        ]);

        if (result === timedOut) {
            // The native call (and the callback unregistration that follows it) keeps
            // running; we just stop waiting here so the caller is not blocked forever.
            // This should be rare and indicates a runtime-side shutdown defect worth
            // reporting upstream, not something for the SDK to retry.
            console.error(
                `In-process FFI host_shutdown did not complete within ${HOST_SHUTDOWN_TIMEOUT_MS}ms; abandoning wait.`
            );
        }
    }
}
