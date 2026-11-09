/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Bounded, lifecycle-fenced host authority rendered to the Assisted Permissions judge. Source-specific meaning and scope remain opaque data interpreted by the judge. The source must match the enclosing update source. At most 8 statements of up to 1,024 characters each (4,096 total), and at most 8 user messages of 16,384 characters in total; a grant needs at least one statement or user message. Scope follows the assisted-approval context JSON bounds. A session holds at most 8 active sources and 32 KiB of rendered grants, not counting user messages, and at most one active source may quote user messages.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthenticatedAuthorityGrant(
    /** Stable identifier for the trusted authority source; must match the update source. */
    @JsonProperty("source") String source,
    /** Stable host-issued identifier for this authority. */
    @JsonProperty("grantId") String grantId,
    /** RFC 3339 expiry timestamp. */
    @JsonProperty("expiresAt") String expiresAt,
    /** Bounded human-readable authority statements rendered to the judge. May be an empty array, never null, only when `userMessages` is not empty; a grant without statements adds no grant record to the judge's system prompt, so its `scope` must then be an empty object. */
    @JsonProperty("statements") List<String> statements,
    /** Structured scope facts constraining the authority statements. Must be an empty object when `statements` is empty. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("scope") Object scope,
    /** The user's own messages in another session, verbatim and oldest first, that the host authenticates as the user's, such as what the user wrote to the session that delegated work to this one. The judge transcript shows each as one of the user's own messages, written in another session, before this session's own user messages. Never include text an agent, tool, or file wrote. Only one active source may quote user messages, because quotes from different sources have no order, and only while the session has no human messages of its own, which could be older or newer than the quotes; human messages the session receives later render after the quotes. Omit when the grant quotes no user messages. */
    @JsonProperty("userMessages") List<String> userMessages
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AuthenticatedAuthorityGrant {}

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
        private String source;
        private String grantId;
        private String expiresAt;
        private List<String> statements;
        private Object scope;
        private boolean $presence4;
        private List<String> userMessages;

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
         * Sets the {@code grantId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder grantId(String value) {
            this.grantId = value;
            return this;
        }

        /**
         * Sets the {@code expiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expiresAt(String value) {
            this.expiresAt = value;
            return this;
        }

        /**
         * Sets the {@code statements} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder statements(List<String> value) {
            this.statements = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(Object value) {
            this.scope = value;
            this.$presence4 = true;
            return this;
        }

        /**
         * Sets the {@code userMessages} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userMessages(List<String> value) {
            this.userMessages = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthenticatedAuthorityGrant build() {
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (grantId == null) throw new IllegalStateException("Missing required property: grantId");
            if (expiresAt == null) throw new IllegalStateException("Missing required property: expiresAt");
            if (statements == null) throw new IllegalStateException("Missing required property: statements");
            if (!$presence4) throw new IllegalStateException("Missing required property: scope");
            return new AuthenticatedAuthorityGrant(source, grantId, expiresAt, statements, scope, userMessages);
        }
    }
}
