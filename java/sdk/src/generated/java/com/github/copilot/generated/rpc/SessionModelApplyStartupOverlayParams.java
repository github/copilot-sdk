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
 * Managed, repository, and CLI model overrides to overlay onto the session at startup.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelApplyStartupOverlayParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Model required by device-managed policy, when configured. */
    @JsonProperty("deviceManagedModel") String deviceManagedModel,
    /** Model required by server-managed policy, when configured. */
    @JsonProperty("serverManagedModel") String serverManagedModel,
    /** Model selected by repository settings, when configured. */
    @JsonProperty("repoModel") String repoModel,
    /** Reasoning effort selected by repository settings, when configured. */
    @JsonProperty("repoReasoningEffort") String repoReasoningEffort,
    /** Context tier selected by repository settings, when configured. */
    @JsonProperty("repoContextTier") String repoContextTier,
    /** Model explicitly selected by the CLI, when provided. */
    @JsonProperty("cliModel") String cliModel,
    /** Whether the overlay is being applied while resuming a deferred session. */
    @JsonProperty("deferredResume") Boolean deferredResume
) {
}
