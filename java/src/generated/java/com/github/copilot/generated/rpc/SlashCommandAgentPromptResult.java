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
 * Schema for the `SlashCommandAgentPromptResult` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SlashCommandAgentPromptResult extends SlashCommandInvocationResult {

    @JsonProperty("kind")
    private final String kind = "agent-prompt";

    @Override
    public String getKind() { return kind; }

    /** Prompt to submit to the agent */
    @JsonProperty("prompt")
    private String prompt;

    /** Prompt text to display to the user */
    @JsonProperty("displayPrompt")
    private String displayPrompt;

    /** Optional target session mode for the agent prompt */
    @JsonProperty("mode")
    private SessionMode mode;

    /** True when the invocation mutated user runtime settings; consumers caching settings should refresh */
    @JsonProperty("runtimeSettingsChanged")
    private Boolean runtimeSettingsChanged;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getDisplayPrompt() { return displayPrompt; }
    public void setDisplayPrompt(String displayPrompt) { this.displayPrompt = displayPrompt; }

    public SessionMode getMode() { return mode; }
    public void setMode(SessionMode mode) { this.mode = mode; }

    public Boolean getRuntimeSettingsChanged() { return runtimeSettingsChanged; }
    public void setRuntimeSettingsChanged(Boolean runtimeSettingsChanged) { this.runtimeSettingsChanged = runtimeSettingsChanged; }
}
