/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a custom agent available for selection in a session.
 *
 * @since 1.0.11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentInfo {

    @JsonProperty("name")
    private String name;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("description")
    private String description;

    /**
     * Gets the unique identifier of the agent.
     *
     * @return the agent name/identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique identifier of the agent.
     *
     * @param name
     *            the agent name/identifier
     * @return this instance for chaining
     */
    public AgentInfo setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the human-readable display name of the agent.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the human-readable display name of the agent.
     *
     * @param displayName
     *            the display name
     * @return this instance for chaining
     */
    public AgentInfo setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Gets the description of the agent's purpose.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the agent's purpose.
     *
     * @param description
     *            the description
     * @return this instance for chaining
     */
    public AgentInfo setDescription(String description) {
        this.description = description;
        return this;
    }
}
