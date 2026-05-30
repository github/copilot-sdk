/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether the shell runs inside a managed PTY session or as an independent background process
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum TaskShellInfoAttachmentMode {
    /** The {@code attached} variant. */
    ATTACHED("attached"),
    /** The {@code detached} variant. */
    DETACHED("detached");

    private final String value;
    TaskShellInfoAttachmentMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static TaskShellInfoAttachmentMode fromValue(String value) {
        for (TaskShellInfoAttachmentMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown TaskShellInfoAttachmentMode value: " + value);
    }
}
