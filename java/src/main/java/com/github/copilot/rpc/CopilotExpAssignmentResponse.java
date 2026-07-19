/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * ExP ("flight") assignment data, in the same JSON shape the Copilot CLI
 * fetches from the experimentation service.
 * <p>
 * Property names serialize as PascalCase ({@code Features}, {@code Flights},
 * {@code Configs}, ...) to match the on-the-wire contract consumed by the
 * runtime. This is an internal/trusted-integrator option, not part of the
 * broadly advertised public surface.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CopilotExpAssignmentResponse {

    @JsonProperty("Features")
    private List<String> features = new ArrayList<>();

    @JsonProperty("Flights")
    private Map<String, String> flights = new LinkedHashMap<>();

    @JsonProperty("Configs")
    private List<ExpConfigEntry> configs = new ArrayList<>();

    @JsonProperty("ParameterGroups")
    private JsonNode parameterGroups;

    @JsonProperty("FlightingVersion")
    private Integer flightingVersion;

    @JsonProperty("ImpressionId")
    private String impressionId;

    @JsonProperty("AssignmentContext")
    private String assignmentContext = "";

    /**
     * Gets the enabled feature names.
     *
     * @return the feature list
     */
    public List<String> getFeatures() {
        return features;
    }

    /**
     * Sets the enabled feature names.
     *
     * @param features
     *            the feature list
     * @return this instance for method chaining
     */
    public CopilotExpAssignmentResponse setFeatures(List<String> features) {
        this.features = features;
        return this;
    }

    /**
     * Gets the assigned flights keyed by flight name.
     *
     * @return the flights map
     */
    public Map<String, String> getFlights() {
        return flights;
    }

    /**
     * Sets the assigned flights keyed by flight name.
     *
     * @param flights
     *            the flights map
     * @return this instance for method chaining
     */
    public CopilotExpAssignmentResponse setFlights(Map<String, String> flights) {
        this.flights = flights;
        return this;
    }

    /**
     * Gets the configuration entries carrying typed parameter values.
     *
     * @return the configuration entries
     */
    public List<ExpConfigEntry> getConfigs() {
        return configs;
    }

    /**
     * Sets the configuration entries carrying typed parameter values.
     *
     * @param configs
     *            the configuration entries
     * @return this instance for method chaining
     */
    public CopilotExpAssignmentResponse setConfigs(List<ExpConfigEntry> configs) {
        this.configs = configs;
        return this;
    }

    /**
     * Gets the opaque parameter-group payload passed through untouched.
     *
     * @return the parameter groups, or {@code null} if not set
     */
    public JsonNode getParameterGroups() {
        return parameterGroups;
    }

    /**
     * Sets the opaque parameter-group payload passed through untouched.
     *
     * @param parameterGroups
     *            the parameter groups
     * @return this instance for method chaining
     */
    public CopilotExpAssignmentResponse setParameterGroups(JsonNode parameterGroups) {
        this.parameterGroups = parameterGroups;
        return this;
    }

    /**
     * Gets the version of the flighting configuration.
     *
     * @return the flighting version, or {@code null} if not set
     */
    public Integer getFlightingVersion() {
        return flightingVersion;
    }

    /**
     * Sets the version of the flighting configuration.
     *
     * @param flightingVersion
     *            the flighting version
     * @return this instance for method chaining
     */
    public CopilotExpAssignmentResponse setFlightingVersion(Integer flightingVersion) {
        this.flightingVersion = flightingVersion;
        return this;
    }

    /**
     * Gets the impression identifier for the assignment.
     *
     * @return the impression identifier, or {@code null} if not set
     */
    public String getImpressionId() {
        return impressionId;
    }

    /**
     * Sets the impression identifier for the assignment.
     *
     * @param impressionId
     *            the impression identifier
     * @return this instance for method chaining
     */
    public CopilotExpAssignmentResponse setImpressionId(String impressionId) {
        this.impressionId = impressionId;
        return this;
    }

    /**
     * Gets the assignment context string forwarded to CAPI and telemetry.
     *
     * @return the assignment context (empty string when unset)
     */
    public String getAssignmentContext() {
        return assignmentContext;
    }

    /**
     * Sets the assignment context string forwarded to CAPI and telemetry.
     *
     * @param assignmentContext
     *            the assignment context
     * @return this instance for method chaining
     */
    public CopilotExpAssignmentResponse setAssignmentContext(String assignmentContext) {
        this.assignmentContext = assignmentContext;
        return this;
    }
}
