/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: abort
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AbortEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AbortData data;

    @Override
    public String getType() {
        return "abort";
    }

    public AbortData getData() {
        return data;
    }

    public void setData(AbortData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbortData(@JsonProperty("reason") String reason) {
    }
}
