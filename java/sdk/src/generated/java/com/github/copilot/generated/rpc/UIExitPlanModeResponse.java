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
 * User response for a pending exit-plan-mode request, with approval state, selected action, auto-approve flag, and feedback.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UIExitPlanModeResponse(
    /** Whether the plan was approved. */
    @JsonProperty("approved") Boolean approved,
    /** The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'. */
    @JsonProperty("selectedAction") UIExitPlanModeAction selectedAction,
    /** Whether subsequent edits should be auto-approved without confirmation. */
    @JsonProperty("autoApproveEdits") Boolean autoApproveEdits,
    /** Feedback from the user when they declined the plan or requested changes. */
    @JsonProperty("feedback") String feedback,
    /** When true, the agent is instructed to end its turn without starting implementation so the client can restore the session model and auto-submit a fresh implementation turn on it. Set only when a distinct plan configuration (a different model, reasoning effort, or context tier) actually ran the planning turn. */
    @JsonProperty("deferImplementation") Boolean deferImplementation
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public UIExitPlanModeResponse {}

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
        private Boolean approved;
        private UIExitPlanModeAction selectedAction;
        private Boolean autoApproveEdits;
        private String feedback;
        private Boolean deferImplementation;

        /**
         * Sets the {@code approved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder approved(Boolean value) {
            this.approved = value;
            return this;
        }

        /**
         * Sets the {@code selectedAction} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectedAction(UIExitPlanModeAction value) {
            this.selectedAction = value;
            return this;
        }

        /**
         * Sets the {@code autoApproveEdits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoApproveEdits(Boolean value) {
            this.autoApproveEdits = value;
            return this;
        }

        /**
         * Sets the {@code feedback} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder feedback(String value) {
            this.feedback = value;
            return this;
        }

        /**
         * Sets the {@code deferImplementation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deferImplementation(Boolean value) {
            this.deferImplementation = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UIExitPlanModeResponse build() {
            if (approved == null) throw new IllegalStateException("Missing required property: approved");
            return new UIExitPlanModeResponse(approved, selectedAction, autoApproveEdits, feedback, deferImplementation);
        }
    }
}
