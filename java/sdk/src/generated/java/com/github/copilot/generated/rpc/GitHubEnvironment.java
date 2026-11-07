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
 * Safe discovery information. Host-side relay bootstrap credentials are never included.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubEnvironment(
    /** Identifier assigned by Mission Control. */
    @JsonProperty("id") String id,
    /** Human-readable environment name. */
    @JsonProperty("name") String name,
    /** Compute kind reported by Mission Control. */
    @JsonProperty("kind") EnvironmentKind kind,
    /** Open-ended operational status vocabulary. */
    @JsonProperty("status") String status,
    /** Hosting capabilities advertised by the environment. */
    @JsonProperty("capabilities") EnvironmentCapabilities capabilities,
    /** Identifier of the environment owner. */
    @JsonProperty("ownerId") String ownerId,
    /** Owner category reported by Mission Control. */
    @JsonProperty("ownerType") String ownerType,
    /** Organization identifier, when the environment belongs to an organization. */
    @JsonProperty("orgId") String orgId,
    /** Discovery labels attached to the environment. */
    @JsonProperty("labels") Map<String, String> labels,
    /** Timestamp of the last heartbeat received by Mission Control. */
    @JsonProperty("lastHeartbeatAt") String lastHeartbeatAt
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public GitHubEnvironment {}

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
        private String id;
        private String name;
        private EnvironmentKind kind;
        private String status;
        private EnvironmentCapabilities capabilities;
        private String ownerId;
        private String ownerType;
        private String orgId;
        private Map<String, String> labels;
        private String lastHeartbeatAt;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder id(String value) {
            this.id = value;
            return this;
        }

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
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(EnvironmentKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(String value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code capabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder capabilities(EnvironmentCapabilities value) {
            this.capabilities = value;
            return this;
        }

        /**
         * Sets the {@code ownerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ownerId(String value) {
            this.ownerId = value;
            return this;
        }

        /**
         * Sets the {@code ownerType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ownerType(String value) {
            this.ownerType = value;
            return this;
        }

        /**
         * Sets the {@code orgId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder orgId(String value) {
            this.orgId = value;
            return this;
        }

        /**
         * Sets the {@code labels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder labels(Map<String, String> value) {
            this.labels = value;
            return this;
        }

        /**
         * Sets the {@code lastHeartbeatAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder lastHeartbeatAt(String value) {
            this.lastHeartbeatAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubEnvironment build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new GitHubEnvironment(id, name, kind, status, capabilities, ownerId, ownerType, orgId, labels, lastHeartbeatAt);
        }
    }
}
