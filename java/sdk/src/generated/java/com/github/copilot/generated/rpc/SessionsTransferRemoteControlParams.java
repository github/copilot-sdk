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
 * Parameters for atomically rebinding the remote-control singleton.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsTransferRemoteControlParams(
    /** Local session id to point remote control at. */
    @JsonProperty("toSessionId") String toSessionId,
    /** When provided, the transfer is rejected unless the singleton currently points at this session id (compare-and-swap semantics to avoid clobbering newer state). */
    @JsonProperty("expectedFromSessionId") String expectedFromSessionId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsTransferRemoteControlParams {}

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
        private String toSessionId;
        private String expectedFromSessionId;

        /**
         * Sets the {@code toSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toSessionId(String value) {
            this.toSessionId = value;
            return this;
        }

        /**
         * Sets the {@code expectedFromSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expectedFromSessionId(String value) {
            this.expectedFromSessionId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsTransferRemoteControlParams build() {
            if (toSessionId == null) throw new IllegalStateException("Missing required property: toSessionId");
            return new SessionsTransferRemoteControlParams(toSessionId, expectedFromSessionId);
        }
    }
}
