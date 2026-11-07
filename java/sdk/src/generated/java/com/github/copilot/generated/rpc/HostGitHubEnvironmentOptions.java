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
 * GitHub Mission Control registration options. The compute ID is application-owned and stable.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostGitHubEnvironmentOptions(
    /** Human-readable environment display name. */
    @JsonProperty("name") String name,
    /** Stable application installation identity, reused across host restarts. */
    @JsonProperty("computeId") String computeId,
    /** Require sealed, connection-bound authentication (default true), independent of listener address. False permits unsealed direct credentials and unbound sealed relay credentials; use only when the caller fully controls and trusts the direct transport. Relay token encryption remains mandatory. */
    @JsonProperty("requireConnectionBinding") Boolean requireConnectionBinding
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HostGitHubEnvironmentOptions {}

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
        private String name;
        private String computeId;
        private Boolean requireConnectionBinding;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code computeId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder computeId(String value) {
            this.computeId = value;
            return this;
        }

        /**
         * Sets the {@code requireConnectionBinding} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requireConnectionBinding(Boolean value) {
            this.requireConnectionBinding = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostGitHubEnvironmentOptions build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (computeId == null) throw new IllegalStateException("Missing required property: computeId");
            return new HostGitHubEnvironmentOptions(name, computeId, requireConnectionBinding);
        }
    }

    /**
     * Creates environment options using the default connection-binding policy.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Human-readable environment display name.
     * @param computeId Stable application installation identity, reused across host restarts.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HostGitHubEnvironmentOptions(
        String name,
        String computeId
    ) {
        this(name, computeId, null);
    }
}
