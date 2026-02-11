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
 * @param timestamp
 *            the timestamp in milliseconds since epoch when the session started
 * @param cwd
 *            the current working directory
 * @param source
 *            the source: "startup", "resume", or "new"
 * @param initialPrompt
 *            the initial prompt, or {@code null}
 * @since 1.0.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionStartHookInput(@JsonProperty("timestamp") long timestamp, @JsonProperty("cwd") String cwd,
        @JsonProperty("source") String source, @JsonProperty("initialPrompt") String initialPrompt) {
}
