/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Canonical digest algorithm for a validated MCP card
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CardDigestAlgorithm {
    /** The {@code sha256-rfc8785} variant. */
    SHA256_RFC8785("sha256-rfc8785");

    private final String value;
    CardDigestAlgorithm(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CardDigestAlgorithm fromValue(String value) {
        for (CardDigestAlgorithm v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CardDigestAlgorithm value: " + value);
    }
}
