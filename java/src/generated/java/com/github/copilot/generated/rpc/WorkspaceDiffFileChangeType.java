/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Type of change represented by this file diff.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkspaceDiffFileChangeType {
    /** The {@code added} variant. */
    ADDED("added"),
    /** The {@code modified} variant. */
    MODIFIED("modified"),
    /** The {@code deleted} variant. */
    DELETED("deleted"),
    /** The {@code renamed} variant. */
    RENAMED("renamed");

    private final String value;
    WorkspaceDiffFileChangeType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkspaceDiffFileChangeType fromValue(String value) {
        for (WorkspaceDiffFileChangeType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkspaceDiffFileChangeType value: " + value);
    }
}
