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
 * One model provider available to the session — the model analog of the account `ProviderDescriptor`. Opaque id/label/kind plus a stable ordering; central code never branches on kind.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderDescriptor(
    /** The neutral provider kind. */
    @JsonProperty("kind") ModelProviderKind kind,
    /** Opaque, stable provider id, stamped onto every model this provider returns. */
    @JsonProperty("id") String id,
    /** Human-readable menu label, owned by the runtime so every consumer renders identical text. */
    @JsonProperty("label") String label,
    /** Stable ordering key for presenting providers in a deterministic sequence. */
    @JsonProperty("ordering") Long ordering
) {
}
