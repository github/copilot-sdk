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
 * Outcome of evaluating the planned server against registry and enterprise policy. Evaluation is read-only.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanPolicyResult(
    /** What policy decided for this server. */
    @JsonProperty("decision") McpPlanPolicyDecision decision,
    /** Which authority produced the decision. */
    @JsonProperty("source") McpPlanPolicySource source,
    /** Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret. */
    @JsonProperty("reason") String reason
) {
}
