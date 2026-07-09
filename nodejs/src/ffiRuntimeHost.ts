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

type KoffiFunction = ReturnType<ReturnType<typeof koffi.load>["func"]>;
type KoffiType = ReturnType<typeof koffi.pointer>;
type KoffiRegisteredCallback = ReturnType<typeof koffi.register>;

interface FfiLibrary {
    hostStart: KoffiFunction;
    hostShutdown: KoffiFunction;
    connectionOpen: KoffiFunction;
    connectionWrite: KoffiFunction;
    connectionClose: KoffiFunction;
    logDroppedCount: KoffiFunction;
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
        // A no-argument, side-effect-free diagnostic getter (returns a dropped-log
        // counter). Used purely as a cheap async FFI call to keep koffi's asynchronous
        // callback broker serviced while the connection is idle (see the pump in start()).
        logDroppedCount: lib.func(`${SYMBOL_PREFIX}log_dropped_count`, "uint64", []),
        outboundCallbackType,
    };
    loadedLibraryPath = libraryPath;
    return loadedLibrary;
}

function buildArgvJson(cliEntrypoint: string): Buffer {
    // A `.js` entrypoint is launched via node; the packaged single-file CLI binary
    // embeds its own Node and is invoked directly.
    const argv = cliEntrypoint.toLowerCase().endsWith(".js")
        ? ["node", cliEntrypoint, "--embedded-host"]
        : [cliEntrypoint, "--embedded-host"];
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
    /**
     * Keeps koffi's asynchronous callback broker serviced while the FFI connection is
     * open, so inbound native→JS frames are delivered promptly even when the SDK is
     * otherwise idle (e.g. awaiting a model response with no client→server writes in
     * flight).
     *
     * The runtime invokes our outbound callback from a worker thread. koffi marshals
     * such foreign-thread callbacks to the JS main thread via a threadsafe-function
     * "broker" that is only serviced WHILE a koffi asynchronous FFI call is in flight —
     * a plain JS timer (setInterval) does NOT pump it. During active request/response
     * traffic the constant `connection_write` calls keep frames flowing, but once the
     * SDK goes idle mid-turn (e.g. the worker is awaiting a model HTTP response, so
     * there are no client→server writes) the broker parks and the next outbound frame
     * (the model response, then everything after it) sits undelivered until the next
     * koffi call — observed as a permanent 30s stall of otherwise-healthy sessions on
     * macOS/Windows (libuv services the broker differently on Linux, which is why it
     * only reproduces off-Linux). Keeping exactly one cheap async FFI call
     * (`log_dropped_count`) continuously in flight keeps the broker pumping so
     * foreign-thread frames are always delivered. This is the koffi analogue of the
     * .NET host's raw function-pointer callback, which runs directly on the runtime
     * thread and needs no event-loop pumping.
     */
    private pumpActive = false;
    /**
     * Inbound frame bytes copied out of the native callback, awaiting delivery to
     * {@link receiveStream} on a clean event-loop tick. See {@link feedInbound}.
     */
    private readonly inboundQueue: Buffer[] = [];
    /** Whether a drain of {@link inboundQueue} is already scheduled. */
    private drainScheduled = false;

    /** The stream JSON-RPC reads server→client frames from. */
    readonly receiveStream: PassThrough;
    /** The stream JSON-RPC writes client→server frames to. */
    readonly sendStream: Writable;

    private constructor(
        private readonly libraryPath: string,
        private readonly cliEntrypoint: string,
        private readonly environment?: Record<string, string | undefined>,
        private readonly workingDirectory?: string
    ) {
        this.lib = loadLibrary(libraryPath);
        this.receiveStream = new PassThrough();
        this.sendStream = new Writable({
            // Write frames with a synchronous FFI call (restored while diagnosing the
            // macOS/Windows inbound-callback stall; async writes were a no-op for it).
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
        environment?: Record<string, string | undefined>,
        workingDirectory?: string
    ): FfiRuntimeHost {
        const fullEntrypoint = resolve(cliEntrypoint);
        const distDir = dirname(fullEntrypoint);
        const libraryPath = join(distDir, "prebuilds", prebuildsFolder, "runtime.node");
        if (!existsSync(libraryPath)) {
            throw new Error(`FFI runtime library not found. Looked for '${libraryPath}'.`);
        }
        return new FfiRuntimeHost(libraryPath, fullEntrypoint, environment, workingDirectory);
    }

    /**
     * Starts the in-process runtime: spawns the CLI worker via the native host,
     * waits for readiness, and opens the FFI JSON-RPC connection.
     */
    async start(): Promise<void> {
        const argvJson = buildArgvJson(this.cliEntrypoint);
        const envJson = buildEnvJson(this.environment);

        // The native host spawns the CLI worker itself and has no cwd parameter, so the
        // worker inherits this process's cwd. Mirror the stdio child's `cwd: workingDirectory`
        // by switching cwd for the duration of the blocking host_start, then restoring it.
        const previousCwd = process.cwd();
        const shouldSwitchCwd = !!this.workingDirectory && this.workingDirectory !== previousCwd;
        if (shouldSwitchCwd) {
            process.chdir(this.workingDirectory!);
        }

        // host_start blocks until the worker connects back and signals readiness
        // (up to ~30s); run it as an async FFI call so the Node event loop isn't blocked.
        try {
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
        } finally {
            if (shouldSwitchCwd) {
                process.chdir(previousCwd);
            }
        }
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

        this.startBrokerPump();
    }

    /**
     * Keeps exactly one cheap async FFI call in flight at all times so koffi's
     * threadsafe-function broker stays serviced and foreign-thread outbound callbacks
     * are delivered without waiting for the next client→server write. Each call
     * reschedules the next on completion; the in-flight libuv request also keeps the
     * event loop alive (replacing the old keep-alive timer). Stops when disposed.
     */
    private startBrokerPump(): void {
        if (this.pumpActive) {
            return;
        }
        this.pumpActive = true;
        const pump = (): void => {
            if (this.disposed || !this.connectionId) {
                this.pumpActive = false;
                return;
            }
            this.lib.logDroppedCount.async((_error: Error | null, _result: unknown) => {
                if (this.disposed || !this.connectionId) {
                    this.pumpActive = false;
                    return;
                }
                pump();
            });
        };
        pump();
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
     * Native outbound callback: copies the inbound frame bytes and hands them to the
     * event loop for delivery, WITHOUT driving the JSON-RPC reader synchronously here.
     *
     * The native pointer is only valid for the duration of this call, so the bytes are
     * decoded/copied eagerly; but writing them to {@link receiveStream} (which
     * synchronously drives frame parsing and JSON-RPC dispatch, and may re-enter the
     * SDK's write path) is deferred to a `setImmediate` drain on a clean stack. This
     * keeps the callback minimal and non-reentrant — the koffi analogue of the .NET
     * host's thread-safe `Channel` callback, which enqueues and returns immediately.
     * Delivering synchronously here could re-enter a native `connection_write` call
     * still on the stack and deadlock (observed as hung `session.rpc.*` round-trips
     * on macOS).
     */
    private feedInbound(bytesPtr: unknown, bytesLen: number | bigint): void {
        // This runs as a native→JS (Node-API) callback, possibly from a secondary
        // thread. An exception thrown here cannot propagate across the FFI boundary and
        // is swallowed by the runtime (surfacing only as a DEP0168 "Uncaught Node-API
        // callback exception" warning), so catch and log it here instead of letting it
        // escape.
        try {
            const length = Number(bytesLen);
            if (!bytesPtr || length <= 0) {
                return;
            }
            const bytes = koffi.decode(
                bytesPtr,
                koffi.array("uint8", length, "Typed")
            ) as Uint8Array;
            this.inboundQueue.push(Buffer.from(bytes));
            if (!this.drainScheduled) {
                this.drainScheduled = true;
                setImmediate(() => this.drainInbound());
            }
        } catch (error) {
            console.error(
                `In-process FFI inbound callback failed: ${error instanceof Error ? (error.stack ?? error.message) : String(error)}`
            );
        }
    }

    /** Delivers queued inbound frames to {@link receiveStream} on a clean event-loop tick. */
    private drainInbound(): void {
        this.drainScheduled = false;
        if (this.disposed) {
            this.inboundQueue.length = 0;
            return;
        }
        let frame: Buffer | undefined;
        while ((frame = this.inboundQueue.shift()) !== undefined) {
            this.receiveStream.write(frame);
        }
    }

    private unregisterCallback(): void {
        if (this.outboundCallback === undefined) {
            return;
        }
        const callback = this.outboundCallback;
        this.outboundCallback = undefined;
        // Defer the unregister to a later tick instead of unregistering synchronously.
        // koffi delivers outbound callbacks from a secondary thread by queuing them onto
        // the JS event loop; at teardown one such delivery can still be queued after we
        // stop the native side. Unregistering while koffi still has a queued call makes
        // koffi invoke a torn-down callback and raise inside its own native code — an
        // uncaught Node-API callback exception (DEP0168) that no JS try/catch can catch.
        // The native connection/host are already closed by the time this runs (see
        // dispose), so no new deliveries originate; a queued delivery fires in libuv's
        // poll phase and setImmediate (check phase) runs right after it in the same loop
        // iteration, so the pending delivery (a no-op, since we are disposed) drains
        // before we free the slot.
        const immediate = setImmediate(() => {
            try {
                koffi.unregister(callback);
            } catch {
                // Ignore teardown failures.
            }
        });
        // Don't let this housekeeping timer keep the process alive.
        immediate.unref?.();
    }

    /** Closes the FFI connection, shuts down the native host, and releases resources. */
    dispose(): void {
        if (this.disposed) {
            return;
        }
        this.disposed = true;

        // The broker pump observes `disposed` and stops rescheduling on its next
        // completion; no timer to clear.
        this.pumpActive = false;

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
