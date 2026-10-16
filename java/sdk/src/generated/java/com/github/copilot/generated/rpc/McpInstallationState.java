/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Configuration ownership and setup observations, distinct from tool permissions.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpInstallationState {
    /** The {@code needs-setup} variant. */
    NEEDS_SETUP("needs-setup"),
    /** The {@code active} variant. */
    ACTIVE("active"),
    /** The {@code authentication-required} variant. */
    AUTHENTICATION_REQUIRED("authentication-required"),
    /** The {@code activation-failed} variant. */
    ACTIVATION_FAILED("activation-failed"),
    /** The {@code configuration-modified} variant. */
    CONFIGURATION_MODIFIED("configuration-modified"),
    /** The {@code recovery-required} variant. */
    RECOVERY_REQUIRED("recovery-required");

    private final String value;
    McpInstallationState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpInstallationState fromValue(String value) {
        for (McpInstallationState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpInstallationState value: " + value);
    }
}
