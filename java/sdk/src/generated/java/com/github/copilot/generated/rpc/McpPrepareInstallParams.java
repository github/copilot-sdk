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
 * Side-effect-free preparation of one original bound remote MCP choice.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPrepareInstallParams(
    /** Required bound catalogue and confirmed remote installation capabilities. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Original single-use bound plan, never a client-authored configuration. */
    @JsonProperty("planHandle") String planHandle,
    /** Exact selected alternative from that plan. */
    @JsonProperty("choiceId") String choiceId,
    /** An existing local session attached to this connection, not permission to attach one. */
    @JsonProperty("policySessionId") String policySessionId,
    /** Declared non-secret values. Non-empty only when the caller requires
`mcp-configured-remote-installation`; omitted values use the card default. */
    @JsonProperty("inputs") List<McpInstallationInput> inputs,
    /** One entry per declared secret placeholder of the selected choice. Non-empty
only when the caller requires `mcp-configured-remote-installation`. */
    @JsonProperty("secrets") List<McpInstallationSecret> secrets,
    /** The exact original source, used transiently only after confirmation. */
    @JsonProperty("source") Object source,
    /** The trusted host presents this choice alongside the exact secret placeholders. */
    @JsonProperty("secretStorage") McpInstallationSecretStorage secretStorage
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPrepareInstallParams {}

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
        private CatalogClientContract contract;
        private String planHandle;
        private String choiceId;
        private String policySessionId;
        private List<McpInstallationInput> inputs;
        private List<McpInstallationSecret> secrets;
        private Object source;
        private McpInstallationSecretStorage secretStorage;

        /**
         * Sets the {@code contract} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contract(CatalogClientContract value) {
            this.contract = value;
            return this;
        }

        /**
         * Sets the {@code planHandle} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planHandle(String value) {
            this.planHandle = value;
            return this;
        }

        /**
         * Sets the {@code choiceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder choiceId(String value) {
            this.choiceId = value;
            return this;
        }

        /**
         * Sets the {@code policySessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policySessionId(String value) {
            this.policySessionId = value;
            return this;
        }

        /**
         * Sets the {@code inputs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inputs(List<McpInstallationInput> value) {
            this.inputs = value;
            return this;
        }

        /**
         * Sets the {@code secrets} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder secrets(List<McpInstallationSecret> value) {
            this.secrets = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(Object value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code secretStorage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder secretStorage(McpInstallationSecretStorage value) {
            this.secretStorage = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpPrepareInstallParams build() {
            if (contract == null) throw new IllegalStateException("Missing required property: contract");
            if (planHandle == null) throw new IllegalStateException("Missing required property: planHandle");
            if (choiceId == null) throw new IllegalStateException("Missing required property: choiceId");
            if (policySessionId == null) throw new IllegalStateException("Missing required property: policySessionId");
            if (inputs == null) throw new IllegalStateException("Missing required property: inputs");
            if (secrets == null) throw new IllegalStateException("Missing required property: secrets");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (secretStorage == null) throw new IllegalStateException("Missing required property: secretStorage");
            return new McpPrepareInstallParams(contract, planHandle, choiceId, policySessionId, inputs, secrets, source, secretStorage);
        }
    }
}
