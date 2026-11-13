/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Resource removed by a direct-source operation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SkillRemoveSourceKind {
    /** The {@code directory} variant. */
    DIRECTORY("directory"),
    /** The {@code personal} variant. */
    PERSONAL("personal"),
    /** The {@code project} variant. */
    PROJECT("project");

    private final String value;
    SkillRemoveSourceKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SkillRemoveSourceKind fromValue(String value) {
        for (SkillRemoveSourceKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SkillRemoveSourceKind value: " + value);
    }
}
