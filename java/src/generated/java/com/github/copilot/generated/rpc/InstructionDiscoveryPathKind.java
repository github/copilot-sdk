/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether the target is a single file or a directory of instruction files
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum InstructionDiscoveryPathKind {
    /** The {@code file} variant. */
    FILE("file"),
    /** The {@code directory} variant. */
    DIRECTORY("directory");

    private final String value;
    InstructionDiscoveryPathKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static InstructionDiscoveryPathKind fromValue(String value) {
        for (InstructionDiscoveryPathKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown InstructionDiscoveryPathKind value: " + value);
    }
}
