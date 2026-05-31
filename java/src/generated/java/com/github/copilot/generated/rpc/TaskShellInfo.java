/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Schema for the `TaskShellInfo` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class TaskShellInfo extends TaskInfo {

    @JsonProperty("type")
    private final String type = "shell";

    @Override
    public String getType() { return type; }

    /** Unique task identifier */
    @JsonProperty("id")
    private String id;

    /** Short description of the task */
    @JsonProperty("description")
    private String description;

    /** Current lifecycle status of the task */
    @JsonProperty("status")
    private TaskStatus status;

    /** ISO 8601 timestamp when the task was started */
    @JsonProperty("startedAt")
    private OffsetDateTime startedAt;

    /** ISO 8601 timestamp when the task finished */
    @JsonProperty("completedAt")
    private OffsetDateTime completedAt;

    /** Command being executed */
    @JsonProperty("command")
    private String command;

    /** Whether the shell runs inside a managed PTY session or as an independent background process */
    @JsonProperty("attachmentMode")
    private TaskShellInfoAttachmentMode attachmentMode;

    /** Whether task execution is synchronously awaited or managed in the background */
    @JsonProperty("executionMode")
    private TaskExecutionMode executionMode;

    /** Whether this shell task can be promoted to background mode */
    @JsonProperty("canPromoteToBackground")
    private Boolean canPromoteToBackground;

    /** Path to the detached shell log, when available */
    @JsonProperty("logPath")
    private String logPath;

    /** Process ID when available */
    @JsonProperty("pid")
    private Long pid;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public TaskShellInfoAttachmentMode getAttachmentMode() { return attachmentMode; }
    public void setAttachmentMode(TaskShellInfoAttachmentMode attachmentMode) { this.attachmentMode = attachmentMode; }

    public TaskExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(TaskExecutionMode executionMode) { this.executionMode = executionMode; }

    public Boolean getCanPromoteToBackground() { return canPromoteToBackground; }
    public void setCanPromoteToBackground(Boolean canPromoteToBackground) { this.canPromoteToBackground = canPromoteToBackground; }

    public String getLogPath() { return logPath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }

    public Long getPid() { return pid; }
    public void setPid(Long pid) { this.pid = pid; }
}
