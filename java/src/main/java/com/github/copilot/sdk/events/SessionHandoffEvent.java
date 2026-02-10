/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Event: session.handoff
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionHandoffEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionHandoffData data;

    @Override
    public String getType() {
        return "session.handoff";
    }

    public SessionHandoffData getData() {
        return data;
    }

    public void setData(SessionHandoffData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionHandoffData(@JsonProperty("handoffTime") OffsetDateTime handoffTime,
            @JsonProperty("sourceType") String sourceType, @JsonProperty("repository") Repository repository,
            @JsonProperty("context") String context, @JsonProperty("summary") String summary,
            @JsonProperty("remoteSessionId") String remoteSessionId) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Repository(@JsonProperty("owner") String owner, @JsonProperty("name") String name,
                @JsonProperty("branch") String branch) {
        }
    }
}
