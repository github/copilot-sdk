/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "exit_plan_mode.requested". Plan approval request with plan content and available user actions
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ExitPlanModeRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "exit_plan_mode.requested"; }

    @JsonProperty("data")
    private ExitPlanModeRequestedEventData data;

    public ExitPlanModeRequestedEventData getData() { return data; }
    public void setData(ExitPlanModeRequestedEventData data) { this.data = data; }

    /** Data payload for {@link ExitPlanModeRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExitPlanModeRequestedEventData(
        /** Unique identifier for this request; used to respond via session.respondToExitPlanMode() */
        @JsonProperty("requestId") String requestId,
        /** Summary of the plan that was created */
        @JsonProperty("summary") String summary,
        /** Full content of the plan file */
        @JsonProperty("planContent") String planContent,
        /** Available actions the user can take */
        @JsonProperty("actions") List<ExitPlanModeAction> actions,
        /** Recommended action to preselect for the user */
        @JsonProperty("recommendedAction") ExitPlanModeAction recommendedAction,
        /** Model the session had selected when the plan was authored, when one is known */
        @JsonProperty("model") String model
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ExitPlanModeRequestedEventData {}

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
            private String requestId;
            private String summary;
            private String planContent;
            private List<ExitPlanModeAction> actions;
            private ExitPlanModeAction recommendedAction;
            private String model;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code summary} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder summary(String value) {
                this.summary = value;
                return this;
            }

            /**
             * Sets the {@code planContent} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder planContent(String value) {
                this.planContent = value;
                return this;
            }

            /**
             * Sets the {@code actions} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder actions(List<ExitPlanModeAction> value) {
                this.actions = value;
                return this;
            }

            /**
             * Sets the {@code recommendedAction} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder recommendedAction(ExitPlanModeAction value) {
                this.recommendedAction = value;
                return this;
            }

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ExitPlanModeRequestedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (summary == null) throw new IllegalStateException("Missing required property: summary");
                if (planContent == null) throw new IllegalStateException("Missing required property: planContent");
                if (actions == null) throw new IllegalStateException("Missing required property: actions");
                if (recommendedAction == null) throw new IllegalStateException("Missing required property: recommendedAction");
                return new ExitPlanModeRequestedEventData(requestId, summary, planContent, actions, recommendedAction, model);
            }
        }
    }
}
