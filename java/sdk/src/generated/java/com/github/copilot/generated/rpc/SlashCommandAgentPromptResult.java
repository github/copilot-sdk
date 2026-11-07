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
 * Slash-command invocation result that submits an agent prompt, with display prompt, optional mode, optional user-facing notice, settings-change flag, and opaque reservation finalization token.
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

    /** Optional user-facing notice to show before the prompt is submitted */
    @JsonProperty("notice")
    private String notice;

    /** True when the invocation mutated user runtime settings; consumers caching settings should refresh */
    @JsonProperty("runtimeSettingsChanged")
    private Boolean runtimeSettingsChanged;

    /** Opaque token used to finalize this invocation's objective prompt reservation */
    @JsonProperty("reservationToken")
    private String reservationToken;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getDisplayPrompt() { return displayPrompt; }
    public void setDisplayPrompt(String displayPrompt) { this.displayPrompt = displayPrompt; }

    public SessionMode getMode() { return mode; }
    public void setMode(SessionMode mode) { this.mode = mode; }

    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }

    public Boolean getRuntimeSettingsChanged() { return runtimeSettingsChanged; }
    public void setRuntimeSettingsChanged(Boolean runtimeSettingsChanged) { this.runtimeSettingsChanged = runtimeSettingsChanged; }

    public String getReservationToken() { return reservationToken; }
    public void setReservationToken(String reservationToken) { this.reservationToken = reservationToken; }


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
        private String prompt;
        private String displayPrompt;
        private SessionMode mode;
        private String notice;
        private Boolean runtimeSettingsChanged;
        private String reservationToken;

        /**
         * Sets the {@code prompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prompt(String value) {
            this.prompt = value;
            return this;
        }

        /**
         * Sets the {@code displayPrompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayPrompt(String value) {
            this.displayPrompt = value;
            return this;
        }

        /**
         * Sets the {@code mode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mode(SessionMode value) {
            this.mode = value;
            return this;
        }

        /**
         * Sets the {@code notice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder notice(String value) {
            this.notice = value;
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
         * Sets the {@code reservationToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reservationToken(String value) {
            this.reservationToken = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SlashCommandAgentPromptResult build() {
            if (prompt == null) throw new IllegalStateException("Missing required property: prompt");
            if (displayPrompt == null) throw new IllegalStateException("Missing required property: displayPrompt");
            SlashCommandAgentPromptResult instance = new SlashCommandAgentPromptResult();
            instance.setPrompt(prompt);
            instance.setDisplayPrompt(displayPrompt);
            instance.setMode(mode);
            instance.setNotice(notice);
            instance.setRuntimeSettingsChanged(runtimeSettingsChanged);
            instance.setReservationToken(reservationToken);
            return instance;
        }
    }
}
