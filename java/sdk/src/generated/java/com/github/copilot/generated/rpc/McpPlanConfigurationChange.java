/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The configuration-change alternative for the transportChoices entry at the same index. Only the selected alternative is applied; entries are not cumulative. The payload stays behind the runtime boundary.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanConfigurationChange(
    /** Whether the change would create a new entry or modify an existing one. */
    @JsonProperty("operation") McpPlanConfigurationOperation operation,
    /** Scope the change would be written to. */
    @JsonProperty("scope") McpPlanScope scope,
    /** Configuration key the change applies to. */
    @JsonProperty("configKey") String configKey,
    /** Names of the configuration fields the change would set, without their values. */
    @JsonProperty("changedFields") List<String> changedFields,
    /** Secret placeholders the written configuration would reference. The constrained placeholder type cannot carry a literal secret value. */
    @JsonProperty("secretReferences") List<String> secretReferences
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPlanConfigurationChange {}

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
        private McpPlanConfigurationOperation operation;
        private McpPlanScope scope;
        private String configKey;
        private List<String> changedFields;
        private List<String> secretReferences;

        /**
         * Sets the {@code operation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder operation(McpPlanConfigurationOperation value) {
            this.operation = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(McpPlanScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code configKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configKey(String value) {
            this.configKey = value;
            return this;
        }

        /**
         * Sets the {@code changedFields} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder changedFields(List<String> value) {
            this.changedFields = value;
            return this;
        }

        /**
         * Sets the {@code secretReferences} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder secretReferences(List<String> value) {
            this.secretReferences = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpPlanConfigurationChange build() {
            if (operation == null) throw new IllegalStateException("Missing required property: operation");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (configKey == null) throw new IllegalStateException("Missing required property: configKey");
            if (changedFields == null) throw new IllegalStateException("Missing required property: changedFields");
            if (secretReferences == null) throw new IllegalStateException("Missing required property: secretReferences");
            return new McpPlanConfigurationChange(operation, scope, configKey, changedFields, secretReferences);
        }
    }
}
