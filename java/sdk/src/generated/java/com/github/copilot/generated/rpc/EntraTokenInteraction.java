/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * How far OneAuth may go to acquire the requested token.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum EntraTokenInteraction {
    /** The {@code silent} variant. */
    SILENT("silent"),
    /** The {@code interactive} variant. */
    INTERACTIVE("interactive"),
    /** The {@code force-interactive} variant. */
    FORCE_INTERACTIVE("force-interactive");

    private final String value;
    EntraTokenInteraction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static EntraTokenInteraction fromValue(String value) {
        for (EntraTokenInteraction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown EntraTokenInteraction value: " + value);
    }
}
