/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.usage_info
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionUsageInfoEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionUsageInfoData data;

    @Override
    public String getType() {
        return "session.usage_info";
    }

    public SessionUsageInfoData getData() {
        return data;
    }

    public void setData(SessionUsageInfoData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionUsageInfoData(@JsonProperty("tokenLimit") double tokenLimit,
            @JsonProperty("currentTokens") double currentTokens,
            @JsonProperty("messagesLength") double messagesLength) {
    }
}
