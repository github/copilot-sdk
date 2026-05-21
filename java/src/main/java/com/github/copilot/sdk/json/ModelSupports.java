/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model support flags.
 *
 * @since 1.0.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelSupports {

    @JsonProperty("vision")
    private boolean vision;

    @JsonProperty("reasoningEffort")
    private boolean reasoningEffort;

    public boolean isVision() {
        return vision;
    }

    public ModelSupports setVision(boolean vision) {
        this.vision = vision;
        return this;
    }

    /**
     * Returns whether this model supports reasoning effort configuration.
     *
     * @return {@code true} if the model supports reasoning effort
     */
    public boolean isReasoningEffort() {
        return reasoningEffort;
    }

    /**
     * Sets whether this model supports reasoning effort configuration.
     *
     * @param reasoningEffort
     *            {@code true} if the model supports reasoning effort
     * @return this instance for method chaining
     */
    public ModelSupports setReasoningEffort(boolean reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
        return this;
    }
}
