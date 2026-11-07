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
 * A memory the agent stored after validation, secret filtering and approval.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record MemoryStoreStoreParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Short topic the memory is about. */
    @JsonProperty("subject") String subject,
    /** The fact to remember. */
    @JsonProperty("fact") String fact,
    /** Sources supporting the fact, such as file paths with line numbers or a quotation of the user. */
    @JsonProperty("citations") String citations,
    /** Why the agent considers the fact worth remembering. */
    @JsonProperty("reason") String reason
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    MemoryStoreStoreParams {}

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
        private String subject;
        private String fact;
        private String citations;
        private String reason;

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
         * Sets the {@code subject} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder subject(String value) {
            this.subject = value;
            return this;
        }

        /**
         * Sets the {@code fact} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder fact(String value) {
            this.fact = value;
            return this;
        }

        /**
         * Sets the {@code citations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder citations(String value) {
            this.citations = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public MemoryStoreStoreParams build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (subject == null) throw new IllegalStateException("Missing required property: subject");
            if (fact == null) throw new IllegalStateException("Missing required property: fact");
            if (citations == null) throw new IllegalStateException("Missing required property: citations");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            return new MemoryStoreStoreParams(sessionId, subject, fact, citations, reason);
        }
    }
}
