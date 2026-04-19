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
 * The {@code session.resume} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionResumeEvent extends SessionEvent {

    @Override
    public String getType() { return "session.resume"; }

    @JsonProperty("data")
    private SessionResumeEventData data;

    public SessionResumeEventData getData() { return data; }
    public void setData(SessionResumeEventData data) { this.data = data; }

    /** Data payload for {@link SessionResumeEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionResumeEventData(
        /** ISO 8601 timestamp when the session was resumed */
        @JsonProperty("resumeTime") OffsetDateTime resumeTime,
        /** Total number of persisted events in the session at the time of resume */
        @JsonProperty("eventCount") Double eventCount,
        /** Model currently selected at resume time */
        @JsonProperty("selectedModel") String selectedModel,
        /** Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh") */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Updated working directory and git context at resume time */
        @JsonProperty("context") SessionResumeEventDataContext context,
        /** Whether the session was already in use by another client at resume time */
        @JsonProperty("alreadyInUse") Boolean alreadyInUse,
        /** Whether this session supports remote steering via Mission Control */
        @JsonProperty("remoteSteerable") Boolean remoteSteerable
    ) {

        /** Updated working directory and git context at resume time */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionResumeEventDataContext(
            /** Current working directory path */
            @JsonProperty("cwd") String cwd,
            /** Root directory of the git repository, resolved via git rev-parse */
            @JsonProperty("gitRoot") String gitRoot,
            /** Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps) */
            @JsonProperty("repository") String repository,
            /** Hosting platform type of the repository (github or ado) */
            @JsonProperty("hostType") SessionResumeEventDataContextHostType hostType,
            /** Current git branch name */
            @JsonProperty("branch") String branch,
            /** Head commit of current git branch at session start time */
            @JsonProperty("headCommit") String headCommit,
            /** Base commit of current git branch at session start time */
            @JsonProperty("baseCommit") String baseCommit
        ) {

            /** Hosting platform type of the repository (github or ado) */
            public enum SessionResumeEventDataContextHostType {
                /** The {@code github} variant. */
                GITHUB("github"),
                /** The {@code ado} variant. */
                ADO("ado");

                private final String value;
                SessionResumeEventDataContextHostType(String value) { this.value = value; }
                @com.fasterxml.jackson.annotation.JsonValue
                public String getValue() { return value; }
                @com.fasterxml.jackson.annotation.JsonCreator
                public static SessionResumeEventDataContextHostType fromValue(String value) {
                    for (SessionResumeEventDataContextHostType v : values()) {
                        if (v.value.equals(value)) return v;
                    }
                    throw new IllegalArgumentException("Unknown SessionResumeEventDataContextHostType value: " + value);
                }
            }
        }
    }
}
