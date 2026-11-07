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
 * GitHub URL and remote export and steering state, with an optional warning when cloud session storage policy refuses export.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionRemoteEnableResult(
    /** GitHub frontend URL for this session */
    @JsonProperty("url") String url,
    /** Whether remote steering is enabled */
    @JsonProperty("remoteSteerable") Boolean remoteSteerable,
    /** Whether this request accepted or retained remote export. False when export is disabled or refused; initialization may still be pending when true. */
    @JsonProperty("started") Boolean started,
    /** Warning explaining why cloud session storage policy prevented export. Absent when no policy warning applies. */
    @JsonProperty("warning") String warning
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionRemoteEnableResult {}

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
        private String url;
        private Boolean remoteSteerable;
        private Boolean started;
        private String warning;

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Sets the {@code remoteSteerable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remoteSteerable(Boolean value) {
            this.remoteSteerable = value;
            return this;
        }

        /**
         * Sets the {@code started} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder started(Boolean value) {
            this.started = value;
            return this;
        }

        /**
         * Sets the {@code warning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warning(String value) {
            this.warning = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionRemoteEnableResult build() {
            if (remoteSteerable == null) throw new IllegalStateException("Missing required property: remoteSteerable");
            return new SessionRemoteEnableResult(url, remoteSteerable, started, warning);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param url GitHub frontend URL for this session
     * @param remoteSteerable Whether remote steering is enabled
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionRemoteEnableResult(
        String url,
        Boolean remoteSteerable
    ) {
        this(url, remoteSteerable, null, null);
    }
}
