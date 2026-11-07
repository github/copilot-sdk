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
 * One callable decision model.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionModelInfo(
    /** Model id to pass as `model` to `decisions.ask`. */
    @JsonProperty("id") String id,
    /** Display name. */
    @JsonProperty("name") String name,
    /** Whether `decisions.ask` uses this model when `model` is omitted. */
    @JsonProperty("isDefault") Boolean isDefault,
    /** Effective limits callers can rely on: the stricter of the backend's and the runtime's. */
    @JsonProperty("limits") DecisionModelLimits limits,
    /** Whether the backend can refuse individual questions. */
    @JsonProperty("supportsRefusals") Boolean supportsRefusals
) {
}
