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
 * Schema for the `SlashCommandTextResult` type.
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

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Boolean getMarkdown() { return markdown; }
    public void setMarkdown(Boolean markdown) { this.markdown = markdown; }

    public Boolean getPreserveAnsi() { return preserveAnsi; }
    public void setPreserveAnsi(Boolean preserveAnsi) { this.preserveAnsi = preserveAnsi; }

    public Boolean getRuntimeSettingsChanged() { return runtimeSettingsChanged; }
    public void setRuntimeSettingsChanged(Boolean runtimeSettingsChanged) { this.runtimeSettingsChanged = runtimeSettingsChanged; }
}
