/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model limits.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelLimits {

    @JsonProperty("max_prompt_tokens")
    private Integer maxPromptTokens;

    @JsonProperty("max_context_window_tokens")
    private int maxContextWindowTokens;

    @JsonProperty("vision")
    private ModelVisionLimits vision;

    public Integer getMaxPromptTokens() {
        return maxPromptTokens;
    }

    public ModelLimits setMaxPromptTokens(Integer maxPromptTokens) {
        this.maxPromptTokens = maxPromptTokens;
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
