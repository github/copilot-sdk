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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Application callback routed over its existing SDK connection.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HostSessionCreateCallback(
    /** Trusted admission identity, separate from inference credentials. Always supplied by the runtime for admitted handoffs. */
    @JsonProperty("authentication") Object authentication,
    /** Listener UUID identifying the owning application's host. */
    @JsonProperty("hostId") String hostId,
    /** Unique identity of the session participation being requested. */
    @JsonProperty("handoffId") String handoffId,
    /** Resume an app-owned durable session instead of creating a new session. */
    @JsonProperty("resume") Boolean resume,
    /** Host-selected SDK creation or resume settings, without executable callbacks or tools. */
    @JsonProperty("config") Map<String, Object> config
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    HostSessionCreateCallback {}

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
        private Object authentication;
        private String hostId;
        private String handoffId;
        private Boolean resume;
        private Map<String, Object> config;

        /**
         * Sets the {@code authentication} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authentication(Object value) {
            this.authentication = value;
            return this;
        }

        /**
         * Sets the {@code hostId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hostId(String value) {
            this.hostId = value;
            return this;
        }

        /**
         * Sets the {@code handoffId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder handoffId(String value) {
            this.handoffId = value;
            return this;
        }

        /**
         * Sets the {@code resume} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resume(Boolean value) {
            this.resume = value;
            return this;
        }

        /**
         * Sets the {@code config} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder config(Map<String, Object> value) {
            this.config = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostSessionCreateCallback build() {
            if (hostId == null) throw new IllegalStateException("Missing required property: hostId");
            if (handoffId == null) throw new IllegalStateException("Missing required property: handoffId");
            if (config == null) throw new IllegalStateException("Missing required property: config");
            return new HostSessionCreateCallback(authentication, hostId, handoffId, resume, config);
        }
    }
}
