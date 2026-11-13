/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Source acquisition used by a direct skill installation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SkillInstallSourceKind {
    /** The {@code url} variant. */
    URL("url"),
    /** The {@code file} variant. */
    FILE("file"),
    /** The {@code directory} variant. */
    DIRECTORY("directory");

    private final String value;
    SkillInstallSourceKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SkillInstallSourceKind fromValue(String value) {
        for (SkillInstallSourceKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SkillInstallSourceKind value: " + value);
    }
}
