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
 * Registry or enterprise policy refused the operation.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogPolicyRejectedError extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "policy-rejected";

    @Override
    public String getKind() { return kind; }

    /** Which authority produced the decision. */
    @JsonProperty("source")
    private McpPlanPolicySource source;

    /** Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public McpPlanPolicySource getSource() { return source; }
    public void setSource(McpPlanPolicySource source) { this.source = source; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
