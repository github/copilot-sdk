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
 * Optional source filter, metadata-load limit, and context filter applied to the returned sessions.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsListParams(
    /** Which session sources to include. Defaults to `local` for backward compatibility. */
    @JsonProperty("source") SessionSource source,
    /** When provided, only the first N local sessions (sorted by modification time, newest first) load full metadata; remaining sessions return basic info only. Use 0 to return only basic info for every local session. Has no effect on remote entries (which always carry their full shape). */
    @JsonProperty("metadataLimit") Long metadataLimit,
    /** Optional filter applied to the returned sessions */
    @JsonProperty("filter") SessionListFilter filter,
    /** When true, include detached maintenance sessions. Defaults to false for user-facing session lists. */
    @JsonProperty("includeDetached") Boolean includeDetached,
    /** Only meaningful when `source` includes remote. When true, propagates errors from the remote service instead of silently returning an empty remote list. Defaults to false. */
    @JsonProperty("throwOnError") Boolean throwOnError
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsListParams {}

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
        private SessionSource source;
        private Long metadataLimit;
        private SessionListFilter filter;
        private Boolean includeDetached;
        private Boolean throwOnError;

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(SessionSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code metadataLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder metadataLimit(Long value) {
            this.metadataLimit = value;
            return this;
        }

        /**
         * Sets the {@code filter} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder filter(SessionListFilter value) {
            this.filter = value;
            return this;
        }

        /**
         * Sets the {@code includeDetached} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder includeDetached(Boolean value) {
            this.includeDetached = value;
            return this;
        }

        /**
         * Sets the {@code throwOnError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder throwOnError(Boolean value) {
            this.throwOnError = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsListParams build() {
            return new SessionsListParams(source, metadataLimit, filter, includeDetached, throwOnError);
        }
    }
}
