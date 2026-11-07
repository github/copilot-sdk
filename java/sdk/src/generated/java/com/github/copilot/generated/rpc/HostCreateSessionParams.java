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
 * One application-owned session handoff, requested by the supervised hosting participant.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HostCreateSessionParams(
    /** Trusted admission identity supplied by the hosting participant. Required by the runtime before a handoff is admitted. */
    @JsonProperty("authentication") Object authentication,
    /** Unique identity for this participation, independent of the session lifetime. */
    @JsonProperty("handoffId") String handoffId,
    /** Resume an app-owned durable session instead of creating a new session. */
    @JsonProperty("resume") Boolean resume,
    /** For an explicit publication, borrow an existing owning-connection resident
instead of invoking the resume factory. Cold sessions still use the factory. */
    @JsonProperty("preferResident") Boolean preferResident,
    /** Host-selected SDK creation or resume settings, without executable callbacks or tools. */
    @JsonProperty("config") Map<String, Object> config
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    HostCreateSessionParams {}

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
        private String handoffId;
        private Boolean resume;
        private Boolean preferResident;
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
         * Sets the {@code preferResident} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder preferResident(Boolean value) {
            this.preferResident = value;
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
        public HostCreateSessionParams build() {
            if (handoffId == null) throw new IllegalStateException("Missing required property: handoffId");
            if (config == null) throw new IllegalStateException("Missing required property: config");
            return new HostCreateSessionParams(authentication, handoffId, resume, preferResident, config);
        }
    }
}
