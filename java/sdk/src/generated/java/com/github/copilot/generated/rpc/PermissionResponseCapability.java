/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Response capability available to the client when it settled a permission request.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionResponseCapability {
    /** The {@code interactive} variant. */
    INTERACTIVE("interactive"),
    /** The {@code headless} variant. */
    HEADLESS("headless"),
    /** The {@code none} variant. */
    NONE("none");

    private final String value;
    PermissionResponseCapability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionResponseCapability fromValue(String value) {
        for (PermissionResponseCapability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionResponseCapability value: " + value);
    }
}
