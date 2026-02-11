/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for a session-end hook.
 * <p>
 * Allows specifying cleanup actions and session summary.
 *
 * @param suppressOutput
 *            {@code true} to suppress output, or {@code null}
 * @param cleanupActions
 *            the cleanup actions to perform, or {@code null}
 * @param sessionSummary
 *            the session summary, or {@code null}
 * @since 1.0.7
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionEndHookOutput(@JsonProperty("suppressOutput") Boolean suppressOutput,
        @JsonProperty("cleanupActions") List<String> cleanupActions,
        @JsonProperty("sessionSummary") String sessionSummary) {
}
