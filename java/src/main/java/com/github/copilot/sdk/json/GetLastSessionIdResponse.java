package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal response object from getting the last session ID.
 *
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetLastSessionIdResponse(@JsonProperty("sessionId") String sessionId) {
}
