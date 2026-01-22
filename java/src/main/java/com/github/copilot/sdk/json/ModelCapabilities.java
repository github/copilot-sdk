/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model capabilities and limits.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelCapabilities {

    @JsonProperty("supports")
    private ModelSupports supports;

    @JsonProperty("limits")
    private ModelLimits limits;

    public ModelSupports getSupports() {
        return supports;
    }

    public ModelCapabilities setSupports(ModelSupports supports) {
        this.supports = supports;
        return this;
    }

    public ModelLimits getLimits() {
        return limits;
    }

    public ModelCapabilities setLimits(ModelLimits limits) {
        this.limits = limits;
        return this;
    }
}
