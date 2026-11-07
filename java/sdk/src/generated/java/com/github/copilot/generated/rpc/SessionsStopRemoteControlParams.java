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
 * Request parameters for the {@code sessions.stopRemoteControl} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsStopRemoteControlParams(
    /** When provided, the stop is rejected unless the singleton currently points at this session id (compare-and-swap semantics). */
    @JsonProperty("expectedSessionId") String expectedSessionId,
    /** When true, the singleton is unconditionally torn down regardless of `expectedSessionId`. Use during shutdown or explicit `/remote off`. */
    @JsonProperty("force") Boolean force
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsStopRemoteControlParams {}

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
        private String expectedSessionId;
        private Boolean force;

        /**
         * Sets the {@code expectedSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expectedSessionId(String value) {
            this.expectedSessionId = value;
            return this;
        }

        /**
         * Sets the {@code force} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder force(Boolean value) {
            this.force = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsStopRemoteControlParams build() {
            return new SessionsStopRemoteControlParams(expectedSessionId, force);
        }
    }
}
