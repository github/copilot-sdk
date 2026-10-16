/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Severity of a managed-settings validation finding.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ManagedSettingsDiagnosticSeverity {
    /** The {@code error} variant. */
    ERROR("error"),
    /** The {@code warning} variant. */
    WARNING("warning");

    private final String value;
    ManagedSettingsDiagnosticSeverity(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsDiagnosticSeverity fromValue(String value) {
        for (ManagedSettingsDiagnosticSeverity v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsDiagnosticSeverity value: " + value);
    }
}
