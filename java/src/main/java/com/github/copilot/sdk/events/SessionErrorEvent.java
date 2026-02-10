/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.error
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionErrorEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionErrorData data;

    @Override
    public String getType() {
        return "session.error";
    }

    public SessionErrorData getData() {
        return data;
    }

    public void setData(SessionErrorData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionErrorData(@JsonProperty("errorType") String errorType, @JsonProperty("message") String message,
            @JsonProperty("stack") String stack, @JsonProperty("statusCode") Double statusCode,
            @JsonProperty("providerCallId") String providerCallId) {
    }
}
