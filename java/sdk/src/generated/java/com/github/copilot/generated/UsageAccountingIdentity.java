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
 * Ordered accounting identity assigned under the source session's emission lock.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageAccountingIdentity(
    /** Session that assigned this accounting sequence. */
    @JsonProperty("sourceSessionId") String sourceSessionId,
    /** Monotonically increasing sequence within the source session. */
    @JsonProperty("sequence") Long sequence,
    /** Existing API call identifier, or a runtime-generated identity when none was supplied. */
    @JsonProperty("usageId") String usageId
) {
}
