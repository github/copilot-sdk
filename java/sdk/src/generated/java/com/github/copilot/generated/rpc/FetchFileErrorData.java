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
 * JSON-RPC error.data for file creation, writes, completion, or incomplete-file cleanup failures (-32603).
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FetchFileErrorData(
    /** Always fileSystem; these errors do not describe an HTTP transport failure. */
    @JsonProperty("kind") FetchFileErrorKind kind,
    /** The failed filesystem operation. */
    @JsonProperty("operation") FetchFileOperation operation,
    /** Platform error number, when supplied by the operating system. */
    @JsonProperty("osErrorCode") Long osErrorCode
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FetchFileErrorData {}

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
        private FetchFileErrorKind kind;
        private FetchFileOperation operation;
        private Long osErrorCode;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(FetchFileErrorKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code operation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder operation(FetchFileOperation value) {
            this.operation = value;
            return this;
        }

        /**
         * Sets the {@code osErrorCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder osErrorCode(Long value) {
            this.osErrorCode = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FetchFileErrorData build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (operation == null) throw new IllegalStateException("Missing required property: operation");
            return new FetchFileErrorData(kind, operation, osErrorCode);
        }
    }
}
