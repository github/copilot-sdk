/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Exception thrown when a JSON-RPC error occurs during communication with the
 * Copilot CLI server.
 * <p>
 * This exception wraps error responses from the JSON-RPC protocol, including
 * the error code, message, and optional raw JSON data returned by the server.
 * Calls to {@link java.util.concurrent.CompletableFuture#get()} wrap this
 * exception in an {@link java.util.concurrent.ExecutionException}; calls to
 * {@link java.util.concurrent.CompletableFuture#join()} wrap it in a
 * {@link java.util.concurrent.CompletionException}.
 *
 * @since 1.0.15-preview.1
 */
public final class JsonRpcException extends RuntimeException {

    // Preserve exceptions serialized before this class became public and gained
    // data.
    private static final long serialVersionUID = -2127886082879272410L;

    private final int code;
    private final JsonNode data;

    /**
     * Creates a new JSON-RPC exception.
     *
     * @param code
     *            the JSON-RPC error code
     * @param message
     *            the error message from the server
     */
    public JsonRpcException(int code, String message) {
        this(code, message, null);
    }

    /**
     * Creates a new JSON-RPC exception with optional raw JSON error data.
     *
     * @param code
     *            the JSON-RPC error code
     * @param message
     *            the error message from the server
     * @param data
     *            the error data, or {@code null} if omitted; a JSON null is
     *            represented by a
     *            {@link com.fasterxml.jackson.databind.node.NullNode}
     * @since 1.0.15-preview.1
     */
    public JsonRpcException(int code, String message, JsonNode data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    /**
     * Returns the JSON-RPC error code.
     * <p>
     * Standard JSON-RPC error codes include:
     * <ul>
     * <li>-32700: Parse error</li>
     * <li>-32600: Invalid request</li>
     * <li>-32601: Method not found</li>
     * <li>-32602: Invalid params</li>
     * <li>-32603: Internal error</li>
     * </ul>
     *
     * @return the error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the raw JSON error data without converting its value or shape.
     * <p>
     * An omitted data member is represented by Java {@code null}. An explicit JSON
     * null is represented by a
     * {@link com.fasterxml.jackson.databind.node.NullNode}. This value is not
     * included in the exception's message or string representation. The node is
     * shared, not defensively copied; callers that modify container nodes should
     * first use {@link JsonNode#deepCopy()}.
     *
     * @return the error data, or {@code null} if the server omitted it
     * @since 1.0.15-preview.1
     */
    public JsonNode getData() {
        return data;
    }
}
