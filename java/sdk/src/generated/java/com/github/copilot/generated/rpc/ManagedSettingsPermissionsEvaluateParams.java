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
import com.github.copilot.generated.ManagedPermissionsContext;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Managed policy context and ordered operations for pure, sessionless evaluation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsPermissionsEvaluateParams(
    /** Trusted, reusable managed permission policy snapshot; no policy is discovered during evaluation. */
    @JsonProperty("context") ManagedPermissionsContext context,
    /** Operations to evaluate, preserving input order and duplicates. An empty batch is valid. */
    @JsonProperty("operations") List<ManagedPermissionOperation> operations
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsPermissionsEvaluateParams {}

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
        private ManagedPermissionsContext context;
        private List<ManagedPermissionOperation> operations;

        /**
         * Sets the {@code context} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder context(ManagedPermissionsContext value) {
            this.context = value;
            return this;
        }

        /**
         * Sets the {@code operations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder operations(List<ManagedPermissionOperation> value) {
            this.operations = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsPermissionsEvaluateParams build() {
            if (context == null) throw new IllegalStateException("Missing required property: context");
            if (operations == null) throw new IllegalStateException("Missing required property: operations");
            return new ManagedSettingsPermissionsEvaluateParams(context, operations);
        }
    }
}
