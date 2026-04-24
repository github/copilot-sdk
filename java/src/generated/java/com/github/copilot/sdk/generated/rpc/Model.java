/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Model(
    /** Model identifier (e.g., "claude-sonnet-4.5") */
    @JsonProperty("id") String id,
    /** Display name */
    @JsonProperty("name") String name,
    /** Model capabilities and limits */
    @JsonProperty("capabilities") ModelCapabilities capabilities,
    /** Policy state (if applicable) */
    @JsonProperty("policy") ModelPolicy policy,
    /** Billing information */
    @JsonProperty("billing") ModelBilling billing,
    /** Supported reasoning effort levels (only present if model supports reasoning effort) */
    @JsonProperty("supportedReasoningEfforts") List<String> supportedReasoningEfforts,
    /** Default reasoning effort level (only present if model supports reasoning effort) */
    @JsonProperty("defaultReasoningEffort") String defaultReasoningEffort
) {
}
