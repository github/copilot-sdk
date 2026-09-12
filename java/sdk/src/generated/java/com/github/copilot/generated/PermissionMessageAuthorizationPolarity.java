/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Which direction a message-backed authorization claim moves authority in.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionMessageAuthorizationPolarity {
    /** The {@code grant} variant. */
    GRANT("grant"),
    /** The {@code denial} variant. */
    DENIAL("denial");

    private final String value;
    PermissionMessageAuthorizationPolarity(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionMessageAuthorizationPolarity fromValue(String value) {
        for (PermissionMessageAuthorizationPolarity v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionMessageAuthorizationPolarity value: " + value);
    }
}
