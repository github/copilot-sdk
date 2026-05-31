/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Managed-server child was spawned and registered successfully.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AgentRegistrySpawnSpawned extends AgentRegistrySpawnResult {

    @JsonProperty("kind")
    private final String kind = "spawned";

    @Override
    public String getKind() { return kind; }

    /** Full registry entry for the spawned child. Lets the controller call `handleLiveTargetSelected(entry)` directly without re-reading the registry (avoids a TOCTOU window). */
    @JsonProperty("entry")
    private AgentRegistryLiveTargetEntry entry;

    /** Whether the delegate already sent the initial prompt. Always omitted in the current wiring: the controller sends the prompt post-attach via the standard LocalRpcSession.send path. */
    @JsonProperty("initialPromptSent")
    private Boolean initialPromptSent;

    /** If the delegate attempted to send the initial prompt and failed, the categorized error message. */
    @JsonProperty("initialPromptError")
    private String initialPromptError;

    /** Per-spawn log-capture outcome; populated from spawnLiveTarget. */
    @JsonProperty("logCapture")
    private AgentRegistryLogCapture logCapture;

    public AgentRegistryLiveTargetEntry getEntry() { return entry; }
    public void setEntry(AgentRegistryLiveTargetEntry entry) { this.entry = entry; }

    public Boolean getInitialPromptSent() { return initialPromptSent; }
    public void setInitialPromptSent(Boolean initialPromptSent) { this.initialPromptSent = initialPromptSent; }

    public String getInitialPromptError() { return initialPromptError; }
    public void setInitialPromptError(String initialPromptError) { this.initialPromptError = initialPromptError; }

    public AgentRegistryLogCapture getLogCapture() { return logCapture; }
    public void setLogCapture(AgentRegistryLogCapture logCapture) { this.logCapture = logCapture; }
}
