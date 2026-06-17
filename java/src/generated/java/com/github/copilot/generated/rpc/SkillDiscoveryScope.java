/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Which tier this directory belongs to
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SkillDiscoveryScope {
    /** The {@code project} variant. */
    PROJECT("project"),
    /** The {@code personal-copilot} variant. */
    PERSONAL_COPILOT("personal-copilot"),
    /** The {@code personal-agents} variant. */
    PERSONAL_AGENTS("personal-agents"),
    /** The {@code custom} variant. */
    CUSTOM("custom");

    private final String value;
    SkillDiscoveryScope(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SkillDiscoveryScope fromValue(String value) {
        for (SkillDiscoveryScope v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SkillDiscoveryScope value: " + value);
    }
}
