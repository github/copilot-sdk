/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.rpc.ModelBillingTokenPrices;

/**
 * Model billing information.
 *
 * @since 1.0.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelBilling {

    @JsonProperty("multiplier")
    private Double multiplier;

    @JsonProperty("tokenPrices")
    private ModelBillingTokenPrices tokenPrices;

    public Double getMultiplier() {
        return multiplier;
    }

    public ModelBilling setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
        return this;
    }

    public ModelBillingTokenPrices getTokenPrices() {
        return tokenPrices;
    }

    public ModelBilling setTokenPrices(ModelBillingTokenPrices tokenPrices) {
        this.tokenPrices = tokenPrices;
        return this;
    }
}
