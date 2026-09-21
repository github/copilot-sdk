/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Lifecycle state of a permission-recovery episode
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionRecoveryStatus {
    /** The {@code recovering} variant. */
    RECOVERING("recovering"),
    /** The {@code awaiting_approval} variant. */
    AWAITING_APPROVAL("awaiting_approval"),
    /** The {@code resolved} variant. */
    RESOLVED("resolved"),
    /** The {@code blocked} variant. */
    BLOCKED("blocked");

    private final String value;
    PermissionRecoveryStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionRecoveryStatus fromValue(String value) {
        for (PermissionRecoveryStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionRecoveryStatus value: " + value);
    }
}
