/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Ordered accounting identity assigned under the source session's emission lock.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageAccountingIdentity(
    /** Session that assigned this accounting sequence. */
    @JsonProperty("sourceSessionId") String sourceSessionId,
    /** Monotonically increasing sequence within the source session. */
    @JsonProperty("sequence") Long sequence,
    /** Existing API call identifier, or a runtime-generated identity when none was supplied. */
    @JsonProperty("usageId") String usageId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public UsageAccountingIdentity {}

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
        private String sourceSessionId;
        private Long sequence;
        private String usageId;

        /**
         * Sets the {@code sourceSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sourceSessionId(String value) {
            this.sourceSessionId = value;
            return this;
        }

        /**
         * Sets the {@code sequence} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sequence(Long value) {
            this.sequence = value;
            return this;
        }

        /**
         * Sets the {@code usageId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder usageId(String value) {
            this.usageId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UsageAccountingIdentity build() {
            if (sourceSessionId == null) throw new IllegalStateException("Missing required property: sourceSessionId");
            if (sequence == null) throw new IllegalStateException("Missing required property: sequence");
            if (usageId == null) throw new IllegalStateException("Missing required property: usageId");
            return new UsageAccountingIdentity(sourceSessionId, sequence, usageId);
        }
    }
}
