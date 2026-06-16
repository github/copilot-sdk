/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.rpc.ModelBillingTokenPrices;
import java.util.OptionalDouble;

/**
 * Model billing information.
 *
 * @since 1.0.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelBilling {

    @JsonProperty("multiplier")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double multiplier;

    @JsonProperty("tokenPrices")
    private ModelBillingTokenPrices tokenPrices;

    @JsonIgnore
    public double getMultiplier() {
        return multiplier != null ? multiplier : 0.0;
    }

    public ModelBilling setMultiplier(double multiplier) {
        this.multiplier = multiplier;
        return this;
    }

    /**
     * Returns the billing multiplier as an {@link java.util.OptionalDouble},
     * allowing callers to distinguish "absent" from "zero".
     *
     * @return an {@link java.util.OptionalDouble} containing the multiplier, or
     *         {@link java.util.OptionalDouble#empty()} if not set
     * @since 1.0.2
     */
    @JsonIgnore
    public OptionalDouble getMultiplierOpt() {
        return multiplier == null ? OptionalDouble.empty() : OptionalDouble.of(multiplier);
    }

    public ModelBillingTokenPrices getTokenPrices() {
        return tokenPrices;
    }

    public ModelBilling setTokenPrices(ModelBillingTokenPrices tokenPrices) {
        this.tokenPrices = tokenPrices;
        return this;
    }
}
