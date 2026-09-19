/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Runtime handling applied to a recovery attempt
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionRecoveryAttemptDisposition {
    /** The {@code deferred} variant. */
    DEFERRED("deferred"),
    /** The {@code prompted} variant. */
    PROMPTED("prompted"),
    /** The {@code approved} variant. */
    APPROVED("approved"),
    /** The {@code denied} variant. */
    DENIED("denied"),
    /** The {@code blocked} variant. */
    BLOCKED("blocked"),
    /** The {@code succeeded} variant. */
    SUCCEEDED("succeeded");

    private final String value;
    PermissionRecoveryAttemptDisposition(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionRecoveryAttemptDisposition fromValue(String value) {
        for (PermissionRecoveryAttemptDisposition v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionRecoveryAttemptDisposition value: " + value);
    }
}
