/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about an available model.
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
}
