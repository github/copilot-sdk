/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Source location type (e.g., project, personal-copilot, plugin, builtin)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SkillSource {
    /** The {@code project} variant. */
    PROJECT("project"),
    /** The {@code inherited} variant. */
    INHERITED("inherited"),
    /** The {@code personal-copilot} variant. */
    PERSONAL_COPILOT("personal-copilot"),
    /** The {@code personal-agents} variant. */
    PERSONAL_AGENTS("personal-agents"),
    /** The {@code plugin} variant. */
    PLUGIN("plugin"),
    /** The {@code custom} variant. */
    CUSTOM("custom"),
    /** The {@code builtin} variant. */
    BUILTIN("builtin");

    private final String value;
    SkillSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SkillSource fromValue(String value) {
        for (SkillSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SkillSource value: " + value);
    }
}
