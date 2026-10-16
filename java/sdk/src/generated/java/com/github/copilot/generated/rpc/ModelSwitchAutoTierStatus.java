/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Whether the requested preference was already effective or was accepted for later transactional activation.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelSwitchAutoTierStatus {
    /** The {@code unchanged} variant. */
    UNCHANGED("unchanged"),
    /** The {@code pending} variant. */
    PENDING("pending");

    private final String value;
    ModelSwitchAutoTierStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelSwitchAutoTierStatus fromValue(String value) {
        for (ModelSwitchAutoTierStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelSwitchAutoTierStatus value: " + value);
    }
}
