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
 * Variant {@code show-dialog} of {@link SlashCommandInvocationResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SlashCommandInvocationResultShowDialog extends SlashCommandInvocationResult {

    @JsonProperty("kind")
    private final String kind = "show-dialog";

    @Override
    public String getKind() { return kind; }

    /** Dialog the host should display. */
    @JsonProperty("dialog")
    private SlashCommandModelPickerDialog dialog;

    /** Whether command execution changed persisted runtime settings. */
    @JsonProperty("runtimeSettingsChanged")
    private Boolean runtimeSettingsChanged;

    public SlashCommandModelPickerDialog getDialog() { return dialog; }
    public void setDialog(SlashCommandModelPickerDialog dialog) { this.dialog = dialog; }

    public Boolean getRuntimeSettingsChanged() { return runtimeSettingsChanged; }
    public void setRuntimeSettingsChanged(Boolean runtimeSettingsChanged) { this.runtimeSettingsChanged = runtimeSettingsChanged; }
}
