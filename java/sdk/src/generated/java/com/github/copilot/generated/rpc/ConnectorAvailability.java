/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Availability of the EXPERIMENTAL session connector API.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ConnectorAvailability {
    /** The {@code enabled} variant. */
    ENABLED("enabled"),
    /** The {@code disabled} variant. */
    DISABLED("disabled"),
    /** The {@code unavailable} variant. */
    UNAVAILABLE("unavailable");

    private final String value;
    ConnectorAvailability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ConnectorAvailability fromValue(String value) {
        for (ConnectorAvailability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ConnectorAvailability value: " + value);
    }
}
