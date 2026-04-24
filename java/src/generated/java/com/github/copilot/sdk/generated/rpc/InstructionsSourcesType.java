/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Category of instruction source — used for merge logic
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum InstructionsSourcesType {
    /** The {@code home} variant. */
    HOME("home"),
    /** The {@code repo} variant. */
    REPO("repo"),
    /** The {@code model} variant. */
    MODEL("model"),
    /** The {@code vscode} variant. */
    VSCODE("vscode"),
    /** The {@code nested-agents} variant. */
    NESTED_AGENTS("nested-agents"),
    /** The {@code child-instructions} variant. */
    CHILD_INSTRUCTIONS("child-instructions");

    private final String value;
    InstructionsSourcesType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static InstructionsSourcesType fromValue(String value) {
        for (InstructionsSourcesType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown InstructionsSourcesType value: " + value);
    }
}
