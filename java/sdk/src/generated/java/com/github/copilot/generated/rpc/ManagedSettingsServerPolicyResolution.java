/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * How the server layer of an ingestion was resolved.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum ManagedSettingsServerPolicyResolution {
    /** The {@code live} variant. */
    LIVE("live"),
    /** The {@code freshCache} variant. */
    FRESHCACHE("freshCache"),
    /** The {@code cacheMiss} variant. */
    CACHEMISS("cacheMiss"),
    /** The {@code fallbackCache} variant. */
    FALLBACKCACHE("fallbackCache"),
    /** The {@code failOpen} variant. */
    FAILOPEN("failOpen"),
    /** The {@code failClosed} variant. */
    FAILCLOSED("failClosed"),
    /** The {@code skippedNoToken} variant. */
    SKIPPEDNOTOKEN("skippedNoToken");

    private final String value;
    ManagedSettingsServerPolicyResolution(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsServerPolicyResolution fromValue(String value) {
        for (ManagedSettingsServerPolicyResolution v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsServerPolicyResolution value: " + value);
    }
}
