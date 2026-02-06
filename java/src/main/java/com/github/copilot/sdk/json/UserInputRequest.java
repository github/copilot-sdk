/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request for user input from the agent.
 * <p>
 * This is sent when the agent uses the ask_user tool to request input from the
 * user.
 *
 * @since 1.0.6
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInputRequest {

    @JsonProperty("question")
    private String question;

    @JsonProperty("choices")
    private List<String> choices;

    @JsonProperty("allowFreeform")
    private Boolean allowFreeform;

    /**
     * Gets the question to ask the user.
     *
     * @return the question text
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Sets the question to ask the user.
     *
     * @param question
     *            the question text
     * @return this instance for method chaining
     */
    public UserInputRequest setQuestion(String question) {
        this.question = question;
        return this;
    }

    /**
     * Gets the optional choices for multiple choice questions.
     *
     * @return the list of choices, or {@code null} for freeform input
     */
    public List<String> getChoices() {
        return choices == null ? null : Collections.unmodifiableList(choices);
    }

    /**
     * Sets the choices for multiple choice questions.
     *
     * @param choices
     *            the list of choices
     * @return this instance for method chaining
     */
    public UserInputRequest setChoices(List<String> choices) {
        this.choices = choices;
        return this;
    }

    /**
     * Returns whether freeform text input is allowed.
     *
     * @return {@code true} if freeform input is allowed, {@code null} if not
     *         specified
     */
    public Boolean getAllowFreeform() {
        return allowFreeform;
    }

    /**
     * Sets whether freeform text input is allowed.
     *
     * @param allowFreeform
     *            {@code true} to allow freeform input
     * @return this instance for method chaining
     */
    public UserInputRequest setAllowFreeform(Boolean allowFreeform) {
        this.allowFreeform = allowFreeform;
        return this;
    }
}
