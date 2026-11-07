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
 * A computed Skill uninstall plan. Nothing has been removed.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillUninstallPlan(
    /** One-use uninstall plan handle. */
    @JsonProperty("planHandle") String planHandle,
    /** Original removal operation identifier returned before confirmation. */
    @JsonProperty("operationId") String operationId,
    /** Original wall-clock expiry as an ISO 8601 timestamp. */
    @JsonProperty("expiresAt") String expiresAt,
    /** Owned Skill installation being removed. */
    @JsonProperty("installation") SkillInstallationSummary installation,
    /** Safe review to present before applying removal. */
    @JsonProperty("review") SkillInstallationReview review
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillUninstallPlan {}

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
        private String planHandle;
        private String operationId;
        private String expiresAt;
        private SkillInstallationSummary installation;
        private SkillInstallationReview review;

        /**
         * Sets the {@code planHandle} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planHandle(String value) {
            this.planHandle = value;
            return this;
        }

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
         * Sets the {@code expiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expiresAt(String value) {
            this.expiresAt = value;
            return this;
        }

        /**
         * Sets the {@code installation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installation(SkillInstallationSummary value) {
            this.installation = value;
            return this;
        }

        /**
         * Sets the {@code review} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder review(SkillInstallationReview value) {
            this.review = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillUninstallPlan build() {
            if (planHandle == null) throw new IllegalStateException("Missing required property: planHandle");
            if (operationId == null) throw new IllegalStateException("Missing required property: operationId");
            if (expiresAt == null) throw new IllegalStateException("Missing required property: expiresAt");
            if (installation == null) throw new IllegalStateException("Missing required property: installation");
            if (review == null) throw new IllegalStateException("Missing required property: review");
            return new SkillUninstallPlan(planHandle, operationId, expiresAt, installation, review);
        }
    }
}
