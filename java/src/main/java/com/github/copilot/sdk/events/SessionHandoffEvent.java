/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Event: session.handoff
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
    public static class SessionHandoffData {

        @JsonProperty("handoffTime")
        private OffsetDateTime handoffTime;

        @JsonProperty("sourceType")
        private String sourceType;

        @JsonProperty("repository")
        private Repository repository;

        @JsonProperty("context")
        private String context;

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("remoteSessionId")
        private String remoteSessionId;

        public OffsetDateTime getHandoffTime() {
            return handoffTime;
        }

        public void setHandoffTime(OffsetDateTime handoffTime) {
            this.handoffTime = handoffTime;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public Repository getRepository() {
            return repository;
        }

        public void setRepository(Repository repository) {
            this.repository = repository;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getRemoteSessionId() {
            return remoteSessionId;
        }

        public void setRemoteSessionId(String remoteSessionId) {
            this.remoteSessionId = remoteSessionId;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Repository {

            @JsonProperty("owner")
            private String owner;

            @JsonProperty("name")
            private String name;

            @JsonProperty("branch")
            private String branch;

            public String getOwner() {
                return owner;
            }

            public void setOwner(String owner) {
                this.owner = owner;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getBranch() {
                return branch;
            }

            public void setBranch(String branch) {
                this.branch = branch;
            }
        }
    }
}
