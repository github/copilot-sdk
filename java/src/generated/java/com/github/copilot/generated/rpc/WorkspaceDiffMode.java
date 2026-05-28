/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Diff mode requested by the client.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkspaceDiffMode {
    /** The {@code unstaged} variant. */
    UNSTAGED("unstaged"),
    /** The {@code branch} variant. */
    BRANCH("branch");

    private final String value;
    WorkspaceDiffMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkspaceDiffMode fromValue(String value) {
        for (WorkspaceDiffMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkspaceDiffMode value: " + value);
    }
}
