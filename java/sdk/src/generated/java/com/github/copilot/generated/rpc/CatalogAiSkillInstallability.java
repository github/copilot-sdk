/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Typed non-installable state for an AI skill candidate
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogAiSkillInstallability {
    /** The {@code not-installable-kind} variant. */
    NOT_INSTALLABLE_KIND("not-installable-kind");

    private final String value;
    CatalogAiSkillInstallability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogAiSkillInstallability fromValue(String value) {
        for (CatalogAiSkillInstallability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogAiSkillInstallability value: " + value);
    }
}
