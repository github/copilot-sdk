/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The handshake failed for a reason other than authentication.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class McpOauthTestConnectionResultFailed extends McpOauthTestConnectionResult {

    @JsonProperty("status")
    private final String status = "failed";

    @Override
    public String getStatus() { return status; }

    /** Transport or session policy failure detail. */
    @JsonProperty("error")
    private String error;

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
