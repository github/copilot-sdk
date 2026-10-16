/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Which channel registered the policy helper that produced a helper layer.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum ManagedSettingsPolicyHelperAuthority {
    /** The {@code device} variant. */
    DEVICE("device"),
    /** The {@code server} variant. */
    SERVER("server");

    private final String value;
    ManagedSettingsPolicyHelperAuthority(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsPolicyHelperAuthority fromValue(String value) {
        for (ManagedSettingsPolicyHelperAuthority v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsPolicyHelperAuthority value: " + value);
    }
}
