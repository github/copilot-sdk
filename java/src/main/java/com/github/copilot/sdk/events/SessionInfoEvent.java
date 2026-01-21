/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: session.info
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SessionInfoEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SessionInfoData data;

    @Override
    public String getType() {
        return "session.info";
    }

    public SessionInfoData getData() {
        return data;
    }

    public void setData(SessionInfoData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionInfoData {

        @JsonProperty("infoType")
        private String infoType;

        @JsonProperty("message")
        private String message;

        public String getInfoType() {
            return infoType;
        }

        public void setInfoType(String infoType) {
            this.infoType = infoType;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
