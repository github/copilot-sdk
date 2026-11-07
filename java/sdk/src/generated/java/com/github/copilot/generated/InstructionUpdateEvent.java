/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "instruction.update". An authoritative developer-instruction update. Event order defines instruction order. Persistent updates are durable and restore across replay, resume, rewind, fork, and compaction; turn-scoped updates must be emitted with ephemeral=true and are never restored.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class InstructionUpdateEvent extends SessionEvent {

    @Override
    public String getType() { return "instruction.update"; }

    @JsonProperty("data")
    private InstructionUpdateEventData data;

    public InstructionUpdateEventData getData() { return data; }
    public void setData(InstructionUpdateEventData data) { this.data = data; }

    /** Data payload for {@link InstructionUpdateEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InstructionUpdateEventData(
        /** Authoritative instruction text. Empty or whitespace-only content is rejected. */
        @JsonProperty("content") String content,
        /** Authority responsible for the update. */
        @JsonProperty("authority") InstructionUpdateAuthority authority,
        /** Whether the update persists beyond the current turn. */
        @JsonProperty("lifetime") InstructionUpdateLifetime lifetime,
        /** Earliest legal insertion boundary. */
        @JsonProperty("boundary") InstructionUpdateBoundary boundary,
        /** Deterministic behavior when the active provider cannot represent the update. */
        @JsonProperty("fallback") InstructionUpdateFallback fallback,
        /** Optional stable source name for diagnostics and future host-authored updates. */
        @JsonProperty("name") String name,
        /** Whether this update was recorded after the conversation's initial user message. */
        @JsonProperty("postInitial") Boolean postInitial
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param content Authoritative instruction text. Empty or whitespace-only content is rejected.
         * @param authority Authority responsible for the update.
         * @param lifetime Whether the update persists beyond the current turn.
         * @param boundary Earliest legal insertion boundary.
         * @param fallback Deterministic behavior when the active provider cannot represent the update.
         * @param name Optional stable source name for diagnostics and future host-authored updates.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public InstructionUpdateEventData(
            String content,
            InstructionUpdateAuthority authority,
            InstructionUpdateLifetime lifetime,
            InstructionUpdateBoundary boundary,
            InstructionUpdateFallback fallback,
            String name
        ) {
            this(content, authority, lifetime, boundary, fallback, name, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public InstructionUpdateEventData {}

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
            private InstructionUpdateAuthority authority;
            private InstructionUpdateLifetime lifetime;
            private InstructionUpdateBoundary boundary;
            private InstructionUpdateFallback fallback;
            private String name;
            private Boolean postInitial;

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
             * Sets the {@code authority} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder authority(InstructionUpdateAuthority value) {
                this.authority = value;
                return this;
            }

            /**
             * Sets the {@code lifetime} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder lifetime(InstructionUpdateLifetime value) {
                this.lifetime = value;
                return this;
            }

            /**
             * Sets the {@code boundary} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder boundary(InstructionUpdateBoundary value) {
                this.boundary = value;
                return this;
            }

            /**
             * Sets the {@code fallback} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder fallback(InstructionUpdateFallback value) {
                this.fallback = value;
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
             * Sets the {@code postInitial} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder postInitial(Boolean value) {
                this.postInitial = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public InstructionUpdateEventData build() {
                if (content == null) throw new IllegalStateException("Missing required property: content");
                if (authority == null) throw new IllegalStateException("Missing required property: authority");
                if (lifetime == null) throw new IllegalStateException("Missing required property: lifetime");
                if (boundary == null) throw new IllegalStateException("Missing required property: boundary");
                if (fallback == null) throw new IllegalStateException("Missing required property: fallback");
                return new InstructionUpdateEventData(content, authority, lifetime, boundary, fallback, name, postInitial);
            }
        }
    }
}
