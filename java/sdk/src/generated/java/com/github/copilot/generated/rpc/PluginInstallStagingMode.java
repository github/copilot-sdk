/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Where completed plugin content was staged before atomic promotion.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PluginInstallStagingMode {
    /** The {@code external} variant. */
    EXTERNAL("external"),
    /** The {@code destination_sibling} variant. */
    DESTINATION_SIBLING("destination_sibling");

    private final String value;
    PluginInstallStagingMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PluginInstallStagingMode fromValue(String value) {
        for (PluginInstallStagingMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PluginInstallStagingMode value: " + value);
    }
}
