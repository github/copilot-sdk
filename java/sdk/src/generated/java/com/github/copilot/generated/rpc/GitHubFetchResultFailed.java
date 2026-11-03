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
 * Variant {@code failed} of {@link GitHubFetchResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class GitHubFetchResultFailed extends GitHubFetchResult {

    @JsonProperty("kind")
    private final String kind = "failed";

    @Override
    public String getKind() { return kind; }

    /** Authentication, transport, or cancellation error. */
    @JsonProperty("message")
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
