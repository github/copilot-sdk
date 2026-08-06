/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Disposition of a permission request as observed by the responding client.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionDecisionOutcome {
    /** The {@code auto_approved} variant. */
    AUTO_APPROVED("auto_approved"),
    /** The {@code autopilot_denied} variant. */
    AUTOPILOT_DENIED("autopilot_denied"),
    /** The {@code prompted_user} variant. */
    PROMPTED_USER("prompted_user");

    private final String value;
    PermissionDecisionOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionDecisionOutcome fromValue(String value) {
        for (PermissionDecisionOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionDecisionOutcome value: " + value);
    }
}
