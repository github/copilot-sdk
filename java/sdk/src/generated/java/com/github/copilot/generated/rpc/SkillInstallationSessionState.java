/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Bound-session observation after reconciling persisted enablement.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SkillInstallationSessionState {
    /** The {@code loaded-enabled} variant. */
    LOADED_ENABLED("loaded-enabled"),
    /** The {@code loaded-disabled} variant. */
    LOADED_DISABLED("loaded-disabled"),
    /** The {@code not-loaded} variant. */
    NOT_LOADED("not-loaded"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    SkillInstallationSessionState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SkillInstallationSessionState fromValue(String value) {
        for (SkillInstallationSessionState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SkillInstallationSessionState value: " + value);
    }
}
