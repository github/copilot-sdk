/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why a discoverable candidate cannot be installed
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogNotInstallableReason {
    /** The {@code kind-not-installable} variant. */
    KIND_NOT_INSTALLABLE("kind-not-installable"),
    /** The {@code ai-skill-not-installable} variant. */
    AI_SKILL_NOT_INSTALLABLE("ai-skill-not-installable"),
    /** The {@code policy-forbids} variant. */
    POLICY_FORBIDS("policy-forbids");

    private final String value;
    CatalogNotInstallableReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogNotInstallableReason fromValue(String value) {
        for (CatalogNotInstallableReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogNotInstallableReason value: " + value);
    }
}
