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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedPermissionEvaluation(
    /** Original input operation associated with this verdict. */
    @JsonProperty("operation") ManagedPermissionOperation operation,
    /** Managed-policy decision only; never bypasses user permissions or other security controls. */
    @JsonProperty("verdict") ManagedPermissionVerdict verdict
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedPermissionEvaluation {}

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
        private ManagedPermissionOperation operation;
        private ManagedPermissionVerdict verdict;

        /**
         * Sets the {@code operation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder operation(ManagedPermissionOperation value) {
            this.operation = value;
            return this;
        }

        /**
         * Sets the {@code verdict} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder verdict(ManagedPermissionVerdict value) {
            this.verdict = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedPermissionEvaluation build() {
            if (operation == null) throw new IllegalStateException("Missing required property: operation");
            if (verdict == null) throw new IllegalStateException("Missing required property: verdict");
            return new ManagedPermissionEvaluation(operation, verdict);
        }
    }
}
