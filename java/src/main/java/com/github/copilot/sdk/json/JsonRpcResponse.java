/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 response structure.
 * <p>
 * This is an internal class representing the wire format of a JSON-RPC
 * response. It follows the JSON-RPC 2.0 specification. A response contains
 * either a result or an error, but not both.
 *
 * @see JsonRpcRequest
 * @see JsonRpcError
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0
 *      Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JsonRpcResponse {

    @JsonProperty("jsonrpc")
    private String jsonrpc;

    @JsonProperty("id")
    private Object id;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private JsonRpcError error;

    /**
     * Gets the JSON-RPC version.
     *
     * @return the version string (should be "2.0")
     */
    public String getJsonrpc() {
        return jsonrpc;
    }

    /**
     * Sets the JSON-RPC version.
     *
     * @param jsonrpc
     *            the version string
     */
    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    /**
     * Gets the response ID.
     *
     * @return the request identifier this response corresponds to
     */
    public Object getId() {
        return id;
    }

    /**
     * Sets the response ID.
     *
     * @param id
     *            the response identifier
     */
    public void setId(Object id) {
        this.id = id;
    }

    /**
     * Gets the result of the RPC call.
     *
     * @return the result object, or {@code null} if there was an error
     */
    public Object getResult() {
        return result;
    }

    /**
     * Sets the result of the RPC call.
     *
     * @param result
     *            the result object
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * Gets the error if the RPC call failed.
     *
     * @return the error object, or {@code null} if successful
     */
    public JsonRpcError getError() {
        return error;
    }

    /**
     * Sets the error for a failed RPC call.
     *
     * @param error
     *            the error object
     */
    public void setError(JsonRpcError error) {
        this.error = error;
    }
}
