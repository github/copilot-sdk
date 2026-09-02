/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The runtime could not reach the catalog authority or retrieve a card. Covers being offline as well as transport-level failure.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogNetworkFailureError extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "network-failure";

    @Override
    public String getKind() { return kind; }

    /** Categorised failure, low cardinality so it can be aggregated without carrying a URL. */
    @JsonProperty("reason")
    private CatalogNetworkFailureReason reason;

    /** HTTP status code, when the failure was a rejected response. */
    @JsonProperty("statusCode")
    private Long statusCode;

    /** Bounded cooldown in seconds before another catalog request should be attempted, when the authority supplied a numeric Retry-After value or the runtime applied its documented fallback. */
    @JsonProperty("retryAfterSeconds")
    private Long retryAfterSeconds;

    /** Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogNetworkFailureReason getReason() { return reason; }
    public void setReason(CatalogNetworkFailureReason reason) { this.reason = reason; }

    public Long getStatusCode() { return statusCode; }
    public void setStatusCode(Long statusCode) { this.statusCode = statusCode; }

    public Long getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(Long retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
