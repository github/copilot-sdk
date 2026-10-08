/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Copilot plan tier used by the session quota projection.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionQuotaPlanTier {
    /** The {@code free} variant. */
    FREE("free"),
    /** The {@code edu} variant. */
    EDU("edu"),
    /** The {@code pro} variant. */
    PRO("pro"),
    /** The {@code pro_plus} variant. */
    PRO_PLUS("pro_plus"),
    /** The {@code business} variant. */
    BUSINESS("business"),
    /** The {@code enterprise} variant. */
    ENTERPRISE("enterprise"),
    /** The {@code max} variant. */
    MAX("max"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    SessionQuotaPlanTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionQuotaPlanTier fromValue(String value) {
        for (SessionQuotaPlanTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionQuotaPlanTier value: " + value);
    }
}
