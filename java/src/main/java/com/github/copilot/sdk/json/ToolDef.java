/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Low-level tool definition for JSON-RPC communication.
 * <p>
 * This is an internal class representing the wire format of a tool. For
 * registering tools with the SDK, use {@link ToolDefinition} instead.
 *
 * @see ToolDefinition
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ToolDef {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private Object parameters;

    /**
     * Creates an empty tool definition.
     */
    public ToolDef() {
    }

    /**
     * Creates a tool definition with all fields.
     *
     * @param name
     *            the unique tool identifier
     * @param description
     *            the tool description
     * @param parameters
     *            the JSON Schema for tool parameters
     */
    public ToolDef(String name, String description, Object parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    /**
     * Gets the tool name.
     *
     * @return the tool name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the tool name.
     *
     * @param name
     *            the tool name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the tool description.
     *
     * @return the tool description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the tool description.
     *
     * @param description
     *            the tool description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the JSON Schema for tool parameters.
     *
     * @return the parameters schema
     */
    public Object getParameters() {
        return parameters;
    }

    /**
     * Sets the JSON Schema for tool parameters.
     *
     * @param parameters
     *            the parameters schema
     */
    public void setParameters(Object parameters) {
        this.parameters = parameters;
    }
}
