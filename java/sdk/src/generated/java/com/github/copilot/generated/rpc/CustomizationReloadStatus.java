/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Result of reloading a customization component.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CustomizationReloadStatus {
    /** The {@code reloaded} variant. */
    public static final CustomizationReloadStatus RELOADED = new CustomizationReloadStatus("reloaded");
    /** The {@code skipped} variant. */
    public static final CustomizationReloadStatus SKIPPED = new CustomizationReloadStatus("skipped");
    /** The {@code failed} variant. */
    public static final CustomizationReloadStatus FAILED = new CustomizationReloadStatus("failed");
    /** The default value when no recognized variant is available. */
    public static final CustomizationReloadStatus UNKNOWN = new CustomizationReloadStatus("unknown");

    private final String value;
    private CustomizationReloadStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CustomizationReloadStatus fromValue(String value) {
        if (RELOADED.value.equals(value)) return RELOADED;
        if (SKIPPED.value.equals(value)) return SKIPPED;
        if (FAILED.value.equals(value)) return FAILED;
        if (UNKNOWN.value.equals(value)) return UNKNOWN;
        if (value != null) return new CustomizationReloadStatus(value);
        throw new IllegalArgumentException("Unknown CustomizationReloadStatus value: " + value);
    }
    @Override
    public boolean equals(Object other) {
        return other instanceof CustomizationReloadStatus that && value.equals(that.value);
    }
    @Override
    public int hashCode() { return value.hashCode(); }
}
