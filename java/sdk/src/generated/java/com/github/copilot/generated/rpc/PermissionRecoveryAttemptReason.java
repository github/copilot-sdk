/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Controlled reason for an individual attempt disposition
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionRecoveryAttemptReason {
    /** The {@code permission_required} variant. */
    PERMISSION_REQUIRED("permission_required"),
    /** The {@code repeated_attempt} variant. */
    REPEATED_ATTEMPT("repeated_attempt"),
    /** The {@code attempts_exhausted} variant. */
    ATTEMPTS_EXHAUSTED("attempts_exhausted"),
    /** The {@code permission_approved} variant. */
    PERMISSION_APPROVED("permission_approved"),
    /** The {@code permission_denied} variant. */
    PERMISSION_DENIED("permission_denied"),
    /** The {@code responder_unavailable} variant. */
    RESPONDER_UNAVAILABLE("responder_unavailable"),
    /** The {@code equivalent_alternative_succeeded} variant. */
    EQUIVALENT_ALTERNATIVE_SUCCEEDED("equivalent_alternative_succeeded");

    private final String value;
    PermissionRecoveryAttemptReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionRecoveryAttemptReason fromValue(String value) {
        for (PermissionRecoveryAttemptReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionRecoveryAttemptReason value: " + value);
    }
}
