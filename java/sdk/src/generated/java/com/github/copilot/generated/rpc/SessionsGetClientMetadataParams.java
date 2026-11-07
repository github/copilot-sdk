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
 * Bounded batch request for client-owned metadata from persisted local sessions.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsGetClientMetadataParams(
    /** Session IDs to inspect. Results preserve this order. */
    @JsonProperty("sessionIds") List<String> sessionIds,
    /** Case-sensitive keys to project from each valid bag. Each key must be non-empty, at most 256 UTF-8 bytes, and outside the reserved `copilot/` and `github/` namespaces. Omit to return every entry. */
    @JsonProperty("keys") List<String> keys
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsGetClientMetadataParams {}

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
        private List<String> sessionIds;
        private List<String> keys;

        /**
         * Sets the {@code sessionIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionIds(List<String> value) {
            this.sessionIds = value;
            return this;
        }

        /**
         * Sets the {@code keys} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder keys(List<String> value) {
            this.keys = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsGetClientMetadataParams build() {
            if (sessionIds == null) throw new IllegalStateException("Missing required property: sessionIds");
            return new SessionsGetClientMetadataParams(sessionIds, keys);
        }
    }
}
