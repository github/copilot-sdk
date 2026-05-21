/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Message role: "system" for system prompts, "developer" for developer-injected instructions
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SystemMessageRole {
    /** The {@code system} variant. */
    SYSTEM("system"),
    /** The {@code developer} variant. */
    DEVELOPER("developer");

    private final String value;
    SystemMessageRole(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SystemMessageRole fromValue(String value) {
        for (SystemMessageRole v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SystemMessageRole value: " + value);
    }
}
