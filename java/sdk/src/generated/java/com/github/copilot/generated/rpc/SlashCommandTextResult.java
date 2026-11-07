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
 * Slash-command invocation result containing text output plus Markdown/ANSI rendering flags.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SlashCommandTextResult extends SlashCommandInvocationResult {

    @JsonProperty("kind")
    private final String kind = "text";

    @Override
    public String getKind() { return kind; }

    /** Text output for the client to render */
    @JsonProperty("text")
    private String text;

    /** Whether text contains Markdown */
    @JsonProperty("markdown")
    private Boolean markdown;

    /** Whether ANSI sequences should be preserved */
    @JsonProperty("preserveAnsi")
    private Boolean preserveAnsi;

    /** True when the invocation mutated user runtime settings; consumers caching settings should refresh */
    @JsonProperty("runtimeSettingsChanged")
    private Boolean runtimeSettingsChanged;

    /** Present when the invocation changed the sandbox for this session only. Nothing was persisted, so consumers must mirror the change onto the live session rather than reloading settings, and must not treat it as a settings change. */
    @JsonProperty("sandboxSessionChange")
    private SandboxSessionChange sandboxSessionChange;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Boolean getMarkdown() { return markdown; }
    public void setMarkdown(Boolean markdown) { this.markdown = markdown; }

    public Boolean getPreserveAnsi() { return preserveAnsi; }
    public void setPreserveAnsi(Boolean preserveAnsi) { this.preserveAnsi = preserveAnsi; }

    public Boolean getRuntimeSettingsChanged() { return runtimeSettingsChanged; }
    public void setRuntimeSettingsChanged(Boolean runtimeSettingsChanged) { this.runtimeSettingsChanged = runtimeSettingsChanged; }

    public SandboxSessionChange getSandboxSessionChange() { return sandboxSessionChange; }
    public void setSandboxSessionChange(SandboxSessionChange sandboxSessionChange) { this.sandboxSessionChange = sandboxSessionChange; }


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
        private String text;
        private Boolean markdown;
        private Boolean preserveAnsi;
        private Boolean runtimeSettingsChanged;
        private SandboxSessionChange sandboxSessionChange;

        /**
         * Sets the {@code text} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder text(String value) {
            this.text = value;
            return this;
        }

        /**
         * Sets the {@code markdown} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder markdown(Boolean value) {
            this.markdown = value;
            return this;
        }

        /**
         * Sets the {@code preserveAnsi} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder preserveAnsi(Boolean value) {
            this.preserveAnsi = value;
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
         * Sets the {@code sandboxSessionChange} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxSessionChange(SandboxSessionChange value) {
            this.sandboxSessionChange = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SlashCommandTextResult build() {
            if (text == null) throw new IllegalStateException("Missing required property: text");
            SlashCommandTextResult instance = new SlashCommandTextResult();
            instance.setText(text);
            instance.setMarkdown(markdown);
            instance.setPreserveAnsi(preserveAnsi);
            instance.setRuntimeSettingsChanged(runtimeSettingsChanged);
            instance.setSandboxSessionChange(sandboxSessionChange);
            return instance;
        }
    }
}
