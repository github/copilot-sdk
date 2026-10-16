/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Administrator-configured Ensemble preference, independent of execution support.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ManagedEnsembleTier {
    /** The {@code balance} variant. */
    BALANCE("balance"),
    /** The {@code high} variant. */
    HIGH("high"),
    /** The {@code max} variant. */
    MAX("max"),
    /** The {@code unmanaged} variant. */
    UNMANAGED("unmanaged");

    private final String value;
    ManagedEnsembleTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedEnsembleTier fromValue(String value) {
        for (ManagedEnsembleTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedEnsembleTier value: " + value);
    }
}
