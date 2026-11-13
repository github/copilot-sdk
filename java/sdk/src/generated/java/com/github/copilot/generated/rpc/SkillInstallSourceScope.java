/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Destination scope for a directly installed skill file.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SkillInstallSourceScope {
    /** The {@code personal} variant. */
    PERSONAL("personal"),
    /** The {@code project} variant. */
    PROJECT("project");

    private final String value;
    SkillInstallSourceScope(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SkillInstallSourceScope fromValue(String value) {
        for (SkillInstallSourceScope v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SkillInstallSourceScope value: " + value);
    }
}
