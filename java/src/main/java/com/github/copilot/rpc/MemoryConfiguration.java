/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for session memory.
 * <p>
 * Controls whether the session can read and write persistent memory.
 *
 * @since 1.6.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemoryConfiguration {

    @JsonProperty("enabled")
    private boolean enabled;

    /**
     * Gets whether memory is enabled for the session.
     *
     * @return {@code true} if memory is enabled, {@code false} otherwise
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether memory is enabled for the session.
     *
     * @param enabled
     *            {@code true} to enable memory, {@code false} to disable
     * @return this config for method chaining
     */
    public MemoryConfiguration setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
