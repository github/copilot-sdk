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
 * Schema for the `TaskAgentInfo` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class TaskAgentInfo extends TaskInfo {

    @JsonProperty("type")
    private final String type = "agent";

    @Override
    public String getType() { return type; }

    /** Unique task identifier */
    @JsonProperty("id")
    private String id;

    /** Tool call ID associated with this agent task */
    @JsonProperty("toolCallId")
    private String toolCallId;

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

    /** Accumulated active execution time in milliseconds */
    @JsonProperty("activeTimeMs")
    private Long activeTimeMs;

    /** ISO 8601 timestamp when the current active period began */
    @JsonProperty("activeStartedAt")
    private OffsetDateTime activeStartedAt;

    /** Error message when the task failed */
    @JsonProperty("error")
    private String error;

    /** Type of agent running this task */
    @JsonProperty("agentType")
    private String agentType;

    /** Prompt passed to the agent */
    @JsonProperty("prompt")
    private String prompt;

    /** Result text from the task when available */
    @JsonProperty("result")
    private String result;

    /** Model used for the task when specified */
    @JsonProperty("model")
    private String model;

    /** Whether task execution is synchronously awaited or managed in the background */
    @JsonProperty("executionMode")
    private TaskExecutionMode executionMode;

    /** Whether the task is currently in the original sync wait and can be moved to background mode. False once it is already backgrounded, idle, finished, or no longer has a promotable sync waiter. */
    @JsonProperty("canPromoteToBackground")
    private Boolean canPromoteToBackground;

    /** Most recent response text from the agent */
    @JsonProperty("latestResponse")
    private String latestResponse;

    /** ISO 8601 timestamp when the agent entered idle state */
    @JsonProperty("idleSince")
    private OffsetDateTime idleSince;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public Long getActiveTimeMs() { return activeTimeMs; }
    public void setActiveTimeMs(Long activeTimeMs) { this.activeTimeMs = activeTimeMs; }

    public OffsetDateTime getActiveStartedAt() { return activeStartedAt; }
    public void setActiveStartedAt(OffsetDateTime activeStartedAt) { this.activeStartedAt = activeStartedAt; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public TaskExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(TaskExecutionMode executionMode) { this.executionMode = executionMode; }

    public Boolean getCanPromoteToBackground() { return canPromoteToBackground; }
    public void setCanPromoteToBackground(Boolean canPromoteToBackground) { this.canPromoteToBackground = canPromoteToBackground; }

    public String getLatestResponse() { return latestResponse; }
    public void setLatestResponse(String latestResponse) { this.latestResponse = latestResponse; }

    public OffsetDateTime getIdleSince() { return idleSince; }
    public void setIdleSince(OffsetDateTime idleSince) { this.idleSince = idleSince; }
}
