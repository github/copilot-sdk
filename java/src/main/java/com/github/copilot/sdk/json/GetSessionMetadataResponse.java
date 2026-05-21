/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal response object from getting session metadata by ID.
 *
 * @param session
 *            the session metadata, or {@code null} if not found
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetSessionMetadataResponse(@JsonProperty("session") SessionMetadata session) {
}
