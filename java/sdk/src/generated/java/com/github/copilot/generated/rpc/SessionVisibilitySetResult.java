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
 * Effective sharing status and shareable GitHub URL after updating session visibility.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionVisibilitySetResult(
    /** Whether the session has been synced to Mission Control (i.e. has a GitHub task). When false, the visibility change could not be applied and `status`/`shareUrl` are absent. */
    @JsonProperty("synced") Boolean synced,
    /** Effective sharing status after the update. May differ from the requested status for task types that are already visible to repository readers by default. Absent when the update could not be applied (e.g. the session is not synced or the user is not authenticated). */
    @JsonProperty("status") SessionVisibilityStatus status,
    /** Shareable GitHub URL for the session. Present when the session is synced and the URL can be resolved. */
    @JsonProperty("shareUrl") String shareUrl
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionVisibilitySetResult {}

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
        private Boolean synced;
        private SessionVisibilityStatus status;
        private String shareUrl;

        /**
         * Sets the {@code synced} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder synced(Boolean value) {
            this.synced = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(SessionVisibilityStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code shareUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder shareUrl(String value) {
            this.shareUrl = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionVisibilitySetResult build() {
            if (synced == null) throw new IllegalStateException("Missing required property: synced");
            return new SessionVisibilitySetResult(synced, status, shareUrl);
        }
    }
}
