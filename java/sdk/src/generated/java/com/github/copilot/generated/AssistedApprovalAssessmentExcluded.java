/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * A deterministic runtime or policy gate prevented automatic approval without consulting the judge.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistedApprovalAssessmentExcluded extends AssistedApprovalAssessment {

    @JsonProperty("status")
    private final String status = "excluded";

    @Override
    public String getStatus() { return status; }

    /** Machine-readable exclusion reason. Current runtimes emit `managed-approval-required`, `sandbox-bypass`, `extension-permission-access`, `action-too-long`, `authorization-history-incomplete`, `path-not-authorized`, or `content-excluded`; consumers must tolerate future strings. */
    @JsonProperty("reason")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }


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
        private String reason;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AssistedApprovalAssessmentExcluded build() {
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            AssistedApprovalAssessmentExcluded instance = new AssistedApprovalAssessmentExcluded();
            instance.setReason(reason);
            return instance;
        }
    }
}
