/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Client surface that submitted a permission response.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionDecisionSurface {
    /** The {@code tui} variant. */
    TUI("tui"),
    /** The {@code prompt_mode} variant. */
    PROMPT_MODE("prompt_mode"),
    /** The {@code copilot_app} variant. */
    COPILOT_APP("copilot_app"),
    /** The {@code sdk} variant. */
    SDK("sdk");

    private final String value;
    PermissionDecisionSurface(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionDecisionSurface fromValue(String value) {
        for (PermissionDecisionSurface v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionDecisionSurface value: " + value);
    }
}
