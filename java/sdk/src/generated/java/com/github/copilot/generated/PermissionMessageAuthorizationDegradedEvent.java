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
 * Session event "permission.messageAuthorizationDegraded". Historical decode-only degradation marker from the retired extractor. Current runtimes ignore it for permission decisions.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionMessageAuthorizationDegradedEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.messageAuthorizationDegraded"; }

    @JsonProperty("data")
    private PermissionMessageAuthorizationDegradedEventData data;

    public PermissionMessageAuthorizationDegradedEventData getData() { return data; }
    public void setData(PermissionMessageAuthorizationDegradedEventData data) { this.data = data; }

    /** Data payload for {@link PermissionMessageAuthorizationDegradedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionMessageAuthorizationDegradedEventData(
        /** The human turn that could not be represented safely. */
        @CopilotExperimental
        @JsonProperty("turnIndex") Long turnIndex
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionMessageAuthorizationDegradedEventData {}

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
            private Long turnIndex;

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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public PermissionMessageAuthorizationDegradedEventData build() {
                if (turnIndex == null) throw new IllegalStateException("Missing required property: turnIndex");
                return new PermissionMessageAuthorizationDegradedEventData(turnIndex);
            }
        }
    }
}
