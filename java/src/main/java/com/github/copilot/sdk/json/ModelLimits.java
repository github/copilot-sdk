/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.OptionalInt;

/**
 * Model limits.
 *
 * @since 1.0.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelLimits {

    @JsonProperty("max_prompt_tokens")
    private Integer maxPromptTokens;

    @JsonProperty("max_context_window_tokens")
    private int maxContextWindowTokens;

    @JsonProperty("vision")
    private ModelVisionLimits vision;

    @JsonIgnore
    public OptionalInt getMaxPromptTokens() {
        return maxPromptTokens == null ? OptionalInt.empty() : OptionalInt.of(maxPromptTokens);
    }

    public ModelLimits setMaxPromptTokens(int maxPromptTokens) {
        this.maxPromptTokens = maxPromptTokens;
        return this;
    }

    /**
     * Clears the maxPromptTokens setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public ModelLimits clearMaxPromptTokens() {
        this.maxPromptTokens = null;
        return this;
    }

    public int getMaxContextWindowTokens() {
        return maxContextWindowTokens;
    }

    public ModelLimits setMaxContextWindowTokens(int maxContextWindowTokens) {
        this.maxContextWindowTokens = maxContextWindowTokens;
        return this;
    }

    public ModelVisionLimits getVision() {
        return vision;
    }

    public ModelLimits setVision(ModelVisionLimits vision) {
        this.vision = vision;
        return this;
    }
}
