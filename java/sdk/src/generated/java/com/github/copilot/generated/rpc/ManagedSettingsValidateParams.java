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
 * A candidate managed-settings document to validate without applying it.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsValidateParams(
    /** The document to validate: a JSON object, or a string containing the document's JSON text. Preview documents are limited to 1 MiB and 64 levels of nesting, a stricter resource limit than delivered-policy parsing; violations are returned as diagnostics. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("content") Object content,
    /** Channel the document is meant for (`device`, `server`, or `policyHelper`). Some keys are only honored in some channels; for example, a `policyHelper` registration is ignored in policy-helper output. When omitted, no channel-specific checks run. */
    @JsonProperty("layer") String layer
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsValidateParams {}

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
        private Object content;
        private boolean $presence0;
        private String layer;

        /**
         * Sets the {@code content} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder content(Object value) {
            this.content = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code layer} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder layer(String value) {
            this.layer = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsValidateParams build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: content");
            return new ManagedSettingsValidateParams(content, layer);
        }
    }
}
