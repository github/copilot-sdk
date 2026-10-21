/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code persistence} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerPersistenceApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerPersistenceApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Atomically replaces a JSON file's entire contents while holding the runtime's per-path lock. The lock lives in the runtime process, so a host that writes these files itself takes no part in it and can tear a concurrent runtime write; route the write here instead. Unlike a merging write this never reads or parses the existing file, so a previously corrupted snapshot is replaced wholesale rather than failing every later write. Use it only for callers that always persist a complete snapshot.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<PersistenceWriteRawJsonLockedResult> writeRawJsonLocked(PersistenceWriteRawJsonLockedParams params) {
        return caller.invoke("persistence.writeRawJsonLocked", params, PersistenceWriteRawJsonLockedResult.class);
    }

}
