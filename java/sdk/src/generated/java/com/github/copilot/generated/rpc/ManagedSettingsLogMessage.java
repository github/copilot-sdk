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
 * One diagnostic emitted while ingesting managed settings. Returned rather than logged by the runtime so the host decides where ingestion diagnostics go, which matters because an ingestion runs before a session exists to carry a log sink.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsLogMessage(
    /** Severity of this diagnostic. */
    @JsonProperty("level") ManagedSettingsLogLevel level,
    /** Human-readable diagnostic text. */
    @JsonProperty("message") String message
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsLogMessage {}

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
        private ManagedSettingsLogLevel level;
        private String message;

        /**
         * Sets the {@code level} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder level(ManagedSettingsLogLevel value) {
            this.level = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsLogMessage build() {
            if (level == null) throw new IllegalStateException("Missing required property: level");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            return new ManagedSettingsLogMessage(level, message);
        }
    }
}
