/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * The operation that requires a policy-helper response.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum ManagedSettingsPolicyHelperReason {
    /** The {@code sessionStart} variant. */
    SESSIONSTART("sessionStart"),
    /** The {@code pluginActivation} variant. */
    PLUGINACTIVATION("pluginActivation");

    private final String value;
    ManagedSettingsPolicyHelperReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsPolicyHelperReason fromValue(String value) {
        for (ManagedSettingsPolicyHelperReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsPolicyHelperReason value: " + value);
    }
}
