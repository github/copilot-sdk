/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Capacity is independent of whether a numeric balance was supplied.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderQuotaCapacityState {
    /** The {@code available} variant. */
    AVAILABLE("available"),
    /** The {@code exhausted} variant. */
    EXHAUSTED("exhausted"),
    /** The {@code unlimited} variant. */
    UNLIMITED("unlimited"),
    /** The {@code not_required} variant. */
    NOT_REQUIRED("not_required"),
    /** The {@code not_applicable} variant. */
    NOT_APPLICABLE("not_applicable"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown"),
    /** The {@code unavailable} variant. */
    UNAVAILABLE("unavailable");

    private final String value;
    ProviderQuotaCapacityState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderQuotaCapacityState fromValue(String value) {
        for (ProviderQuotaCapacityState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderQuotaCapacityState value: " + value);
    }
}
