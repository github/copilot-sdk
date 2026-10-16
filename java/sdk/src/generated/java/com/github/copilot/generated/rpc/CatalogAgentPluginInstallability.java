/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Typed installability state for an Agent Plugin candidate
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogAgentPluginInstallability {
    /** The {@code installable} variant. */
    INSTALLABLE("installable"),
    /** The {@code install-data-unavailable} variant. */
    INSTALL_DATA_UNAVAILABLE("install-data-unavailable"),
    /** The {@code feature-disabled} variant. */
    FEATURE_DISABLED("feature-disabled"),
    /** The {@code not-installable-kind} variant. */
    NOT_INSTALLABLE_KIND("not-installable-kind");

    private final String value;
    CatalogAgentPluginInstallability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogAgentPluginInstallability fromValue(String value) {
        for (CatalogAgentPluginInstallability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogAgentPluginInstallability value: " + value);
    }
}
