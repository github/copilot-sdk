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
 * Schema for the `SlashCommandSelectSubcommandResult` type.
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
}
