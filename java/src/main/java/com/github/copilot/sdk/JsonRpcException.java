/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

/**
 * Exception thrown when a JSON-RPC error occurs during communication with the
 * Copilot CLI server.
 * <p>
 * This exception wraps error responses from the JSON-RPC protocol, including
 * the error code and message returned by the server.
 */
final class JsonRpcException extends RuntimeException {

    private final int code;

    /**
     * Creates a new JSON-RPC exception.
     *
     * @param code
     *            the JSON-RPC error code
     * @param message
     *            the error message from the server
     */
    public JsonRpcException(int code, String message) {
        super(message);
        this.code = code;
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
}
