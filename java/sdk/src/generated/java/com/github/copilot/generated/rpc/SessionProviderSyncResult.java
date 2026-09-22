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
 * The selectable model entries and selection ids synthesized for the synchronized BYOK models.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProviderSyncResult(
    /** Synthesized selectable model entries for the synchronized BYOK models. */
    @JsonProperty("models") List<Object> models,
    /** Provider-qualified model selection ids present after synchronization. */
    @JsonProperty("selectionIds") List<String> selectionIds,
    /** True when synchronization withdrew the selected host-managed model, leaving the session with no explicit selection, so ordinary model resolution picks the session default. Synchronization never promotes a surviving host model in its place: publishing a model offers it, and the choice of which model to use stays with the user. */
    @JsonProperty("modelDeselected") Boolean modelDeselected
) {
}
