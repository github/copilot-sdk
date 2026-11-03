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
 * Variant {@code proxy-response} of {@link GitHubFetchResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class GitHubFetchResultProxyResponse extends GitHubFetchResult {

    @JsonProperty("kind")
    private final String kind = "proxy-response";

    @Override
    public String getKind() { return kind; }

    /** Complete proxy failure message. */
    @JsonProperty("message")
    private String message;

    /** HTTP proxy status. */
    @JsonProperty("status")
    private Long status;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getStatus() { return status; }
    public void setStatus(Long status) { this.status = status; }
}
