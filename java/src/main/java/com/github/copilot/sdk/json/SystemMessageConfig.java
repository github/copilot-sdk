/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.copilot.sdk.SystemMessageMode;

/**
 * Configuration for customizing the system message.
 * <p>
 * The system message controls the behavior and personality of the AI assistant.
 * This configuration allows you to either append to or replace the default
 * system message.
 *
 * <h2>Example - Append Mode</h2>
 *
 * <pre>{@code
 * var config = new SystemMessageConfig().setMode(SystemMessageMode.APPEND)
 * 		.setContent("Always respond in a formal tone.");
 * }</pre>
 *
 * <h2>Example - Replace Mode</h2>
 *
 * <pre>{@code
 * var config = new SystemMessageConfig().setMode(SystemMessageMode.REPLACE)
 * 		.setContent("You are a helpful coding assistant.");
 * }</pre>
 *
 * @see SessionConfig#setSystemMessage(SystemMessageConfig)
 * @see SystemMessageMode
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemMessageConfig {

    private SystemMessageMode mode;
    private String content;

    /**
     * Gets the system message mode.
     *
     * @return the mode (APPEND or REPLACE)
     */
    public SystemMessageMode getMode() {
        return mode;
    }

    /**
     * Sets the system message mode.
     * <p>
     * Use {@link SystemMessageMode#APPEND} to add to the default system message
     * while preserving guardrails, or {@link SystemMessageMode#REPLACE} to fully
     * customize the system message.
     *
     * @param mode
     *            the mode (APPEND or REPLACE)
     * @return this config for method chaining
     */
    public SystemMessageConfig setMode(SystemMessageMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Gets the system message content.
     *
     * @return the content to append or use as replacement
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the system message content.
     * <p>
     * This is the text that will be appended to or replace the default system
     * message, depending on the configured mode.
     *
     * @param content
     *            the system message content
     * @return this config for method chaining
     */
    public SystemMessageConfig setContent(String content) {
        this.content = content;
        return this;
    }
}
