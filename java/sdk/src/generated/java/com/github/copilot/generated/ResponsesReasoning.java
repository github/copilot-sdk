/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Original request-level and effective conversation reasoning effort for a Responses history boundary
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponsesReasoning(
    /** Provider model whose reasoning settings this boundary records */
    @JsonProperty("model") String model,
    /** Original request-level effort, retained while replaying this conversation prefix */
    @JsonProperty("initialEffort") String initialEffort,
    /** Effective effort selected before this message, independent of the response-level reasoning field */
    @JsonProperty("effort") String effort
) {
}
