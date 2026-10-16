/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why a feed entry cannot be presented
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogFeedUnavailableReason {
    /** The {@code not-found} variant. */
    NOT_FOUND("not-found"),
    /** The {@code unsupported-kind} variant. */
    UNSUPPORTED_KIND("unsupported-kind"),
    /** The {@code invalid-resource} variant. */
    INVALID_RESOURCE("invalid-resource");

    private final String value;
    CatalogFeedUnavailableReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogFeedUnavailableReason fromValue(String value) {
        for (CatalogFeedUnavailableReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogFeedUnavailableReason value: " + value);
    }
}
