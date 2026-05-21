/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Where this source lives — used for UI grouping
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum InstructionsSourcesLocation {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code repository} variant. */
    REPOSITORY("repository"),
    /** The {@code working-directory} variant. */
    WORKING_DIRECTORY("working-directory");

    private final String value;
    InstructionsSourcesLocation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static InstructionsSourcesLocation fromValue(String value) {
        for (InstructionsSourcesLocation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown InstructionsSourcesLocation value: " + value);
    }
}
