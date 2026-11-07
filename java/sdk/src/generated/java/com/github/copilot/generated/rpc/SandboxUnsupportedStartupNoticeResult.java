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
 * The startup warning, absent when the machine can run a sandbox.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxUnsupportedStartupNoticeResult(
    /** The user-facing warning, carrying the platform remedy and the recovery the policy in force actually allows. Absent when this machine supports a sandbox, so a host can call unconditionally and print only what it gets. */
    @JsonProperty("notice") String notice
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxUnsupportedStartupNoticeResult {}

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
        private String notice;

        /**
         * Sets the {@code notice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder notice(String value) {
            this.notice = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxUnsupportedStartupNoticeResult build() {
            return new SandboxUnsupportedStartupNoticeResult(notice);
        }
    }
}
