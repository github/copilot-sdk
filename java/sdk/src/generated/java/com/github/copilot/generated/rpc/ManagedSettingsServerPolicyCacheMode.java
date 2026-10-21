/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * How an ingestion may use the on-disk server-policy cache.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum ManagedSettingsServerPolicyCacheMode {
    /** The {@code preferFresh} variant. */
    PREFERFRESH("preferFresh"),
    /** The {@code cacheOnly} variant. */
    CACHEONLY("cacheOnly"),
    /** The {@code fallbackOnly} variant. */
    FALLBACKONLY("fallbackOnly"),
    /** The {@code disabled} variant. */
    DISABLED("disabled");

    private final String value;
    ManagedSettingsServerPolicyCacheMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsServerPolicyCacheMode fromValue(String value) {
        for (ManagedSettingsServerPolicyCacheMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsServerPolicyCacheMode value: " + value);
    }
}
