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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Host callback. The host owns workspace records, UI, approval routing and cleanup.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionManagementCreateParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The originating session; the host must not escalate its permissions. */
    @JsonProperty("parentSessionId") String parentSessionId,
    /** Runtime-allocated identity. The host must create a fresh session with this exact ID. */
    @JsonProperty("newSessionId") String newSessionId,
    /** The new session must use this workspace and configuration directory. */
    @JsonProperty("cwd") String cwd,
    /** Parent's resolved configuration directory, which the host must preserve. */
    @JsonProperty("configDir") String configDir,
    /** Must not be broadened by the creating host. */
    @JsonProperty("permissionMode") PermissionMode permissionMode,
    /** Effective model to apply when creating the child. */
    @JsonProperty("model") String model,
    /** Provider owning the effective model, when specified. */
    @JsonProperty("providerId") String providerId,
    /** Effective reasoning effort, when specified. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Effective model-supported context tier, when specified. */
    @JsonProperty("contextTier") ContextTier contextTier,
    /** The host must preserve the parent's current model restrictions. */
    @JsonProperty("allowedModels") List<String> allowedModels,
    /** Optional short name supplied for the independent session. */
    @JsonProperty("name") String name
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionManagementCreateParams {}

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
        private String parentSessionId;
        private String newSessionId;
        private String cwd;
        private String configDir;
        private PermissionMode permissionMode;
        private String model;
        private String providerId;
        private String reasoningEffort;
        private ContextTier contextTier;
        private List<String> allowedModels;
        private String name;

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
         * Sets the {@code parentSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder parentSessionId(String value) {
            this.parentSessionId = value;
            return this;
        }

        /**
         * Sets the {@code newSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder newSessionId(String value) {
            this.newSessionId = value;
            return this;
        }

        /**
         * Sets the {@code cwd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cwd(String value) {
            this.cwd = value;
            return this;
        }

        /**
         * Sets the {@code configDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder configDir(String value) {
            this.configDir = value;
            return this;
        }

        /**
         * Sets the {@code permissionMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder permissionMode(PermissionMode value) {
            this.permissionMode = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code providerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder providerId(String value) {
            this.providerId = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reasoningEffort(String value) {
            this.reasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code contextTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder contextTier(ContextTier value) {
            this.contextTier = value;
            return this;
        }

        /**
         * Sets the {@code allowedModels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder allowedModels(List<String> value) {
            this.allowedModels = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionManagementCreateParams build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (parentSessionId == null) throw new IllegalStateException("Missing required property: parentSessionId");
            if (newSessionId == null) throw new IllegalStateException("Missing required property: newSessionId");
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            if (configDir == null) throw new IllegalStateException("Missing required property: configDir");
            if (permissionMode == null) throw new IllegalStateException("Missing required property: permissionMode");
            return new SessionManagementCreateParams(sessionId, parentSessionId, newSessionId, cwd, configDir, permissionMode, model, providerId, reasoningEffort, contextTier, allowedModels, name);
        }
    }
}
