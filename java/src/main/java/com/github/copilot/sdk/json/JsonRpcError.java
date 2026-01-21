/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 error structure.
 * <p>
 * This is an internal class representing an error in a JSON-RPC response. It
 * contains an error code, message, and optional additional data.
 *
 * <h2>Standard Error Codes</h2>
 * <ul>
 * <li>-32700: Parse error</li>
 * <li>-32600: Invalid Request</li>
 * <li>-32601: Method not found</li>
 * <li>-32602: Invalid params</li>
 * <li>-32603: Internal error</li>
 * </ul>
 *
 * @see JsonRpcResponse
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC
 *      Error Object</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JsonRpcError {

    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data;

    /**
     * Gets the error code.
     *
     * @return the integer error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Sets the error code.
     *
     * @param code
     *            the integer error code
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * Gets the error message.
     *
     * @return the human-readable error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the error message.
     *
     * @param message
     *            the error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the additional error data.
     *
     * @return the additional data, or {@code null} if none
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the additional error data.
     *
     * @param data
     *            the additional data
     */
    public void setData(Object data) {
        this.data = data;
    }
}
