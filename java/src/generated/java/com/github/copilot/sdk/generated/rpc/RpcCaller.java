/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * Interface for invoking JSON-RPC methods with typed responses.
 * <p>
 * Implementations delegate to the underlying transport layer
 * (e.g., a {@code JsonRpcClient} instance). Use a method reference:
 * <pre>{@code
 * RpcCaller caller = jsonRpcClient::invoke;
 * }</pre>
 * Note: because the {@code invoke} method has a type parameter, this interface cannot
 * be implemented using a lambda expression — use a method reference or anonymous class.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public interface RpcCaller {

    /**
     * Invokes a JSON-RPC method and returns a future for the typed response.
     *
     * @param <T>        the expected response type
     * @param method     the JSON-RPC method name
     * @param params     the request parameters (may be a {@code Map}, DTO record, or {@code JsonNode})
     * @param resultType the {@link Class} of the expected response type
     * @return a {@link CompletableFuture} that completes with the deserialized result
     */
    <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType);
}
