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
 * Endpoint URLs from the raw Copilot `/copilot_internal/v2/token` user-response passthrough.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CopilotUserResponseEndpoints(
    /** Copilot API endpoint URL. */
    @JsonProperty("api") String api,
    /** Origin-tracker endpoint URL. */
    @JsonProperty("origin-tracker") String originTracker,
    /** Copilot proxy endpoint URL. */
    @JsonProperty("proxy") String proxy,
    /** Copilot telemetry endpoint URL. */
    @JsonProperty("telemetry") String telemetry,
    /** Experimental-service endpoint URL. */
    @JsonProperty("exp") String exp
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CopilotUserResponseEndpoints {}

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
        private String api;
        private String originTracker;
        private String proxy;
        private String telemetry;
        private String exp;

        /**
         * Sets the {@code api} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder api(String value) {
            this.api = value;
            return this;
        }

        /**
         * Sets the {@code origin-tracker} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder originTracker(String value) {
            this.originTracker = value;
            return this;
        }

        /**
         * Sets the {@code proxy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder proxy(String value) {
            this.proxy = value;
            return this;
        }

        /**
         * Sets the {@code telemetry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder telemetry(String value) {
            this.telemetry = value;
            return this;
        }

        /**
         * Sets the {@code exp} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder exp(String value) {
            this.exp = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CopilotUserResponseEndpoints build() {
            return new CopilotUserResponseEndpoints(api, originTracker, proxy, telemetry, exp);
        }
    }
}
