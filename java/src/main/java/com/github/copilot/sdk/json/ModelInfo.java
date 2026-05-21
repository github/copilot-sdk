/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about an available model.
 *
 * @since 1.0.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelInfo {

    /**
     * Model identifier (e.g., "claude-sonnet-4.5").
     */
    @JsonProperty("id")
    private String id;

    /**
     * Display name.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Model capabilities and limits.
     */
    @JsonProperty("capabilities")
    private ModelCapabilities capabilities;

    /**
     * Policy state.
     */
    @JsonProperty("policy")
    private ModelPolicy policy;

    /**
     * Billing information.
     */
    @JsonProperty("billing")
    private ModelBilling billing;

    /**
     * Supported reasoning effort levels (only present if model supports reasoning
     * effort).
     */
    @JsonProperty("supportedReasoningEfforts")
    private List<String> supportedReasoningEfforts;

    /**
     * Default reasoning effort level (only present if model supports reasoning
     * effort).
     */
    @JsonProperty("defaultReasoningEffort")
    private String defaultReasoningEffort;

    public String getId() {
        return id;
    }

    public ModelInfo setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public ModelInfo setName(String name) {
        this.name = name;
        return this;
    }

    public ModelCapabilities getCapabilities() {
        return capabilities;
    }

    public ModelInfo setCapabilities(ModelCapabilities capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public ModelPolicy getPolicy() {
        return policy;
    }

    public ModelInfo setPolicy(ModelPolicy policy) {
        this.policy = policy;
        return this;
    }

    public ModelBilling getBilling() {
        return billing;
    }

    public ModelInfo setBilling(ModelBilling billing) {
        this.billing = billing;
        return this;
    }

    /**
     * Gets the supported reasoning effort levels.
     *
     * @return the list of supported reasoning effort levels, or {@code null} if the
     *         model doesn't support reasoning effort
     */
    public List<String> getSupportedReasoningEfforts() {
        return supportedReasoningEfforts;
    }

    /**
     * Sets the supported reasoning effort levels.
     *
     * @param supportedReasoningEfforts
     *            the list of supported reasoning effort levels
     * @return this instance for method chaining
     */
    public ModelInfo setSupportedReasoningEfforts(List<String> supportedReasoningEfforts) {
        this.supportedReasoningEfforts = supportedReasoningEfforts;
        return this;
    }

    /**
     * Gets the default reasoning effort level.
     *
     * @return the default reasoning effort level, or {@code null} if the model
     *         doesn't support reasoning effort
     */
    public String getDefaultReasoningEffort() {
        return defaultReasoningEffort;
    }

    /**
     * Sets the default reasoning effort level.
     *
     * @param defaultReasoningEffort
     *            the default reasoning effort level
     * @return this instance for method chaining
     */
    public ModelInfo setDefaultReasoningEffort(String defaultReasoningEffort) {
        this.defaultReasoningEffort = defaultReasoningEffort;
        return this;
    }
}
