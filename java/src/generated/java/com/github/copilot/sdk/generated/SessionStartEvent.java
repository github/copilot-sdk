/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * The {@code session.start} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionStartEvent extends SessionEvent {

    @Override
    public String getType() { return "session.start"; }

    @JsonProperty("data")
    private SessionStartEventData data;

    public SessionStartEventData getData() { return data; }
    public void setData(SessionStartEventData data) { this.data = data; }

    /** Data payload for {@link SessionStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionStartEventData(
        /** Unique identifier for the session */
        @JsonProperty("sessionId") String sessionId,
        /** Schema version number for the session event format */
        @JsonProperty("version") Double version,
        /** Identifier of the software producing the events (e.g., "copilot-agent") */
        @JsonProperty("producer") String producer,
        /** Version string of the Copilot application */
        @JsonProperty("copilotVersion") String copilotVersion,
        /** ISO 8601 timestamp when the session was created */
        @JsonProperty("startTime") OffsetDateTime startTime,
        /** Model selected at session creation time, if any */
        @JsonProperty("selectedModel") String selectedModel,
        /** Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh") */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Working directory and git context at session start */
        @JsonProperty("context") SessionStartEventDataContext context,
        /** Whether the session was already in use by another client at start time */
        @JsonProperty("alreadyInUse") Boolean alreadyInUse,
        /** Whether this session supports remote steering via Mission Control */
        @JsonProperty("remoteSteerable") Boolean remoteSteerable
    ) {

        /** Working directory and git context at session start */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionStartEventDataContext(
            /** Current working directory path */
            @JsonProperty("cwd") String cwd,
            /** Root directory of the git repository, resolved via git rev-parse */
            @JsonProperty("gitRoot") String gitRoot,
            /** Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps) */
            @JsonProperty("repository") String repository,
            /** Hosting platform type of the repository (github or ado) */
            @JsonProperty("hostType") SessionStartEventDataContextHostType hostType,
            /** Current git branch name */
            @JsonProperty("branch") String branch,
            /** Head commit of current git branch at session start time */
            @JsonProperty("headCommit") String headCommit,
            /** Base commit of current git branch at session start time */
            @JsonProperty("baseCommit") String baseCommit
        ) {

            /** Hosting platform type of the repository (github or ado) */
            public enum SessionStartEventDataContextHostType {
                /** The {@code github} variant. */
                GITHUB("github"),
                /** The {@code ado} variant. */
                ADO("ado");

                private final String value;
                SessionStartEventDataContextHostType(String value) { this.value = value; }
                @com.fasterxml.jackson.annotation.JsonValue
                public String getValue() { return value; }
                @com.fasterxml.jackson.annotation.JsonCreator
                public static SessionStartEventDataContextHostType fromValue(String value) {
                    for (SessionStartEventDataContextHostType v : values()) {
                        if (v.value.equals(value)) return v;
                    }
                    throw new IllegalArgumentException("Unknown SessionStartEventDataContextHostType value: " + value);
                }
            }
        }
    }
}
