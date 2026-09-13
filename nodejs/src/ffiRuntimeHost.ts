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
import {
    DataType,
    PointerType,
    arrayConstructor,
    createExternalBuffer,
    createPointer,
    freePointer,
    funcConstructor,
    load,
    open,
    restorePointer,
    unwrapPointer,
    type FuncConstructorOptions,
    type JsExternal,
} from "ffi-rs";
import { resolve } from "node:path";
import { PassThrough, Writable } from "node:stream";

const SYMBOL_PREFIX = "copilot_runtime_";
const LIBRARY_KEY_PREFIX = "@github/copilot-sdk/runtime:";
const CLEANUP_RETRY_INTERVAL_MS = 100;

interface FfiLibrary {
    hostStart(
        argv: Buffer,
        argvLength: number,
        environment: Buffer,
        environmentLength: number
    ): Promise<number>;
    hostShutdown(serverId: number): Promise<boolean>;
    connectionOpen(
        serverId: number,
        callback: JsExternal,
        userData: JsExternal,
        config: Buffer,
        configLength: number,
        auth: Buffer,
        authLength: number,
        additional: Buffer,
        additionalLength: number
    ): Promise<number>;
    connectionWrite(connectionId: number, frame: Buffer, frameLength: number): Promise<boolean>;
    connectionClose(connectionId: number): Promise<boolean>;
    outboundCallbackType: FuncConstructorOptions;
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

    const libraryKey = `${LIBRARY_KEY_PREFIX}${libraryPath}`;
    open({ library: libraryKey, path: libraryPath });
    const outboundCallbackType = funcConstructor({
        // ffi-rs cannot create External values on a native callback thread. Pointer
        // values are ABI-equivalent to i64 on every platform supported by this host,
        // and BigInt preserves all pointer bits when crossing into JavaScript.
        paramsType: [DataType.BigInt, DataType.BigInt, DataType.U64],
        // ffi-rs 1.3.7 corrupts the first pointer-sized bytes of callback-owned memory
        // for void callbacks. The native caller ignores the return register, so use a
        // u64 return and always return zero.
        retType: DataType.U64,
    });

    loadedLibrary = {
        hostStart: (argv, argvLength, environment, environmentLength) =>
            load({
                library: libraryKey,
                funcName: `${SYMBOL_PREFIX}host_start`,
                retType: DataType.U32,
                paramsType: [DataType.U8Array, DataType.U64, DataType.U8Array, DataType.U64],
                paramsValue: [argv, argvLength, environment, environmentLength],
                runInNewThread: true,
            }),
        hostShutdown: (serverId) =>
            load({
                library: libraryKey,
                funcName: `${SYMBOL_PREFIX}host_shutdown`,
                retType: DataType.Boolean,
                paramsType: [DataType.U32],
                paramsValue: [serverId],
                runInNewThread: true,
            }),
        connectionOpen: (
            serverId,
            callback,
            userData,
            config,
            configLength,
            auth,
            authLength,
            additional,
            additionalLength
        ) =>
            load({
                library: libraryKey,
                funcName: `${SYMBOL_PREFIX}connection_open`,
                retType: DataType.U32,
                paramsType: [
                    DataType.U32,
                    DataType.External,
                    DataType.External,
                    DataType.U8Array,
                    DataType.U64,
                    DataType.U8Array,
                    DataType.U64,
                    DataType.U8Array,
                    DataType.U64,
                ],
                paramsValue: [
                    serverId,
                    callback,
                    userData,
                    config,
                    configLength,
                    auth,
                    authLength,
                    additional,
                    additionalLength,
                ],
                runInNewThread: true,
            }),
        connectionWrite: (connectionId, frame, frameLength) =>
            load({
                library: libraryKey,
                funcName: `${SYMBOL_PREFIX}connection_write`,
                retType: DataType.Boolean,
                paramsType: [DataType.U32, DataType.U8Array, DataType.U64],
                paramsValue: [connectionId, frame, frameLength],
                runInNewThread: true,
            }),
        connectionClose: (connectionId) =>
            load({
                library: libraryKey,
                funcName: `${SYMBOL_PREFIX}connection_close`,
                retType: DataType.Boolean,
                paramsType: [DataType.U32],
                paramsValue: [connectionId],
                runInNewThread: true,
            }),
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

function buildEnvJson(environment?: Record<string, string | undefined>): Buffer {
    if (!environment) {
        return Buffer.alloc(0);
    }
    const obj: Record<string, string> = {};
    for (const [key, value] of Object.entries(environment)) {
        if (value !== undefined) {
            obj[key] = value;
        }
    }
    if (Object.keys(obj).length === 0) {
        return Buffer.alloc(0);
    }
    return Buffer.from(JSON.stringify(obj), "utf8");
}

export class FfiRuntimeHost {
    private static readonly quarantinedHosts = new Set<FfiRuntimeHost>();

    private readonly lib: FfiLibrary;
    private serverId = 0;
    private connectionId = 0;
    private disposed = false;
    private starting = false;
    private outboundCallback: JsExternal[] | undefined;
    private outboundCallbackPointer: JsExternal | undefined;
    private inboundAddressOwner: JsExternal[] | undefined;
    private inboundAddressSlot: Buffer | undefined;
    private cleanupRetryTimer: ReturnType<typeof setTimeout> | undefined;
    private cleanupInProgress = false;

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
            write: (chunk: Buffer, _encoding, callback) => {
                void this.writeFrame(chunk).then(
                    () => callback(),
                    (error: unknown) =>
                        callback(error instanceof Error ? error : new Error(String(error)))
                );
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
        if (this.disposed) {
            throw new Error("The in-process runtime host is disposed.");
        }
        this.starting = true;
        const argvJson = buildArgvJson(this.cliEntrypoint, this.args);
        const envJson = buildEnvJson(this.environment);

        try {
            // The native host has no cwd parameter, so it uses this process's cwd. A custom
            // working directory is intentionally
            // unsupported for the in-process transport (rejected by the client constructor)
            // rather than mutating the shared process-global cwd here.

            // host_start constructs the native engine synchronously; run it as an async FFI
            // call so the Node event loop isn't blocked.
            this.serverId = await this.lib.hostStart(
                argvJson,
                argvJson.length,
                envJson,
                envJson.length
            );
            if (!this.serverId) {
                throw new Error(
                    `copilot_runtime_host_start failed (library '${this.libraryPath}').`
                );
            }
            if (this.disposed) {
                throw new Error("The in-process runtime host was disposed during startup.");
            }

            try {
                this.outboundCallback = createPointer({
                    paramsType: [this.lib.outboundCallbackType],
                    paramsValue: [
                        (_userData: bigint, bytesAddress: bigint, bytesLen: number) => {
                            this.feedInbound(bytesAddress, bytesLen);
                            return 0;
                        },
                    ],
                });
                this.outboundCallbackPointer = unwrapPointer(this.outboundCallback)[0];
                this.inboundAddressOwner = createPointer({
                    paramsType: [DataType.BigInt],
                    paramsValue: [0n],
                });
                this.inboundAddressSlot = createExternalBuffer(this.inboundAddressOwner[0], 8);

                const empty = Buffer.alloc(0);
                this.connectionId = await this.lib.connectionOpen(
                    this.serverId,
                    this.outboundCallbackPointer,
                    this.outboundCallbackPointer,
                    empty,
                    0,
                    empty,
                    0,
                    empty,
                    0
                );
                if (!this.connectionId) {
                    throw new Error("copilot_runtime_connection_open failed.");
                }
            } catch (error) {
                this.releaseCallbackResources();
                await this.shutdownHost();
                throw error;
            }
        } finally {
            this.starting = false;
            if (this.disposed) {
                void this.tryFinalizeCleanup();
            }
        }
    }

    private async writeFrame(frame: Buffer): Promise<void> {
        if (this.disposed || !this.connectionId) {
            throw new Error("The in-process runtime connection is closed.");
        }
        const ok = await this.lib.connectionWrite(this.connectionId, frame, frame.length);
        if (!ok) {
            throw new Error("Failed to write a frame to the in-process runtime connection.");
        }
    }

    /**
     * Native outbound (server→client) callback. ffi-rs dispatches it through a blocking
     * thread-safe function, so the native pointer remains valid until this callback
     * returns. Copy the bytes before returning.
     */
    private feedInbound(bytesAddress: bigint, bytesLen: number): void {
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
            if (bytesAddress === 0n || bytesLen <= 0) {
                return;
            }
            if (!this.inboundAddressOwner || !this.inboundAddressSlot) {
                throw new Error("In-process FFI callback address storage is unavailable.");
            }
            this.inboundAddressSlot.writeBigInt64LE(bytesAddress);
            const [bytes] = restorePointer({
                retType: [arrayConstructor({ type: DataType.U8Array, length: bytesLen })],
                paramsValue: this.inboundAddressOwner,
            });
            this.receiveStream.write(Buffer.from(bytes));
        } catch (error) {
            console.error(
                `In-process FFI inbound callback failed: ${error instanceof Error ? (error.stack ?? error.message) : String(error)}`
            );
        }
    }

    private releaseCallbackResources(): boolean {
        let released = true;
        if (this.outboundCallback !== undefined) {
            const callback = this.outboundCallback;
            try {
                freePointer({
                    paramsType: [this.lib.outboundCallbackType],
                    paramsValue: callback,
                    pointerType: PointerType.RsPointer,
                });
                this.outboundCallback = undefined;
                this.outboundCallbackPointer = undefined;
            } catch (error) {
                console.error(
                    `Failed to free in-process FFI callback: ${error instanceof Error ? (error.stack ?? error.message) : String(error)}`
                );
                released = false;
            }
        }
        if (this.inboundAddressOwner !== undefined) {
            const addressOwner = this.inboundAddressOwner;
            try {
                freePointer({
                    paramsType: [DataType.BigInt],
                    paramsValue: addressOwner,
                    pointerType: PointerType.RsPointer,
                });
                this.inboundAddressOwner = undefined;
                this.inboundAddressSlot = undefined;
            } catch (error) {
                console.error(
                    `Failed to free in-process FFI callback address storage: ${error instanceof Error ? (error.stack ?? error.message) : String(error)}`
                );
                released = false;
            }
        }
        if (!released) {
            FfiRuntimeHost.quarantinedHosts.add(this);
        }
        return released;
    }

    private async shutdownHost(): Promise<void> {
        if (!this.serverId) {
            return;
        }
        const serverId = this.serverId;
        try {
            if (!(await this.lib.hostShutdown(serverId))) {
                console.error(`In-process FFI host shutdown did not recognize server ${serverId}.`);
            }
        } catch (error) {
            console.error(
                `Failed to shut down in-process FFI host: ${error instanceof Error ? (error.stack ?? error.message) : String(error)}`
            );
        } finally {
            this.serverId = 0;
        }
    }

    private scheduleCleanupRetry(): void {
        if (this.cleanupRetryTimer !== undefined) {
            return;
        }
        this.cleanupRetryTimer = setTimeout(() => {
            this.cleanupRetryTimer = undefined;
            void this.tryFinalizeCleanup();
        }, CLEANUP_RETRY_INTERVAL_MS);
    }

    private async tryFinalizeCleanup(): Promise<void> {
        if (this.cleanupInProgress) {
            this.scheduleCleanupRetry();
            return;
        }
        this.cleanupInProgress = true;

        try {
            if (this.connectionId) {
                let closed = false;
                try {
                    // Close waits for outbound callbacks, which need the JS event loop
                    // to run and return before native code can report quiescence.
                    closed = await this.lib.connectionClose(this.connectionId);
                } catch (error) {
                    console.error(
                        `Failed to close in-process FFI connection: ${error instanceof Error ? (error.stack ?? error.message) : String(error)}`
                    );
                    this.scheduleCleanupRetry();
                    return;
                }
                if (!closed) {
                    this.scheduleCleanupRetry();
                    return;
                }
                this.connectionId = 0;
            }
            const callbackResourcesReleased = this.releaseCallbackResources();

            await this.shutdownHost();
            if (callbackResourcesReleased) {
                FfiRuntimeHost.quarantinedHosts.delete(this);
            }
        } finally {
            this.cleanupInProgress = false;
        }
    }

    /** Awaits the initial cleanup attempt; a non-quiescent close is retried in the background. */
    async dispose(): Promise<void> {
        if (this.disposed) {
            return;
        }
        this.disposed = true;
        this.receiveStream.end();
        if (!this.starting) {
            await this.tryFinalizeCleanup();
        }
    }
}
