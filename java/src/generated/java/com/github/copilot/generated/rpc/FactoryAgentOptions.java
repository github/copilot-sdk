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
 * Options for one factory-scoped subagent call.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryAgentOptions(
    /** Optional label distinguishing otherwise identical memoized agent calls. */
    @JsonProperty("label") String label,
    /** Optional JSON Schema for structured agent output. */
    @JsonProperty("schema") Object schema,
    /** Optional model identifier for the subagent. */
    @JsonProperty("model") String model
) {
}
