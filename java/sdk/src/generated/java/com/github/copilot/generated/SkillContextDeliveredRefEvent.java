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
 * Session event "skill.context_delivered_ref". Internal durable receipt that reconstructs exact model-visible skill context from earlier session content.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillContextDeliveredRefEvent extends SessionEvent {

    @Override
    public String getType() { return "skill.context_delivered_ref"; }

    @JsonProperty("data")
    private SkillContextDeliveredRefEventData data;

    public SkillContextDeliveredRefEventData getData() { return data; }
    public void setData(SkillContextDeliveredRefEventData data) { this.data = data; }

    /** Data payload for {@link SkillContextDeliveredRefEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SkillContextDeliveredRefEventData(
        /** Content identifier of an earlier inline skill event in this session, in the prefixed form `sha256:<lowercase hex digest>` over the UTF-8 bytes of that event's `content` */
        @JsonProperty("contentId") String contentId,
        /** Exact text preceding the referenced content in the delivered wrapper */
        @JsonProperty("prefix") String prefix,
        /** Exact text following the referenced content in the delivered wrapper */
        @JsonProperty("suffix") String suffix,
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
        public SkillContextDeliveredRefEventData {}

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
            private String contentId;
            private String prefix;
            private String suffix;
            private String source;
            private String interactionId;

            /**
             * Sets the {@code contentId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contentId(String value) {
                this.contentId = value;
                return this;
            }

            /**
             * Sets the {@code prefix} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder prefix(String value) {
                this.prefix = value;
                return this;
            }

            /**
             * Sets the {@code suffix} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder suffix(String value) {
                this.suffix = value;
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
            public SkillContextDeliveredRefEventData build() {
                if (contentId == null) throw new IllegalStateException("Missing required property: contentId");
                if (source == null) throw new IllegalStateException("Missing required property: source");
                return new SkillContextDeliveredRefEventData(contentId, prefix, suffix, source, interactionId);
            }
        }
    }
}
