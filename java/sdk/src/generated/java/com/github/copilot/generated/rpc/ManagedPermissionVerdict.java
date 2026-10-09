/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Managed-policy verdict only: deny blocks the URL, ask requires approval, allow approves under managed policy, and unmanaged leaves the normal consumer permission flow in effect. No verdict bypasses other security controls.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ManagedPermissionVerdict {
    /** The {@code deny} variant. */
    DENY("deny"),
    /** The {@code ask} variant. */
    ASK("ask"),
    /** The {@code allow} variant. */
    ALLOW("allow"),
    /** The {@code unmanaged} variant. */
    UNMANAGED("unmanaged");

    private final String value;
    ManagedPermissionVerdict(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedPermissionVerdict fromValue(String value) {
        for (ManagedPermissionVerdict v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedPermissionVerdict value: " + value);
    }
}
