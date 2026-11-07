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
 * Session event "permission.messageAuthorizationRead". Historical decode-only extractor progress marker. Current runtimes do not run or resume extraction from it.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionMessageAuthorizationReadEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.messageAuthorizationRead"; }

    @JsonProperty("data")
    private PermissionMessageAuthorizationReadEventData data;

    public PermissionMessageAuthorizationReadEventData getData() { return data; }
    public void setData(PermissionMessageAuthorizationReadEventData data) { this.data = data; }

    /** Data payload for {@link PermissionMessageAuthorizationReadEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionMessageAuthorizationReadEventData(
        /** The human turn that was read by the proposer. */
        @CopilotExperimental
        @JsonProperty("turnIndex") Long turnIndex,
        /** Whether this read activates ongoing message-backed extraction. False for a contextual-assent-only pass while auto-approval is off, so unrelated future messages remain outside extraction. */
        @CopilotExperimental
        @JsonProperty("activatesExtraction") Boolean activatesExtraction
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionMessageAuthorizationReadEventData {}

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
            private Boolean activatesExtraction;

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
             * Sets the {@code activatesExtraction} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder activatesExtraction(Boolean value) {
                this.activatesExtraction = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public PermissionMessageAuthorizationReadEventData build() {
                if (turnIndex == null) throw new IllegalStateException("Missing required property: turnIndex");
                return new PermissionMessageAuthorizationReadEventData(turnIndex, activatesExtraction);
            }
        }
    }
}
