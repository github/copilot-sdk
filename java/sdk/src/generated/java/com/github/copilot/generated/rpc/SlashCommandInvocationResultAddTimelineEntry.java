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
        private SlashCommandTimelineEntry entry;
        private String prefillInput;
        private Boolean runtimeSettingsChanged;

        /**
         * Sets the {@code entry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder entry(SlashCommandTimelineEntry value) {
            this.entry = value;
            return this;
        }

        /**
         * Sets the {@code prefillInput} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prefillInput(String value) {
            this.prefillInput = value;
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
        public SlashCommandInvocationResultAddTimelineEntry build() {
            if (entry == null) throw new IllegalStateException("Missing required property: entry");
            SlashCommandInvocationResultAddTimelineEntry instance = new SlashCommandInvocationResultAddTimelineEntry();
            instance.setEntry(entry);
            instance.setPrefillInput(prefillInput);
            instance.setRuntimeSettingsChanged(runtimeSettingsChanged);
            return instance;
        }
    }
}
