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
 * Schema for the `SystemNotificationShellCompleted` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemNotificationShellCompleted extends SystemNotification {

    @JsonProperty("type")
    private final String type = "shell_completed";

    @Override
    public String getType() { return type; }

    /** Unique identifier of the shell session */
    @JsonProperty("shellId")
    private String shellId;

    /** Exit code of the shell command, if available */
    @JsonProperty("exitCode")
    private Long exitCode;

    /** Human-readable description of the command */
    @JsonProperty("description")
    private String description;

    public String getShellId() { return shellId; }
    public void setShellId(String shellId) { this.shellId = shellId; }

    public Long getExitCode() { return exitCode; }
    public void setExitCode(Long exitCode) { this.exitCode = exitCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
