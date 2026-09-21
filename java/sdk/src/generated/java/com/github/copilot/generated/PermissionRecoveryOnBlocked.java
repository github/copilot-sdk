/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Action selected when autonomous recovery cannot continue
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionRecoveryOnBlocked {
    /** The {@code ask} variant. */
    ASK("ask"),
    /** The {@code fail} variant. */
    FAIL("fail");

    private final String value;
    PermissionRecoveryOnBlocked(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionRecoveryOnBlocked fromValue(String value) {
        for (PermissionRecoveryOnBlocked v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionRecoveryOnBlocked value: " + value);
    }
}
