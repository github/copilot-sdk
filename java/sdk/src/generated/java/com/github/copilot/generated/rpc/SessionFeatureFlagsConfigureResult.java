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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Current service state, without configuration documents or credentials.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFeatureFlagsConfigureResult(
    /** Decimal instance identity, without integer precision loss. */
    @JsonProperty("serviceId") String serviceId,
    /** Service revision, with unsigned 32-bit wraparound. */
    @JsonProperty("revision") Long revision,
    /** Whether the service remains active. */
    @JsonProperty("active") Boolean active,
    /** Whether the current audience assignment has resolved. */
    @JsonProperty("expResponseResolved") Boolean expResponseResolved,
    /** Static flags after availability, environment, settings, and explicit overrides. */
    @JsonProperty("flags") Map<String, Boolean> flags,
    /** Effective experiment values. */
    @JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.ALWAYS)
    @JsonProperty("expFlags") Map<String, Object> expFlags,
    /** Original assignment document, including its optional fields. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("response") Object response,
    /** Latest nonempty assignment context. */
    @JsonProperty("assignmentContext") String assignmentContext,
    /** Latest nonempty secondary assignment context. */
    @JsonProperty("secondaryAssignmentContext") String secondaryAssignmentContext,
    /** Organization targeting derived from the authenticated account. */
    @JsonProperty("autoApprovalTargeted") Boolean autoApprovalTargeted,
    /** Original runtime interpretation of the reasoning-summary treatment. */
    @JsonProperty("reasoningSummariesArm") FeatureFlagReasoningSummariesArm reasoningSummariesArm,
    /** Original runtime interpretation of the GPT default-model treatment. */
    @JsonProperty("gptDefaultModelEnabled") Boolean gptDefaultModelEnabled,
    /** Validated instruction-retrieval arm, when present. */
    @JsonProperty("dynamicInstructionsRetrievalArm") String dynamicInstructionsRetrievalArm
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFeatureFlagsConfigureResult {}

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
        private String serviceId;
        private Long revision;
        private Boolean active;
        private Boolean expResponseResolved;
        private Map<String, Boolean> flags;
        private Map<String, Object> expFlags;
        private Object response;
        private boolean $presence6;
        private String assignmentContext;
        private String secondaryAssignmentContext;
        private Boolean autoApprovalTargeted;
        private FeatureFlagReasoningSummariesArm reasoningSummariesArm;
        private Boolean gptDefaultModelEnabled;
        private String dynamicInstructionsRetrievalArm;

        /**
         * Sets the {@code serviceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder serviceId(String value) {
            this.serviceId = value;
            return this;
        }

        /**
         * Sets the {@code revision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder revision(Long value) {
            this.revision = value;
            return this;
        }

        /**
         * Sets the {@code active} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder active(Boolean value) {
            this.active = value;
            return this;
        }

        /**
         * Sets the {@code expResponseResolved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder expResponseResolved(Boolean value) {
            this.expResponseResolved = value;
            return this;
        }

        /**
         * Sets the {@code flags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder flags(Map<String, Boolean> value) {
            this.flags = value;
            return this;
        }

        /**
         * Sets the {@code expFlags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder expFlags(Map<String, Object> value) {
            this.expFlags = value;
            return this;
        }

        /**
         * Sets the {@code response} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder response(Object value) {
            this.response = value;
            this.$presence6 = true;
            return this;
        }

        /**
         * Sets the {@code assignmentContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder assignmentContext(String value) {
            this.assignmentContext = value;
            return this;
        }

        /**
         * Sets the {@code secondaryAssignmentContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder secondaryAssignmentContext(String value) {
            this.secondaryAssignmentContext = value;
            return this;
        }

        /**
         * Sets the {@code autoApprovalTargeted} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder autoApprovalTargeted(Boolean value) {
            this.autoApprovalTargeted = value;
            return this;
        }

        /**
         * Sets the {@code reasoningSummariesArm} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reasoningSummariesArm(FeatureFlagReasoningSummariesArm value) {
            this.reasoningSummariesArm = value;
            return this;
        }

        /**
         * Sets the {@code gptDefaultModelEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder gptDefaultModelEnabled(Boolean value) {
            this.gptDefaultModelEnabled = value;
            return this;
        }

        /**
         * Sets the {@code dynamicInstructionsRetrievalArm} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder dynamicInstructionsRetrievalArm(String value) {
            this.dynamicInstructionsRetrievalArm = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFeatureFlagsConfigureResult build() {
            if (serviceId == null) throw new IllegalStateException("Missing required property: serviceId");
            if (revision == null) throw new IllegalStateException("Missing required property: revision");
            if (active == null) throw new IllegalStateException("Missing required property: active");
            if (expResponseResolved == null) throw new IllegalStateException("Missing required property: expResponseResolved");
            if (flags == null) throw new IllegalStateException("Missing required property: flags");
            if (expFlags == null) throw new IllegalStateException("Missing required property: expFlags");
            if (!$presence6) throw new IllegalStateException("Missing required property: response");
            if (autoApprovalTargeted == null) throw new IllegalStateException("Missing required property: autoApprovalTargeted");
            if (reasoningSummariesArm == null) throw new IllegalStateException("Missing required property: reasoningSummariesArm");
            if (gptDefaultModelEnabled == null) throw new IllegalStateException("Missing required property: gptDefaultModelEnabled");
            return new SessionFeatureFlagsConfigureResult(serviceId, revision, active, expResponseResolved, flags, expFlags, response, assignmentContext, secondaryAssignmentContext, autoApprovalTargeted, reasoningSummariesArm, gptDefaultModelEnabled, dynamicInstructionsRetrievalArm);
        }
    }
}
