/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * For HTTP 400 failures only: whether the response carried a structured CAPI error envelope (structured_error, a deterministic validation failure) or no error body (bodyless, the transient gateway/proxy signature). Absent for non-400 failures.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelCallFailureBadRequestKind {
    /** The {@code bodyless} variant. */
    BODYLESS("bodyless"),
    /** The {@code structured_error} variant. */
    STRUCTURED_ERROR("structured_error");

    private final String value;
    ModelCallFailureBadRequestKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelCallFailureBadRequestKind fromValue(String value) {
        for (ModelCallFailureBadRequestKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelCallFailureBadRequestKind value: " + value);
    }
}
