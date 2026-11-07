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
 * Host presentation state the reasoning-summaries policy depends on.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelGetReasoningSummariesParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The host's persisted `showReasoning` user setting, omitted when unset. */
    @JsonProperty("persistedShowReasoning") Boolean persistedShowReasoning,
    /** Whether the host is currently showing reasoning summaries. Defaults to false. */
    @JsonProperty("summariesVisible") Boolean summariesVisible
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelGetReasoningSummariesParams {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String sessionId;
        private Boolean persistedShowReasoning;
        private Boolean summariesVisible;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code persistedShowReasoning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder persistedShowReasoning(Boolean value) {
            this.persistedShowReasoning = value;
            return this;
        }

        /**
         * Sets the {@code summariesVisible} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder summariesVisible(Boolean value) {
            this.summariesVisible = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModelGetReasoningSummariesParams build() {
            return new SessionModelGetReasoningSummariesParams(sessionId, persistedShowReasoning, summariesVisible);
        }
    }

    /**
     * Creates host presentation state; the session-scoped wrapper supplies the session ID.
     * @deprecated Use named construction via {@link #builder()}.
     * @param persistedShowReasoning The host's persisted `showReasoning` user setting, omitted when unset.
     * @param summariesVisible Whether the host is currently showing reasoning summaries. Defaults to false.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelGetReasoningSummariesParams(
        Boolean persistedShowReasoning,
        Boolean summariesVisible
    ) {
        this(null, persistedShowReasoning, summariesVisible);
    }
}
