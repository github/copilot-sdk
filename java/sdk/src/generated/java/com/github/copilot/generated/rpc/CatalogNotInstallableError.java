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
 * The candidate is discoverable but cannot be installed. `application/ai-skill` resolves here, because it stays searchable while remaining typed non-installable.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogNotInstallableError extends McpPlanInstallResult {

    @JsonProperty("kind")
    private final String kind = "not-installable";

    @Override
    public String getKind() { return kind; }

    /** Why the candidate cannot be installed. */
    @JsonProperty("reason")
    private CatalogNotInstallableReason reason;

    /** Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogNotInstallableReason getReason() { return reason; }
    public void setReason(CatalogNotInstallableReason reason) { this.reason = reason; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
