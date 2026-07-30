/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * The system that produced a citation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CitationProvider {
    /** The {@code anthropic} variant. */
    ANTHROPIC("anthropic"),
    /** The {@code openai} variant. */
    OPENAI("openai"),
    /** The {@code client} variant. */
    CLIENT("client");

    private final String value;
    CitationProvider(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CitationProvider fromValue(String value) {
        for (CitationProvider v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CitationProvider value: " + value);
    }
}
