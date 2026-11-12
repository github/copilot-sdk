/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Internal absolute code-change totals reported by the owning host.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionUsageSetCodeChangesParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Absolute added-line total, replacing the previous reading. */
    @JsonProperty("linesAdded") Double linesAdded,
    /** Absolute removed-line total, replacing the previous reading. */
    @JsonProperty("linesRemoved") Double linesRemoved,
    /** Absolute changed-file count; omission preserves the previous count. */
    @JsonProperty("filesCount") Double filesCount
) {
}
