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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsEnrichMetadataResult(
    /** Enriched records, with summary and context backfilled. Sessions confirmed empty and unnamed may be omitted. */
    @JsonProperty("sessions") List<LocalSessionMetadataValue> sessions
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsEnrichMetadataResult {}

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
        private List<LocalSessionMetadataValue> sessions;

        /**
         * Sets the {@code sessions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessions(List<LocalSessionMetadataValue> value) {
            this.sessions = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsEnrichMetadataResult build() {
            if (sessions == null) throw new IllegalStateException("Missing required property: sessions");
            return new SessionsEnrichMetadataResult(sessions);
        }
    }
}
