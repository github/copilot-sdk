/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single configuration entry within a {@link CopilotExpAssignmentResponse}.
 * <p>
 * Each entry carries an identifier and a bag of typed parameter values, where
 * each value is a string, number, boolean, or {@code null}. Property names
 * serialize as PascalCase to match the experimentation-service wire contract.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpConfigEntry {

    @JsonProperty("Id")
    private String id = "";

    @JsonProperty("Parameters")
    private Map<String, Object> parameters = new LinkedHashMap<>();

    /**
     * Gets the identifier of this configuration entry.
     *
     * @return the entry identifier (empty string when unset)
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier of this configuration entry.
     *
     * @param id
     *            the entry identifier
     * @return this instance for method chaining
     */
    public ExpConfigEntry setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Gets the parameter values keyed by parameter name. Each value is a string,
     * number, boolean, or {@code null}.
     *
     * @return the parameter map
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameter values keyed by parameter name. Each value is a string,
     * number, boolean, or {@code null}.
     *
     * @param parameters
     *            the parameter map
     * @return this instance for method chaining
     */
    public ExpConfigEntry setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }
}
