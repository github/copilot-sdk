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
 * Variant {@code add-timeline-entry} of {@link SlashCommandInvocationResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SlashCommandInvocationResultAddTimelineEntry extends SlashCommandInvocationResult {

    @JsonProperty("kind")
    private final String kind = "add-timeline-entry";

    @Override
    public String getKind() { return kind; }

    /** Timeline entry the host should append. */
    @JsonProperty("entry")
    private SlashCommandTimelineEntry entry;

    /** Optional text the host should prefill into the input editor. */
    @JsonProperty("prefillInput")
    private String prefillInput;

    /** Whether command execution changed persisted runtime settings. */
    @JsonProperty("runtimeSettingsChanged")
    private Boolean runtimeSettingsChanged;

    public SlashCommandTimelineEntry getEntry() { return entry; }
    public void setEntry(SlashCommandTimelineEntry entry) { this.entry = entry; }

    public String getPrefillInput() { return prefillInput; }
    public void setPrefillInput(String prefillInput) { this.prefillInput = prefillInput; }

    public Boolean getRuntimeSettingsChanged() { return runtimeSettingsChanged; }
    public void setRuntimeSettingsChanged(Boolean runtimeSettingsChanged) { this.runtimeSettingsChanged = runtimeSettingsChanged; }
}
