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
 * System notification metadata for a background agent that became idle, including agent ID, type, and description.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemNotificationAgentIdle extends SystemNotification {

    @JsonProperty("type")
    private final String type = "agent_idle";

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

    /** Human-readable description of the agent task */
    @JsonProperty("description")
    private String description;

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
