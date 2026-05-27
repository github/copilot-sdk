/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Platform type for responsive design
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpAppsSetHostContextDetailsPlatform {
    /** The {@code web} variant. */
    WEB("web"),
    /** The {@code desktop} variant. */
    DESKTOP("desktop"),
    /** The {@code mobile} variant. */
    MOBILE("mobile");

    private final String value;
    McpAppsSetHostContextDetailsPlatform(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpAppsSetHostContextDetailsPlatform fromValue(String value) {
        for (McpAppsSetHostContextDetailsPlatform v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpAppsSetHostContextDetailsPlatform value: " + value);
    }
}
