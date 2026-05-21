/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response to a user input request.
 *
 * @since 1.0.6
 */
public class UserInputResponse {

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("wasFreeform")
    private boolean wasFreeform;

    /**
     * Gets the user's answer.
     *
     * @return the answer text
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Sets the user's answer.
     *
     * @param answer
     *            the answer text
     * @return this instance for method chaining
     */
    public UserInputResponse setAnswer(String answer) {
        this.answer = answer;
        return this;
    }

    /**
     * Returns whether the answer was freeform (not from the provided choices).
     *
     * @return {@code true} if the answer was freeform
     */
    public boolean isWasFreeform() {
        return wasFreeform;
    }

    /**
     * Sets whether the answer was freeform.
     *
     * @param wasFreeform
     *            {@code true} if the answer was freeform
     * @return this instance for method chaining
     */
    public UserInputResponse setWasFreeform(boolean wasFreeform) {
        this.wasFreeform = wasFreeform;
        return this;
    }
}
