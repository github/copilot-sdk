/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Where a required value is applied when the planned server is launched
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpPlanValueCategory {
    /** The {@code environment-variable} variant. */
    ENVIRONMENT_VARIABLE("environment-variable"),
    /** The {@code runtime-argument} variant. */
    RUNTIME_ARGUMENT("runtime-argument"),
    /** The {@code package-argument} variant. */
    PACKAGE_ARGUMENT("package-argument"),
    /** The {@code header} variant. */
    HEADER("header"),
    /** The {@code url-variable} variant. */
    URL_VARIABLE("url-variable");

    private final String value;
    McpPlanValueCategory(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpPlanValueCategory fromValue(String value) {
        for (McpPlanValueCategory v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpPlanValueCategory value: " + value);
    }
}
