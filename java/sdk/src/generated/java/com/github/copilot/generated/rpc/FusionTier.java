/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Copilot Fusion tier, matching the CLI's `--fusion-tier`. The runtime resolves each tier to the Auto routing preference it stands for, using the same runtime-owned bindings as the CLI; reads report that preference as `autoTier`, and `session.model.getCurrent` and Auto switch results also report the resolved tier once the session's Auto tier metadata is known.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FusionTier {
    /** The {@code standard} variant. */
    STANDARD("standard"),
    /** The {@code high} variant. */
    HIGH("high"),
    /** The {@code max} variant. */
    MAX("max");

    private final String value;
    FusionTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FusionTier fromValue(String value) {
        for (FusionTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FusionTier value: " + value);
    }
}
