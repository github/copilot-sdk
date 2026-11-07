/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Built-in session diagnostics to include in the bundle. Omitted fields default to true.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebugCollectLogsInclude(
    /** Include the session event log (`events.jsonl`). Defaults to true. */
    @JsonProperty("events") Boolean events,
    /** Include process logs for the session. Defaults to true. */
    @JsonProperty("processLogs") Boolean processLogs,
    /** Include interactive shell logs written under the session's `shell-logs` directory. Defaults to true. */
    @JsonProperty("shellLogs") Boolean shellLogs,
    /** Server-local path to the session's events.jsonl file. Internal callers normally omit this and let the runtime derive it from the session. */
    @JsonProperty("eventsPath") String eventsPath,
    /** Server-local path to the current process log. When set, it is included as `process.log` and its directory is searched for prior logs from the same session. */
    @JsonProperty("currentProcessLogPath") String currentProcessLogPath,
    /** Server-local process log directory to search when `currentProcessLogPath` is unavailable, useful for collecting logs for inactive sessions. */
    @JsonProperty("processLogDirectory") String processLogDirectory,
    /** Maximum number of previous process logs to include. Defaults to 5. */
    @JsonProperty("previousProcessLogLimit") Long previousProcessLogLimit
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DebugCollectLogsInclude {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private Boolean events;
        private Boolean processLogs;
        private Boolean shellLogs;
        private String eventsPath;
        private String currentProcessLogPath;
        private String processLogDirectory;
        private Long previousProcessLogLimit;

        /**
         * Sets the {@code events} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder events(Boolean value) {
            this.events = value;
            return this;
        }

        /**
         * Sets the {@code processLogs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder processLogs(Boolean value) {
            this.processLogs = value;
            return this;
        }

        /**
         * Sets the {@code shellLogs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder shellLogs(Boolean value) {
            this.shellLogs = value;
            return this;
        }

        /**
         * Sets the {@code eventsPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder eventsPath(String value) {
            this.eventsPath = value;
            return this;
        }

        /**
         * Sets the {@code currentProcessLogPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder currentProcessLogPath(String value) {
            this.currentProcessLogPath = value;
            return this;
        }

        /**
         * Sets the {@code processLogDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder processLogDirectory(String value) {
            this.processLogDirectory = value;
            return this;
        }

        /**
         * Sets the {@code previousProcessLogLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder previousProcessLogLimit(Long value) {
            this.previousProcessLogLimit = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DebugCollectLogsInclude build() {
            return new DebugCollectLogsInclude(events, processLogs, shellLogs, eventsPath, currentProcessLogPath, processLogDirectory, previousProcessLogLimit);
        }
    }
}
