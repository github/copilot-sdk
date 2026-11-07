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
 * Inputs to spawn a managed-server child via the controller's spawn delegate.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentRegistrySpawnParams(
    /** Working directory for the spawned child (must be an existing directory) */
    @JsonProperty("cwd") String cwd,
    /** Custom or built-in agent name (e.g. 'explore'). When omitted, the child uses its own default. */
    @JsonProperty("agentName") String agentName,
    /** Model identifier to apply to the new session */
    @JsonProperty("model") String model,
    /** Friendly session name. Must satisfy validateSessionName: non-empty, no leading/trailing whitespace, <=100 chars, no control chars, no double quotes. */
    @JsonProperty("name") String name,
    /** Permission posture for the new session. 'yolo' requires the controller-local session to currently be in allow-all mode. */
    @JsonProperty("permissionMode") AgentRegistrySpawnPermissionMode permissionMode,
    /** Optional first user message. Forwarded to the caller (the CLI's spawn wrapper sends it post-attach via the standard LocalRpcSession.send path). */
    @JsonProperty("initialPrompt") String initialPrompt
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AgentRegistrySpawnParams {}

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
        private String cwd;
        private String agentName;
        private String model;
        private String name;
        private AgentRegistrySpawnPermissionMode permissionMode;
        private String initialPrompt;

        /**
         * Sets the {@code cwd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cwd(String value) {
            this.cwd = value;
            return this;
        }

        /**
         * Sets the {@code agentName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentName(String value) {
            this.agentName = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code permissionMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder permissionMode(AgentRegistrySpawnPermissionMode value) {
            this.permissionMode = value;
            return this;
        }

        /**
         * Sets the {@code initialPrompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder initialPrompt(String value) {
            this.initialPrompt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentRegistrySpawnParams build() {
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            return new AgentRegistrySpawnParams(cwd, agentName, model, name, permissionMode, initialPrompt);
        }
    }
}
