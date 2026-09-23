/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Explicit Agent Plugin compatibility declared by exact catalog tags. Clients must not infer these values from display text or other metadata.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogAgentPluginCompatibilityTag {
    /** The {@code canvas} variant. */
    CANVAS("canvas"),
    /** The {@code canvas-only} variant. */
    CANVAS_ONLY("canvas-only"),
    /** The {@code github-copilot} variant. */
    GITHUB_COPILOT("github-copilot");

    private final String value;
    CatalogAgentPluginCompatibilityTag(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogAgentPluginCompatibilityTag fromValue(String value) {
        for (CatalogAgentPluginCompatibilityTag v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogAgentPluginCompatibilityTag value: " + value);
    }
}
