/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "permission.contextualAuthorization". Historical decode-only contextual authorization claim. Current runtimes preserve the payload but do not establish authority from it.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionContextualAuthorizationEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.contextualAuthorization"; }

    @JsonProperty("data")
    private PermissionContextualAuthorizationEventData data;

    public PermissionContextualAuthorizationEventData getData() { return data; }
    public void setData(PermissionContextualAuthorizationEventData data) { this.data = data; }

    /** Data payload for {@link PermissionContextualAuthorizationEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionContextualAuthorizationEventData(
        /** Deterministic identity of the contextual message grant. */
        @CopilotExperimental
        @JsonProperty("recordId") String recordId,
        /** Original blocked permission request selected by deterministic event ordering, never by the extraction model. */
        @CopilotExperimental
        @JsonProperty("requestId") String requestId,
        /** Human turn containing the contextual decision. */
        @CopilotExperimental
        @JsonProperty("turnIndex") Long turnIndex,
        /** Whether the contextual human span granted or denied authority. */
        @CopilotExperimental
        @JsonProperty("polarity") PermissionMessageAuthorizationPolarity polarity,
        /** Start byte offset of the contextual decision span within the turn. */
        @CopilotExperimental
        @JsonProperty("spanStart") Long spanStart,
        /** End byte offset of the contextual decision span within the turn. */
        @CopilotExperimental
        @JsonProperty("spanEnd") Long spanEnd
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionContextualAuthorizationEventData {}

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
            private String recordId;
            private String requestId;
            private Long turnIndex;
            private PermissionMessageAuthorizationPolarity polarity;
            private Long spanStart;
            private Long spanEnd;

            /**
             * Sets the {@code recordId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder recordId(String value) {
                this.recordId = value;
                return this;
            }

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code turnIndex} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder turnIndex(Long value) {
                this.turnIndex = value;
                return this;
            }

            /**
             * Sets the {@code polarity} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder polarity(PermissionMessageAuthorizationPolarity value) {
                this.polarity = value;
                return this;
            }

            /**
             * Sets the {@code spanStart} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder spanStart(Long value) {
                this.spanStart = value;
                return this;
            }

            /**
             * Sets the {@code spanEnd} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder spanEnd(Long value) {
                this.spanEnd = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public PermissionContextualAuthorizationEventData build() {
                if (recordId == null) throw new IllegalStateException("Missing required property: recordId");
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (turnIndex == null) throw new IllegalStateException("Missing required property: turnIndex");
                if (polarity == null) throw new IllegalStateException("Missing required property: polarity");
                if (spanStart == null) throw new IllegalStateException("Missing required property: spanStart");
                if (spanEnd == null) throw new IllegalStateException("Missing required property: spanEnd");
                return new PermissionContextualAuthorizationEventData(recordId, requestId, turnIndex, polarity, spanStart, spanEnd);
            }
        }
    }
}
