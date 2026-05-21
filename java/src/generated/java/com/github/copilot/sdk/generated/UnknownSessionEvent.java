/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Fallback for event types not yet known to this SDK version.
 * <p>
 * {@link #getType()} returns the original type string from the JSON payload,
 * preserving forward compatibility with event types introduced by newer CLI versions.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UnknownSessionEvent extends SessionEvent {

    @JsonProperty("type")
    private String type = "unknown";

    @Override
    public String getType() { return type; }
}
