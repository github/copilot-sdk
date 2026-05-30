/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Theme variant this icon is intended for
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ToolExecutionCompleteContentResourceLinkIconTheme {
    /** The {@code light} variant. */
    LIGHT("light"),
    /** The {@code dark} variant. */
    DARK("dark");

    private final String value;
    ToolExecutionCompleteContentResourceLinkIconTheme(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ToolExecutionCompleteContentResourceLinkIconTheme fromValue(String value) {
        for (ToolExecutionCompleteContentResourceLinkIconTheme v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ToolExecutionCompleteContentResourceLinkIconTheme value: " + value);
    }
}
