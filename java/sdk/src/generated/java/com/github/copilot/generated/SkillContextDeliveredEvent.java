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
 * Session event "skill.context_delivered". Exact skill context delivered to the model during a tool phase. This is not a user submission or another skill invocation.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillContextDeliveredEvent extends SessionEvent {

    @Override
    public String getType() { return "skill.context_delivered"; }

    @JsonProperty("data")
    private SkillContextDeliveredEventData data;

    public SkillContextDeliveredEventData getData() { return data; }
    public void setData(SkillContextDeliveredEventData data) { this.data = data; }

    /** Data payload for {@link SkillContextDeliveredEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SkillContextDeliveredEventData(
        /** Exact model-facing skill wrapper, including its invocation-time file context */
        @JsonProperty("content") String content,
        /** Unmodified injection provenance, in the form skill-<invocation-name> */
        @JsonProperty("source") String source,
        /** Interaction that delivered this context, when known */
        @JsonProperty("interactionId") String interactionId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SkillContextDeliveredEventData {}

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
            private String content;
            private String source;
            private String interactionId;

            /**
             * Sets the {@code content} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder content(String value) {
                this.content = value;
                return this;
            }

            /**
             * Sets the {@code source} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder source(String value) {
                this.source = value;
                return this;
            }

            /**
             * Sets the {@code interactionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder interactionId(String value) {
                this.interactionId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SkillContextDeliveredEventData build() {
                if (content == null) throw new IllegalStateException("Missing required property: content");
                if (source == null) throw new IllegalStateException("Missing required property: source");
                return new SkillContextDeliveredEventData(content, source, interactionId);
            }
        }
    }
}
