/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Public owner attribution for a client-owned task. Identifiers are opaque and never authorize requests.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskClientOwner(
    /** Opaque session-scoped participant identity */
    @JsonProperty("participantId") String participantId,
    /** Opaque identity of the currently or most recently bound session join */
    @JsonProperty("joinId") String joinId,
    /** Class of the task owner */
    @JsonProperty("kind") TaskClientOwnerKind kind,
    /** Display-only owner name */
    @JsonProperty("displayName") String displayName,
    /** Display-only owner source */
    @JsonProperty("source") String source,
    /** Whether this task's bound join is currently connected */
    @JsonProperty("presence") TaskClientOwnerPresence presence,
    /** ISO 8601 timestamp when the bound join disconnected */
    @JsonProperty("disconnectedAt") OffsetDateTime disconnectedAt
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TaskClientOwner {}

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
        private String participantId;
        private String joinId;
        private TaskClientOwnerKind kind;
        private String displayName;
        private String source;
        private TaskClientOwnerPresence presence;
        private OffsetDateTime disconnectedAt;

        /**
         * Sets the {@code participantId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder participantId(String value) {
            this.participantId = value;
            return this;
        }

        /**
         * Sets the {@code joinId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder joinId(String value) {
            this.joinId = value;
            return this;
        }

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(TaskClientOwnerKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
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
         * Sets the {@code presence} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder presence(TaskClientOwnerPresence value) {
            this.presence = value;
            return this;
        }

        /**
         * Sets the {@code disconnectedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disconnectedAt(OffsetDateTime value) {
            this.disconnectedAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public TaskClientOwner build() {
            if (participantId == null) throw new IllegalStateException("Missing required property: participantId");
            if (joinId == null) throw new IllegalStateException("Missing required property: joinId");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (presence == null) throw new IllegalStateException("Missing required property: presence");
            return new TaskClientOwner(participantId, joinId, kind, displayName, source, presence, disconnectedAt);
        }
    }
}
