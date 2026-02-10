/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.model_change
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionModelChangeEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionModelChangeData data;

    @Override
    public String getType() {
        return "session.model_change";
    }

    public SessionModelChangeData getData() {
        return data;
    }

    public void setData(SessionModelChangeData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionModelChangeData(@JsonProperty("previousModel") String previousModel,
            @JsonProperty("newModel") String newModel) {
    }
}
