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
import javax.annotation.processing.Generated;

/**
 * Per-key lock state and provenance for `ManagedSettingsValues`, with the same field names. Producers emit each typed key in values and meta together; both outer objects are omitted when no typed key is set.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsMeta(
    /** Lock state and provenance of `values.model`. */
    @JsonProperty("model") ManagedSettingMeta model,
    /** Lock state and provenance of `values.autoTier`. */
    @JsonProperty("autoTier") ManagedSettingMeta autoTier,
    /** Lock state and provenance of values.fusionTier, including a legacy alias's original source. An unmanaged value imposes no tier lock. */
    @JsonProperty("fusionTier") ManagedSettingMeta fusionTier,
    /** Lock state and provenance of `values.effortLevel`. */
    @JsonProperty("effortLevel") ManagedSettingMeta effortLevel,
    /** Lock state and provenance of `values.contextTier`. */
    @JsonProperty("contextTier") ManagedSettingMeta contextTier
) {
}
