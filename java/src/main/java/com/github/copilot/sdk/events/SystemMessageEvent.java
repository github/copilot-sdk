/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Event: system.message
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SystemMessageEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SystemMessageData data;

    @Override
    public String getType() {
        return "system.message";
    }

    public SystemMessageData getData() {
        return data;
    }

    public void setData(SystemMessageData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SystemMessageData(@JsonProperty("content") String content, @JsonProperty("type") String type,
            @JsonProperty("metadata") Map<String, Object> metadata) {
    }
}
