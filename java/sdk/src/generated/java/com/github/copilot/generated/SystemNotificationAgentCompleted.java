/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * System notification metadata for a background agent that completed or failed, including agent ID, type, status, description, and prompt.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemNotificationAgentCompleted extends SystemNotification {

    @JsonProperty("type")
    private final String type = "agent_completed";

    @Override
    public String getType() { return type; }

    /** Unique task identifier */
    @JsonProperty("agentId")
    private String agentId;

    /** Friendly, non-unique name intended for display */
    @JsonProperty("displayName")
    private String displayName;

    /** Type of the agent (e.g., explore, task, general-purpose) */
    @JsonProperty("agentType")
    private String agentType;

    /** Whether the agent completed successfully or failed */
    @JsonProperty("status")
    private SystemNotificationAgentCompletedStatus status;

    /** Human-readable description of the agent task */
    @JsonProperty("description")
    private String description;

    /** The full prompt given to the background agent */
    @JsonProperty("prompt")
    private String prompt;

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public SystemNotificationAgentCompletedStatus getStatus() { return status; }
    public void setStatus(SystemNotificationAgentCompletedStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
