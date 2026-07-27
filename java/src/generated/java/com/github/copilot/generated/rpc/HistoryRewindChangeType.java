/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Aggregate file change represented by a rewind preview.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HistoryRewindChangeType {
    /** The {@code created} variant. */
    CREATED("created"),
    /** The {@code deleted} variant. */
    DELETED("deleted"),
    /** The {@code modified} variant. */
    MODIFIED("modified");

    private final String value;
    HistoryRewindChangeType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HistoryRewindChangeType fromValue(String value) {
        for (HistoryRewindChangeType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HistoryRewindChangeType value: " + value);
    }
}
