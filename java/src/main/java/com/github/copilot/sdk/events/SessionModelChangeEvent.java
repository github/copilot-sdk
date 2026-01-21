/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.model_change
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
    public static class SessionModelChangeData {

        @JsonProperty("previousModel")
        private String previousModel;

        @JsonProperty("newModel")
        private String newModel;

        public String getPreviousModel() {
            return previousModel;
        }

        public void setPreviousModel(String previousModel) {
            this.previousModel = previousModel;
        }

        public String getNewModel() {
            return newModel;
        }

        public void setNewModel(String newModel) {
            this.newModel = newModel;
        }
    }
}
