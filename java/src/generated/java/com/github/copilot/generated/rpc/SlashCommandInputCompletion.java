/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SlashCommandInputCompletion {
    /** The {@code directory} variant. */
    DIRECTORY("directory");

    private final String value;
    SlashCommandInputCompletion(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SlashCommandInputCompletion fromValue(String value) {
        for (SlashCommandInputCompletion v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SlashCommandInputCompletion value: " + value);
    }
}
