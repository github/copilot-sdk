/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Aggregate code change metrics for the session
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShutdownCodeChanges(
    /** Total number of lines added during the session */
    @JsonProperty("linesAdded") Double linesAdded,
    /** Total number of lines removed during the session */
    @JsonProperty("linesRemoved") Double linesRemoved,
    /** List of file paths that were modified during the session */
    @JsonProperty("filesModified") List<String> filesModified
) {
}
