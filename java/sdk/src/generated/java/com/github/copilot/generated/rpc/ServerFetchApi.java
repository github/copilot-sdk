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
 * API methods for the {@code fetch} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerFetchApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerFetchApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Allocates a connection-owned HTTP request handle without network work. SDK clients and extensions can use this API without CLI admission. Cancel abandoned handles; disconnect disposes all handles.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<FetchCreateResult> create() {
        return caller.invoke("fetch.create", java.util.Map.of(), FetchCreateResult.class);
    }

    /**
     * Sends a connection-owned handle once, with a required responseMode. Available to all SDK clients and extensions. Stream returns headers immediately; fetch.read returns body chunks over JSON-RPC, or fetch.cancel closes the body without reading it. Text consumes the complete body as WHATWG UTF-8 text with a default decoded limit of 1048576 bytes. File writes directly to a required destinationPath on the runtime host, with atomic no-overwrite creation; incomplete files are removed on failure or cancellation, and completed files belong to the caller and survive disconnect. Stream and file have no default body limit. Explicit bodyLimitBytes, including zero, fails without truncation. HTTP error statuses remain responses. Cancel can interrupt header waits and body consumption. Optional credential contexts resolve fresh credentials; omission adds no service authentication. Revocation applies through redirects under the original authority. Credential failures use -32004; transport failures preserve -32603 and FetchTransportErrorData; filesystem failures use -32603 and FetchFileErrorData. No shared circuit breaker is selected. Retries remain caller-owned.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<FetchSendResult> send(FetchSendParams params) {
        return caller.invoke("fetch.send", params, FetchSendResult.class);
    }

    /**
     * Reads one connection-owned response byte range or EOF. Response chunks cross JSON-RPC as base64; prefer fetch.send with file mode for large bodies. Call only after fetch.send returns in stream mode, with at most one read outstanding. The runtime reads the next chunk on demand. EOF, failure, cancellation, and disconnect dispose the handle. Active credential failures use JSON-RPC code -32004 and CredentialContextErrorData in error.data. Body transport failures preserve code -32603 and the legacy message, with FetchTransportErrorData containing unchanged engine classifications. Already disposed handles remain closed-handle errors, never credential_unavailable.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<FetchReadResult> read(FetchReadParams params) {
        return caller.invoke("fetch.read", params, FetchReadResult.class);
    }

    /**
     * Disposes an owned handle and cancels any pending send or read. Safe before send and after disposal. Unknown and foreign handles are unchanged. Closes an unread stream without draining its body. Incomplete files are removed after pending file I/O settles; completed caller-owned files remain.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<FetchCancelResult> cancel(FetchCancelParams params) {
        return caller.invoke("fetch.cancel", params, FetchCancelResult.class);
    }

}
