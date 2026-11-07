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
 * Quota snapshot map from the raw Copilot user-response passthrough, with chat, completions, premium-interactions, and other entries.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CopilotUserResponseQuotaSnapshots(
    /** Chat quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields. */
    @JsonProperty("chat") CopilotUserResponseQuotaSnapshotsChat chat,
    /** Completions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields. */
    @JsonProperty("completions") CopilotUserResponseQuotaSnapshotsCompletions completions,
    /** Premium-interactions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields. */
    @JsonProperty("premium_interactions") CopilotUserResponseQuotaSnapshotsPremiumInteractions premiumInteractions
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CopilotUserResponseQuotaSnapshots {}

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
        private CopilotUserResponseQuotaSnapshotsChat chat;
        private CopilotUserResponseQuotaSnapshotsCompletions completions;
        private CopilotUserResponseQuotaSnapshotsPremiumInteractions premiumInteractions;

        /**
         * Sets the {@code chat} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder chat(CopilotUserResponseQuotaSnapshotsChat value) {
            this.chat = value;
            return this;
        }

        /**
         * Sets the {@code completions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder completions(CopilotUserResponseQuotaSnapshotsCompletions value) {
            this.completions = value;
            return this;
        }

        /**
         * Sets the {@code premium_interactions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder premiumInteractions(CopilotUserResponseQuotaSnapshotsPremiumInteractions value) {
            this.premiumInteractions = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CopilotUserResponseQuotaSnapshots build() {
            return new CopilotUserResponseQuotaSnapshots(chat, completions, premiumInteractions);
        }
    }
}
