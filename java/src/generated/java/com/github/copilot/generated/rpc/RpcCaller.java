/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.annotation.processing.Generated;

/**
 * Interface for invoking JSON-RPC methods with typed responses.
 * <p>
 * Implementations delegate to the underlying transport layer
 * (e.g., a {@code JsonRpcClient} instance). A method reference is typically the clearest
 * way to adapt a generic {@code invoke} method to this interface:
 * <pre>{@code
 * RpcCaller caller = jsonRpcClient::invoke;
 * }</pre>
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

    /**
     * Invokes a JSON-RPC method and returns a future for the typed response.
     *
     * @param <T>        the expected response type
     * @param method     the JSON-RPC method name
     * @param params     the request parameters (may be a {@code Map}, DTO record, or {@code JsonNode})
     * @param resultType the Jackson {@link JavaType} of the expected response type
     * @return a {@link CompletableFuture} that completes with the deserialized result
     */
    default <T> CompletableFuture<T> invoke(String method, Object params, JavaType resultType) {
        if (resultType.hasRawClass(Void.class) || resultType.hasRawClass(Void.TYPE)) {
            return invoke(method, params, Void.class).thenApply(ignored -> null);
        }
        return invoke(method, params, JsonNode.class).thenApply(result -> {
            try {
                return RpcMapper.INSTANCE.readerFor(resultType).readValue(result);
            } catch (java.io.IOException e) {
                throw new CompletionException(e);
            }
        });
    }
}
