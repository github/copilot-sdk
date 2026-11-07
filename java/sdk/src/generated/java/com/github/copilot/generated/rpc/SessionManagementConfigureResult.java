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
 * Opt-in policy for local session management.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionManagementConfigureResult(
    /** Master switch. Off by default. */
    @JsonProperty("enabled") Boolean enabled,
    /** Discovery and direct access boundary. Defaults to all eligible local peers. */
    @JsonProperty("scope") SessionManagementScope scope,
    /** Allow listing opted-in sessions in this configuration directory. */
    @JsonProperty("list") Boolean list,
    /** Allow requesting a new session from the owning host. */
    @JsonProperty("create") Boolean create,
    /** Allow sending a peer message to another opted-in session. */
    @JsonProperty("message") Boolean message,
    /** Allow closing sessions created by this session or its descendants. On by default. */
    @JsonProperty("close") Boolean close
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionManagementConfigureResult {}

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
        private Boolean enabled;
        private SessionManagementScope scope;
        private Boolean list;
        private Boolean create;
        private Boolean message;
        private Boolean close;

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enabled(Boolean value) {
            this.enabled = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder scope(SessionManagementScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code list} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder list(Boolean value) {
            this.list = value;
            return this;
        }

        /**
         * Sets the {@code create} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder create(Boolean value) {
            this.create = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder message(Boolean value) {
            this.message = value;
            return this;
        }

        /**
         * Sets the {@code close} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder close(Boolean value) {
            this.close = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionManagementConfigureResult build() {
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (list == null) throw new IllegalStateException("Missing required property: list");
            if (create == null) throw new IllegalStateException("Missing required property: create");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            if (close == null) throw new IllegalStateException("Missing required property: close");
            return new SessionManagementConfigureResult(enabled, scope, list, create, message, close);
        }
    }
}
