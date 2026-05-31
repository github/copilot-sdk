/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Terminal/shell output content block with optional exit code and working directory
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionCompleteContentTerminal extends ToolExecutionCompleteContent {

    @JsonProperty("type")
    private final String type = "terminal";

    @Override
    public String getType() { return type; }

    /** Terminal/shell output text */
    @JsonProperty("text")
    private String text;

    /** Process exit code, if the command has completed */
    @JsonProperty("exitCode")
    private Long exitCode;

    /** Working directory where the command was executed */
    @JsonProperty("cwd")
    private String cwd;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Long getExitCode() { return exitCode; }
    public void setExitCode(Long exitCode) { this.exitCode = exitCode; }

    public String getCwd() { return cwd; }
    public void setCwd(String cwd) { this.cwd = cwd; }
}
