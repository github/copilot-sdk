/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for a session-start hook.
 * <p>
 * Allows adding additional context or modifying session configuration.
 *
 * @since 1.0.7
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionStartHookOutput {

    @JsonProperty("additionalContext")
    private String additionalContext;

    @JsonProperty("modifiedConfig")
    private Map<String, Object> modifiedConfig;

    /**
     * Gets the additional context to add.
     *
     * @return the additional context, or {@code null}
     */
    public String getAdditionalContext() {
        return additionalContext;
    }

    /**
     * Sets additional context to be added to the session.
     *
     * @param additionalContext
     *            the additional context
     * @return this instance for method chaining
     */
    public SessionStartHookOutput setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
        return this;
    }

    /**
     * Gets the modified configuration.
     *
     * @return the modified configuration map, or {@code null}
     */
    public Map<String, Object> getModifiedConfig() {
        return modifiedConfig;
    }

    /**
     * Sets modified configuration options for the session.
     *
     * @param modifiedConfig
     *            the modified configuration
     * @return this instance for method chaining
     */
    public SessionStartHookOutput setModifiedConfig(Map<String, Object> modifiedConfig) {
        this.modifiedConfig = modifiedConfig;
        return this;
    }
}
