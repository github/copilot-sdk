/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Canonical Agent Plugin media type
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogAgentPluginMediaType {
    /** The {@code application/vnd.github.copilot-plugin} variant. */
    APPLICATION_VND_GITHUB_COPILOT_PLUGIN("application/vnd.github.copilot-plugin");

    private final String value;
    CatalogAgentPluginMediaType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogAgentPluginMediaType fromValue(String value) {
        for (CatalogAgentPluginMediaType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogAgentPluginMediaType value: " + value);
    }
}
