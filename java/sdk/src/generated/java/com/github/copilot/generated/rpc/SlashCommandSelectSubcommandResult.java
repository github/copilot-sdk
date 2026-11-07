/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Slash-command invocation result asking the client to present subcommand options for a parent command.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SlashCommandSelectSubcommandResult extends SlashCommandInvocationResult {

    @JsonProperty("kind")
    private final String kind = "select-subcommand";

    @Override
    public String getKind() { return kind; }

    /** Parent command name that requires subcommand selection */
    @JsonProperty("command")
    private String command;

    /** Human-readable title for the selection UI */
    @JsonProperty("title")
    private String title;

    /** Available subcommand options for the client to present */
    @JsonProperty("options")
    private List<SlashCommandSelectSubcommandOption> options;

    /** True when the invocation mutated user runtime settings; consumers caching settings should refresh */
    @JsonProperty("runtimeSettingsChanged")
    private Boolean runtimeSettingsChanged;

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<SlashCommandSelectSubcommandOption> getOptions() { return options; }
    public void setOptions(List<SlashCommandSelectSubcommandOption> options) { this.options = options; }

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
        private String command;
        private String title;
        private List<SlashCommandSelectSubcommandOption> options;
        private Boolean runtimeSettingsChanged;

        /**
         * Sets the {@code command} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder command(String value) {
            this.command = value;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the {@code options} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder options(List<SlashCommandSelectSubcommandOption> value) {
            this.options = value;
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
        public SlashCommandSelectSubcommandResult build() {
            if (command == null) throw new IllegalStateException("Missing required property: command");
            if (title == null) throw new IllegalStateException("Missing required property: title");
            if (options == null) throw new IllegalStateException("Missing required property: options");
            SlashCommandSelectSubcommandResult instance = new SlashCommandSelectSubcommandResult();
            instance.setCommand(command);
            instance.setTitle(title);
            instance.setOptions(options);
            instance.setRuntimeSettingsChanged(runtimeSettingsChanged);
            return instance;
        }
    }
}
