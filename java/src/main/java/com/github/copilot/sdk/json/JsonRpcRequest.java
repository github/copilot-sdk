/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 request structure.
 * <p>
 * This is an internal class representing the wire format of a JSON-RPC request.
 * It follows the JSON-RPC 2.0 specification.
 *
 * @see JsonRpcResponse
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0
 *      Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JsonRpcRequest {

    @JsonProperty("jsonrpc")
    private String jsonrpc;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Object params;

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
     * Gets the request ID.
     *
     * @return the request identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the request ID.
     *
     * @param id
     *            the request identifier
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the method name.
     *
     * @return the RPC method to invoke
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the method name.
     *
     * @param method
     *            the RPC method to invoke
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Gets the method parameters.
     *
     * @return the parameters object
     */
    public Object getParams() {
        return params;
    }

    /**
     * Sets the method parameters.
     *
     * @param params
     *            the parameters object
     */
    public void setParams(Object params) {
        this.params = params;
    }
}
