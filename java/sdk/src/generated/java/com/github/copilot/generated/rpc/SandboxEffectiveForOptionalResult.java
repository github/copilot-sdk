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
 * The sandbox a named feature actually launches under.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxEffectiveForOptionalResult(
    /** The effective sandbox config for the requested feature, or a disabled sandbox when the feature is routed out of it. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("sandboxJson") Object sandboxJson
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxEffectiveForOptionalResult {}

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
        private Object sandboxJson;
        private boolean $presence0;

        /**
         * Sets the {@code sandboxJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxJson(Object value) {
            this.sandboxJson = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxEffectiveForOptionalResult build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: sandboxJson");
            return new SandboxEffectiveForOptionalResult(sandboxJson);
        }
    }
}
