/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Which layer disabled bypass-permissions mode. The layer matters because only a server-delivered disable is pinned to an account and can later be lifted by signing in elsewhere.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum ManagedSettingsBypassDisableSource {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code managed} variant. */
    MANAGED("managed"),
    /** The {@code mdm} variant. */
    MDM("mdm");

    private final String value;
    ManagedSettingsBypassDisableSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsBypassDisableSource fromValue(String value) {
        for (ManagedSettingsBypassDisableSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsBypassDisableSource value: " + value);
    }
}
