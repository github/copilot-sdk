/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotExperimental;
import com.github.copilot.generated.rpc.MemoryMode;

/**
 * Configuration for session memory.
 * <p>
 * Controls whether the session can read and write persistent memory.
 *
 * @since 1.6.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllowCopilotExperimental
public class MemoryConfiguration {

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("mode")
    private MemoryMode mode;

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

    /**
     * Gets where memories are placed.
     *
     * @return the memory mode, or {@code null} for the default
     *         {@link MemoryMode#SYSTEM_PROMPT}
     * @apiNote This option is experimental and may change in a future version.
     */
    @CopilotExperimental
    public MemoryMode getMode() {
        return mode;
    }

    /**
     * Sets where memories are placed. {@link MemoryMode#SYSTEM_PROMPT} (default)
     * places memories in the system prompt. {@link MemoryMode#USER_PROMPT} places
     * memories in generated user-role messages.
     *
     * @param mode
     *            the memory mode, or {@code null} to use the default
     * @return this config for method chaining
     * @apiNote This option is experimental and may change in a future version.
     */
    @CopilotExperimental
    public MemoryConfiguration setMode(MemoryMode mode) {
        this.mode = mode;
        return this;
    }
}
