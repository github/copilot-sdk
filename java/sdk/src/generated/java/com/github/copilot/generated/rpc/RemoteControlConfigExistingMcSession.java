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
 * Reattach to an existing MC session without creating a new one.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteControlConfigExistingMcSession(
    /** Existing MC session ID to reattach to. */
    @JsonProperty("mcSessionId") String mcSessionId,
    /** Existing MC task ID for the reattached session. */
    @JsonProperty("mcTaskId") String mcTaskId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public RemoteControlConfigExistingMcSession {}

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
        private String mcSessionId;
        private String mcTaskId;

        /**
         * Sets the {@code mcSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcSessionId(String value) {
            this.mcSessionId = value;
            return this;
        }

        /**
         * Sets the {@code mcTaskId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcTaskId(String value) {
            this.mcTaskId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RemoteControlConfigExistingMcSession build() {
            if (mcSessionId == null) throw new IllegalStateException("Missing required property: mcSessionId");
            if (mcTaskId == null) throw new IllegalStateException("Missing required property: mcTaskId");
            return new RemoteControlConfigExistingMcSession(mcSessionId, mcTaskId);
        }
    }
}
