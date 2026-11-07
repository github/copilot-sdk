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
 * The management request was refused.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record SkillInstallationManagementOutcomeRefused(
    /** Bounded refusal reason. */
    @JsonProperty("reason") SkillInstallationFailureReason reason,
    /** Whole seconds GitHub asked the caller to wait, present only with source-rate-limited. */
    @JsonProperty("retryAfterSeconds") Long retryAfterSeconds,
    /** Skill installation management outcome discriminator. */
    @JsonProperty("kind") String kind
) implements SkillInstallationManagementOutcome {
    /**
     * Creates this variant positionally and normalizes its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillInstallationManagementOutcomeRefused {
        kind = "refused";
    }

    /**
     * Creates this variant without explicitly supplying its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillInstallationManagementOutcomeRefused(
        SkillInstallationFailureReason reason,
        Long retryAfterSeconds
    ) {
        this(reason, retryAfterSeconds, "refused");
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
        private SkillInstallationFailureReason reason;
        private Long retryAfterSeconds;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(SkillInstallationFailureReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code retryAfterSeconds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder retryAfterSeconds(Long value) {
            this.retryAfterSeconds = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillInstallationManagementOutcomeRefused build() {
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            return new SkillInstallationManagementOutcomeRefused(reason, retryAfterSeconds, "refused");
        }
    }
}
