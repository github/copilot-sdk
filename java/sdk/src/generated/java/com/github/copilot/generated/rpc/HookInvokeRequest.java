/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Runtime-owned wire payload for a server-to-client hook callback invocation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HookInvokeRequest(
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("hookType") HookType hookType,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("input") Object input
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    HookInvokeRequest {}

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
        private String sessionId;
        private HookType hookType;
        private Object input;
        private boolean $presence2;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code hookType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hookType(HookType value) {
            this.hookType = value;
            return this;
        }

        /**
         * Sets the {@code input} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder input(Object value) {
            this.input = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HookInvokeRequest build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (hookType == null) throw new IllegalStateException("Missing required property: hookType");
            if (!$presence2) throw new IllegalStateException("Missing required property: input");
            return new HookInvokeRequest(sessionId, hookType, input);
        }
    }
}
