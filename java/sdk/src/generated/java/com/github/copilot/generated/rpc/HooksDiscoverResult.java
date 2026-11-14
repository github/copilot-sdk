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
 * Server-discovered hook actions and partial-load diagnostics from user, repository, plugin, and managed-policy sources. Concrete sessions may include additional session-specific hook sources.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HooksDiscoverResult(
    /** All discovered hook actions. Byte-identical actions remain separate rows even when they share a disable key. */
    @JsonProperty("hooks") List<DiscoveredHook> hooks,
    /** Non-fatal source-loading warnings. Discovery remains complete for the affected source, although the source had a recoverable issue. Repository-settings warnings are prefixed with their project path when attribution is available. */
    @JsonProperty("warnings") List<String> warnings,
    /** Errors for hook sources or actions that could not be loaded, making the result partially incomplete. Other valid actions are still returned. Project-resolution and repository-settings errors are prefixed with their project path. */
    @JsonProperty("errors") List<String> errors
) {
}
