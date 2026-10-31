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
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Fixed timestamps for one observed operation revision.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RuntimeLifecycleOperationTiming(
    /** ISO 8601 timestamp when the operation began. */
    @JsonProperty("startedAt") OffsetDateTime startedAt,
    /** ISO 8601 timestamp when the current phase began. */
    @JsonProperty("phaseStartedAt") OffsetDateTime phaseStartedAt,
    /** ISO 8601 timestamp when this operation revision was recorded. */
    @JsonProperty("transitionedAt") OffsetDateTime transitionedAt
) {
}
