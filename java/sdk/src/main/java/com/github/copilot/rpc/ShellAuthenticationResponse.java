/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Answer to a {@link ShellAuthenticationRequest}, created with
 * {@link #accept(String)} or {@link #cancel()}. Answer a {@code confirm}
 * request with {@code "yes"}, or cancel it to decline.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShellAuthenticationResponse {

    /** The action that answers the request. */
    public static final String ACTION_ACCEPT = "accept";

    /** The action that declines the request. */
    public static final String ACTION_CANCEL = "cancel";

    @JsonProperty("action")
    private String action = ACTION_CANCEL;

    @JsonProperty("value")
    private String value;

    /**
     * Creates a response that answers the request with {@code value}.
     *
     * @param value
     *            the answer
     * @return an accepting response
     */
    public static ShellAuthenticationResponse accept(String value) {
        return new ShellAuthenticationResponse().setAction(ACTION_ACCEPT).setValue(value);
    }

    /**
     * Creates a response that declines the request. The program that asked fails.
     *
     * @return a cancelling response
     */
    public static ShellAuthenticationResponse cancel() {
        return new ShellAuthenticationResponse().setAction(ACTION_CANCEL);
    }

    /**
     * Gets the action: {@link #ACTION_ACCEPT} or {@link #ACTION_CANCEL}.
     *
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the action.
     *
     * @param action
     *            {@link #ACTION_ACCEPT} or {@link #ACTION_CANCEL}
     * @return this instance for method chaining
     */
    public ShellAuthenticationResponse setAction(String action) {
        this.action = action;
        return this;
    }

    /**
     * Gets the answer, set when the action is {@link #ACTION_ACCEPT}.
     *
     * @return the answer, or {@code null}
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the answer.
     *
     * @param value
     *            the answer
     * @return this instance for method chaining
     */
    public ShellAuthenticationResponse setValue(String value) {
        this.value = value;
        return this;
    }
}
