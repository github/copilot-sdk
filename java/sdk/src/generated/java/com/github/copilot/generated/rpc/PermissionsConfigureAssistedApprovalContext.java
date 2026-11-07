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
 * One SDK-host source's bounded JSON value for future Assisted Permissions judge requests. Replaces only the named source; a null value clears that source.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionsConfigureAssistedApprovalContext(
    /** Stable host source identifier. It isolates this source from unrelated producers and may contain ASCII letters, digits, dots, underscores, and hyphens. */
    @JsonProperty("source") String source,
    /** Arbitrary bounded JSON context. A null value clears only this source. Runtime framing states that this value is not an instruction, policy decision, human authorization, or permission. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("value") Object value
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PermissionsConfigureAssistedApprovalContext {}

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
        private String source;
        private Object value;
        private boolean $presence1;

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code value} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder value(Object value) {
            this.value = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PermissionsConfigureAssistedApprovalContext build() {
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (!$presence1) throw new IllegalStateException("Missing required property: value");
            return new PermissionsConfigureAssistedApprovalContext(source, value);
        }
    }
}
