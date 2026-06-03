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
 * Schema for the `SystemNotificationAgentIdle` type.
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

    /** Unique identifier of the background agent */
    @JsonProperty("agentId")
    private String agentId;

    /** Type of the agent (e.g., explore, task, general-purpose) */
    @JsonProperty("agentType")
    private String agentType;

    /** Human-readable description of the agent task */
    @JsonProperty("description")
    private String description;

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
