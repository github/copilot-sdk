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
 * Counts for background agents owned by the session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionBackgroundAgentActivity(
    /** Agents currently executing. */
    @JsonProperty("running") Long running,
    /** Live multi-turn agents parked for another message. */
    @JsonProperty("idle") Long idle,
    /** Running agents accepted by the scoped background-agent cancellation operation. */
    @JsonProperty("cancelable") Long cancelable
) {
}
