/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;

/**
 * <strong>Experimental.</strong> This API may change or be removed in a future
 * release. Opt in to image generation through an authorized Copilot image
 * model. Omission disables on create/cold resume and preserves resident resume
 * state. Not persisted: re-supply after runtime restart. Policy, permissions,
 * offline mode, and tool filters still apply. BYOK image generation is not
 * supported.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@CopilotExperimental
public class ImageGenerationConfig {
    @JsonProperty("enabled")
    private Boolean enabled;

    /**
     * Gets the image generation opt-in.
     *
     * @return true to opt in, false to disable, or null when unset
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Enables or disables image generation.
     *
     * @param enabled
     *            the opt-in, or null when unset
     * @return this config for method chaining
     */
    public ImageGenerationConfig setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
