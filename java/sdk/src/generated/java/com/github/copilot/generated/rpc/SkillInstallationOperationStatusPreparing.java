/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Original operation progress discriminator.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record SkillInstallationOperationStatusPreparing(
    /** Original runtime-issued operation identity. */
    @JsonProperty("operationId") String operationId,
    /** Whether cancellation has been requested. */
    @JsonProperty("cancellationRequested") Boolean cancellationRequested,
    @JsonProperty("phase") String phase
) implements SkillInstallationOperationStatus {
    /**
     * Creates this variant positionally and normalizes its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillInstallationOperationStatusPreparing {
        phase = "preparing";
    }

    /**
     * Creates this variant without explicitly supplying its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillInstallationOperationStatusPreparing(
        String operationId,
        Boolean cancellationRequested
    ) {
        this(operationId, cancellationRequested, "preparing");
    }

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
        private String operationId;
        private Boolean cancellationRequested;

        /**
         * Sets the {@code operationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder operationId(String value) {
            this.operationId = value;
            return this;
        }

        /**
         * Sets the {@code cancellationRequested} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cancellationRequested(Boolean value) {
            this.cancellationRequested = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillInstallationOperationStatusPreparing build() {
            if (operationId == null) throw new IllegalStateException("Missing required property: operationId");
            if (cancellationRequested == null) throw new IllegalStateException("Missing required property: cancellationRequested");
            return new SkillInstallationOperationStatusPreparing(operationId, cancellationRequested, "preparing");
        }
    }
}
