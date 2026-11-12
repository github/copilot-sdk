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
 * A presented handle was not accepted. Handles are runtime-instance scoped, TTL-bound, and single-use, so each way of failing is reported distinctly.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogHandleRejectedError extends McpPlanInstallResult {

    @JsonProperty("kind")
    private final String kind = "handle-rejected";

    @Override
    public String getKind() { return kind; }

    /** Which kind of handle was presented. */
    @JsonProperty("handleType")
    private CatalogHandleType handleType;

    /** Why the handle was rejected. */
    @JsonProperty("reason")
    private CatalogHandleRejectionReason reason;

    /** Human-readable explanation, safe to surface. Never contains the handle itself, nor a query, URL, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogHandleType getHandleType() { return handleType; }
    public void setHandleType(CatalogHandleType handleType) { this.handleType = handleType; }

    public CatalogHandleRejectionReason getReason() { return reason; }
    public void setReason(CatalogHandleRejectionReason reason) { this.reason = reason; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
