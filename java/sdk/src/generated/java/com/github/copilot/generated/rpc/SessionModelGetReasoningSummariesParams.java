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
 * Host presentation state the reasoning-summaries policy depends on.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelGetReasoningSummariesParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The host's persisted `showReasoning` user setting, omitted when unset. */
    @JsonProperty("persistedShowReasoning") Boolean persistedShowReasoning,
    /** Whether the host is currently showing reasoning summaries. Defaults to false. */
    @JsonProperty("summariesVisible") Boolean summariesVisible
) {

    /**
     * Creates host presentation state; the session-scoped wrapper supplies the session ID.
     *
     * @param persistedShowReasoning the persisted visibility setting, or null when unset
     * @param summariesVisible whether summaries are visible, or null for the default
     */
    public SessionModelGetReasoningSummariesParams(Boolean persistedShowReasoning, Boolean summariesVisible) {
        this(null, persistedShowReasoning, summariesVisible);
    }
}
