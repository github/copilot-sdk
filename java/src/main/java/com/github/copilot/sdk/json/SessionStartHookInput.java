/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input for a session-start hook.
 * <p>
 * This hook is invoked when a session starts, allowing you to perform
 * initialization or modify the session configuration.
 *
 * @since 1.0.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionStartHookInput {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("cwd")
    private String cwd;

    @JsonProperty("source")
    private String source;

    @JsonProperty("initialPrompt")
    private String initialPrompt;

    /**
     * Gets the timestamp when the session started.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the session started.
     *
     * @param timestamp
     *            the timestamp in milliseconds since epoch
     * @return this instance for method chaining
     */
    public SessionStartHookInput setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Gets the current working directory.
     *
     * @return the current working directory
     */
    public String getCwd() {
        return cwd;
    }

    /**
     * Sets the current working directory.
     *
     * @param cwd
     *            the current working directory
     * @return this instance for method chaining
     */
    public SessionStartHookInput setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the source of the session start.
     *
     * @return the source: "startup", "resume", or "new"
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source of the session start.
     *
     * @param source
     *            the source: "startup", "resume", or "new"
     * @return this instance for method chaining
     */
    public SessionStartHookInput setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * Gets the initial prompt, if any.
     *
     * @return the initial prompt, or {@code null}
     */
    public String getInitialPrompt() {
        return initialPrompt;
    }

    /**
     * Sets the initial prompt.
     *
     * @param initialPrompt
     *            the initial prompt
     * @return this instance for method chaining
     */
    public SessionStartHookInput setInitialPrompt(String initialPrompt) {
        this.initialPrompt = initialPrompt;
        return this;
    }
}
