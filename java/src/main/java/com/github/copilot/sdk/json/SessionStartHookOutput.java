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
 * @param additionalContext
 *            additional context to be added to the session, or {@code null}
 * @param modifiedConfig
 *            modified configuration options for the session, or {@code null}
 * @since 1.0.7
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionStartHookOutput(@JsonProperty("additionalContext") String additionalContext,
        @JsonProperty("modifiedConfig") Map<String, Object> modifiedConfig) {
}
