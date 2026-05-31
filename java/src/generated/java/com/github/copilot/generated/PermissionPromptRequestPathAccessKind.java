/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Underlying permission kind that needs path approval
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionPromptRequestPathAccessKind {
    /** The {@code read} variant. */
    READ("read"),
    /** The {@code shell} variant. */
    SHELL("shell"),
    /** The {@code write} variant. */
    WRITE("write");

    private final String value;
    PermissionPromptRequestPathAccessKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionPromptRequestPathAccessKind fromValue(String value) {
        for (PermissionPromptRequestPathAccessKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionPromptRequestPathAccessKind value: " + value);
    }
}
