/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The {@code session.warning} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionWarningEvent extends SessionEvent {

    @Override
    public String getType() { return "session.warning"; }

    @JsonProperty("data")
    private SessionWarningEventData data;

    public SessionWarningEventData getData() { return data; }
    public void setData(SessionWarningEventData data) { this.data = data; }

    /** Data payload for {@link SessionWarningEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWarningEventData(
        /** Category of warning (e.g., "subscription", "policy", "mcp") */
        @JsonProperty("warningType") String warningType,
        /** Human-readable warning message for display in the timeline */
        @JsonProperty("message") String message,
        /** Optional URL associated with this warning that the user can open in a browser */
        @JsonProperty("url") String url
    ) {
    }
}
