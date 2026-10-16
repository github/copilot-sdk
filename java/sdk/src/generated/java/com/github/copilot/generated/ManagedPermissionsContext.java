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
 * Reusable managed permission evaluation context. Treat permissions as runtime-owned policy data and retain it verbatim; it includes source-aware composition metadata. This is a snapshot, not a capability or approval token; only use contexts obtained from trusted policy sources.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedPermissionsContext(
    /** Runtime-owned composed permissions object. Absent when no permission policy is configured. */
    @JsonProperty("permissions") Object permissions,
    /** Policy could not be determined; evaluation must deny every operation. */
    @JsonProperty("failClosed") Boolean failClosed
) {
}
