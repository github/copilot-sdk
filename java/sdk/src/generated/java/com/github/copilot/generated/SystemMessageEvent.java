/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "system.message". System/developer instruction content with role and optional template metadata
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemMessageEvent extends SessionEvent {

    @Override
    public String getType() { return "system.message"; }

    @JsonProperty("data")
    private SystemMessageEventData data;

    public SystemMessageEventData getData() { return data; }
    public void setData(SystemMessageEventData data) { this.data = data; }

    /** Data payload for {@link SystemMessageEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SystemMessageEventData(
        /** The system or developer prompt text sent as model input */
        @JsonProperty("content") String content,
        /** Optional ordered structured blocks corresponding to content, retained for prompt-cache layout restoration. */
        @JsonProperty("contentBlocks") List<SystemMessageContentBlock> contentBlocks,
        /** Logical interaction identifier for the model run receiving this prompt */
        @JsonProperty("interactionId") String interactionId,
        /** Message role: "system" for system prompts, "developer" for developer-injected instructions */
        @JsonProperty("role") SystemMessageRole role,
        /** Optional name identifier for the message source */
        @JsonProperty("name") String name,
        /** Metadata about the prompt template and its construction */
        @JsonProperty("metadata") SystemMessageMetadata metadata
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SystemMessageEventData {}

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
            private String content;
            private List<SystemMessageContentBlock> contentBlocks;
            private String interactionId;
            private SystemMessageRole role;
            private String name;
            private SystemMessageMetadata metadata;

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
             * Sets the {@code contentBlocks} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contentBlocks(List<SystemMessageContentBlock> value) {
                this.contentBlocks = value;
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
             * Sets the {@code role} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder role(SystemMessageRole value) {
                this.role = value;
                return this;
            }

            /**
             * Sets the {@code name} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder name(String value) {
                this.name = value;
                return this;
            }

            /**
             * Sets the {@code metadata} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder metadata(SystemMessageMetadata value) {
                this.metadata = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SystemMessageEventData build() {
                if (content == null) throw new IllegalStateException("Missing required property: content");
                if (role == null) throw new IllegalStateException("Missing required property: role");
                return new SystemMessageEventData(content, contentBlocks, interactionId, role, name, metadata);
            }
        }
    }
}
