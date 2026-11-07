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
 * The authoring JSON schema for managed settings recognized by this runtime.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsSchemaResult(
    /** JSON schema (draft 2020-12) with descriptive shared `x-composition` annotations, not a complete runtime composition contract. Model, effortLevel, and contextTier remain coupled; use `managedSettings.compose` for the runtime's effective result. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("schema") Object schema,
    /** Version of the runtime that owns this schema. */
    @JsonProperty("runtimeVersion") String runtimeVersion
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsSchemaResult {}

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
        private Object schema;
        private boolean $presence0;
        private String runtimeVersion;

        /**
         * Sets the {@code schema} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder schema(Object value) {
            this.schema = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code runtimeVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder runtimeVersion(String value) {
            this.runtimeVersion = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsSchemaResult build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: schema");
            if (runtimeVersion == null) throw new IllegalStateException("Missing required property: runtimeVersion");
            return new ManagedSettingsSchemaResult(schema, runtimeVersion);
        }
    }
}
