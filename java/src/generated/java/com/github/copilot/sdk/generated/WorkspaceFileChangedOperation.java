/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Whether the file was newly created or updated
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkspaceFileChangedOperation {
    /** The {@code create} variant. */
    CREATE("create"),
    /** The {@code update} variant. */
    UPDATE("update");

    private final String value;
    WorkspaceFileChangedOperation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkspaceFileChangedOperation fromValue(String value) {
        for (WorkspaceFileChangedOperation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkspaceFileChangedOperation value: " + value);
    }
}
