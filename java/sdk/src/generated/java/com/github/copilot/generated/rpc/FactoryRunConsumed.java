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
 * Durable factory resource consumption.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryRunConsumed(
    /** Accumulated active execution time in milliseconds. */
    @JsonProperty("activeMs") Long activeMs,
    /** Total subagents spawned by the run. */
    @JsonProperty("subagents") Long subagents,
    /** AI usage consumed by the run in nano-AIU. */
    @JsonProperty("nanoAiu") Long nanoAiu
) {
}
