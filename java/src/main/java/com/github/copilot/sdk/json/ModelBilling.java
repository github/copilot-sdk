/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model billing information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelBilling {

    @JsonProperty("multiplier")
    private double multiplier;

    public double getMultiplier() {
        return multiplier;
    }

    public ModelBilling setMultiplier(double multiplier) {
        this.multiplier = multiplier;
        return this;
    }
}
