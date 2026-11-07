/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Variant {@code user_input} of {@link HumanResponseRecordedResponse}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HumanResponseRecordedResponseUserInput extends HumanResponseRecordedResponse {

    @JsonProperty("responseKind")
    private final String responseKind = "user_input";

    @Override
    public String getResponseKind() { return responseKind; }

    /** Exact question displayed to the user. */
    @JsonProperty("question")
    private String question;

    /** Exact choices displayed to the user, when the request offered choices. */
    @JsonProperty("choices")
    private List<String> choices;

    /** Whether the displayed request allowed a free-form answer. */
    @JsonProperty("allowFreeform")
    private Boolean allowFreeform;

    /** Exact selected or free-form answer submitted by the user. */
    @JsonProperty("answer")
    private String answer;

    /** Whether the answer was typed as free-form text rather than selected from the displayed choices. */
    @JsonProperty("wasFreeform")
    private Boolean wasFreeform;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getChoices() { return choices; }
    public void setChoices(List<String> choices) { this.choices = choices; }

    public Boolean getAllowFreeform() { return allowFreeform; }
    public void setAllowFreeform(Boolean allowFreeform) { this.allowFreeform = allowFreeform; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public Boolean getWasFreeform() { return wasFreeform; }
    public void setWasFreeform(Boolean wasFreeform) { this.wasFreeform = wasFreeform; }


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
        private String question;
        private List<String> choices;
        private Boolean allowFreeform;
        private String answer;
        private Boolean wasFreeform;

        /**
         * Sets the {@code question} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder question(String value) {
            this.question = value;
            return this;
        }

        /**
         * Sets the {@code choices} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder choices(List<String> value) {
            this.choices = value;
            return this;
        }

        /**
         * Sets the {@code allowFreeform} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowFreeform(Boolean value) {
            this.allowFreeform = value;
            return this;
        }

        /**
         * Sets the {@code answer} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder answer(String value) {
            this.answer = value;
            return this;
        }

        /**
         * Sets the {@code wasFreeform} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder wasFreeform(Boolean value) {
            this.wasFreeform = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HumanResponseRecordedResponseUserInput build() {
            if (question == null) throw new IllegalStateException("Missing required property: question");
            if (answer == null) throw new IllegalStateException("Missing required property: answer");
            if (wasFreeform == null) throw new IllegalStateException("Missing required property: wasFreeform");
            HumanResponseRecordedResponseUserInput instance = new HumanResponseRecordedResponseUserInput();
            instance.setQuestion(question);
            instance.setChoices(choices);
            instance.setAllowFreeform(allowFreeform);
            instance.setAnswer(answer);
            instance.setWasFreeform(wasFreeform);
            return instance;
        }
    }
}
