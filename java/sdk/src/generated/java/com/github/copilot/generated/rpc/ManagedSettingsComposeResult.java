/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The effective managed settings the runtime would enforce for the given documents, in the same shape `managedSettings.resolve` returns.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsComposeResult(
    /** Effective managed settings, in the same shape as `session.managedSettings.get`. */
    @JsonProperty("resolved") ManagedSettingsResolvedData resolved,
    /** Typed effective values, as in `managedSettings.resolve`. */
    @JsonProperty("values") ManagedSettingsValues values,
    /** Per-key lock state and provenance for `values`. */
    @JsonProperty("meta") ManagedSettingsMeta meta,
    /** Only the supplied channels, strongest first, with canonical documents. Empty canonical documents are represented as absent settings, as in live resolution. */
    @JsonProperty("layers") List<ManagedSettingsLayer> layers,
    /** Warnings about ignored content, with paths prefixed by the channel name. */
    @JsonProperty("diagnostics") List<ManagedSettingsDiagnostic> diagnostics
) {
}
