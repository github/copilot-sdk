/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether an MCP server candidate can be planned for installation
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogMcpServerInstallability {
    /** The {@code installable} variant. */
    INSTALLABLE("installable"),
    /** The {@code not-installable-policy} variant. */
    NOT_INSTALLABLE_POLICY("not-installable-policy");

    private final String value;
    CatalogMcpServerInstallability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogMcpServerInstallability fromValue(String value) {
        for (CatalogMcpServerInstallability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogMcpServerInstallability value: " + value);
    }
}
