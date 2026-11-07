/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Outcome of a stopRemoteControl call.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsStopRemoteControlResult(
    /** State of the runtime-managed remote-control singleton. */
    @JsonProperty("status") Object status,
    /** Whether the singleton was actually torn down by this call. */
    @JsonProperty("stopped") Boolean stopped
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsStopRemoteControlResult {}

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
        private Object status;
        private Boolean stopped;

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(Object value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code stopped} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder stopped(Boolean value) {
            this.stopped = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsStopRemoteControlResult build() {
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (stopped == null) throw new IllegalStateException("Missing required property: stopped");
            return new SessionsStopRemoteControlResult(status, stopped);
        }
    }
}
