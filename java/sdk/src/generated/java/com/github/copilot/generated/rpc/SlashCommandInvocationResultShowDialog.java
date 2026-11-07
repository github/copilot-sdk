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


    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private SlashCommandModelPickerDialog dialog;
        private Boolean runtimeSettingsChanged;

        /**
         * Sets the {@code dialog} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dialog(SlashCommandModelPickerDialog value) {
            this.dialog = value;
            return this;
        }

        /**
         * Sets the {@code runtimeSettingsChanged} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder runtimeSettingsChanged(Boolean value) {
            this.runtimeSettingsChanged = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SlashCommandInvocationResultShowDialog build() {
            if (dialog == null) throw new IllegalStateException("Missing required property: dialog");
            SlashCommandInvocationResultShowDialog instance = new SlashCommandInvocationResultShowDialog();
            instance.setDialog(dialog);
            instance.setRuntimeSettingsChanged(runtimeSettingsChanged);
            return instance;
        }
    }
}
