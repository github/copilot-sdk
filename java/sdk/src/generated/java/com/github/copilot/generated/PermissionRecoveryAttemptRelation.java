/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Relationship of an attempt to earlier permission requests
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionRecoveryAttemptRelation {
    /** The {@code initial} variant. */
    INITIAL("initial"),
    /** The {@code retry} variant. */
    RETRY("retry"),
    /** The {@code alternative} variant. */
    ALTERNATIVE("alternative");

    private final String value;
    PermissionRecoveryAttemptRelation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionRecoveryAttemptRelation fromValue(String value) {
        for (PermissionRecoveryAttemptRelation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionRecoveryAttemptRelation value: " + value);
    }
}
