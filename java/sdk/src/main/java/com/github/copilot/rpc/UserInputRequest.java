/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Optional;

/**
 * Request for user input from the agent.
 * <p>
 * This is sent when the agent uses the ask_user tool to request input from the
 * user.
 *
 * @since 1.0.6
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInputRequest {

    @JsonProperty("question")
    private String question;

    @JsonProperty("choices")
    private List<String> choices;

    @JsonProperty("allowFreeform")
    private Boolean allowFreeform;

    @JsonProperty("requestId")
    private String requestId;

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
     * @return an {@link java.util.Optional} containing {@code true} if freeform
     *         input is allowed, or {@link java.util.Optional#empty()} if not
     *         specified
     */
    @JsonIgnore
    public Optional<Boolean> getAllowFreeform() {
        return Optional.ofNullable(allowFreeform);
    }

    /**
     * Sets whether freeform text input is allowed.
     *
     * @param allowFreeform
     *            {@code true} to allow freeform input
     * @return this instance for method chaining
     */
    public UserInputRequest setAllowFreeform(boolean allowFreeform) {
        this.allowFreeform = allowFreeform;
        return this;
    }

    /**
     * Clears the allowFreeform setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public UserInputRequest clearAllowFreeform() {
        this.allowFreeform = null;
        return this;
    }

    /**
     * Gets the runtime request ID of the pending {@code user_input.requested}
     * request.
     * <p>
     * Pass it as {@code requestId} to the session's
     * {@code session.ui.handlePendingUserInput} RPC (or the trusted
     * {@code session.ui.handleHumanUserInput} path) to correlate an out-of-band
     * answer with this exact request. This is not the JSON-RPC message ID.
     *
     * @return the runtime request ID, or {@code null} when the runtime predates
     *         this field
     * @since 1.0.15-preview.1
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Sets the runtime request ID of the pending {@code user_input.requested}
     * request.
     *
     * @param requestId
     *            the runtime request ID, or {@code null} if unknown
     * @return this instance for method chaining
     * @since 1.0.15-preview.1
     */
    public UserInputRequest setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

}
