/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.github.copilot.rpc;

/**
 * Options for searching message content in a Copilot session.
 */
public class SearchMessagesOptions {

    private String eventType;
    private boolean regex;
    private boolean caseSensitive;

    /**
     * Gets the event type filter.
     *
     * @return {@code user.message}, {@code assistant.message}, or {@code null}
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Restricts results to a message event type.
     *
     * @param eventType
     *            {@code user.message} or {@code assistant.message}
     * @return this options object for method chaining
     */
    public SearchMessagesOptions setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    /**
     * Gets whether the query is treated as a regular expression.
     *
     * @return {@code true} for regular-expression matching
     */
    public boolean isRegex() {
        return regex;
    }

    /**
     * Sets whether the query is treated as a regular expression.
     *
     * @param regex
     *            {@code true} to enable regular-expression matching
     * @return this options object for method chaining
     */
    public SearchMessagesOptions setRegex(boolean regex) {
        this.regex = regex;
        return this;
    }

    /**
     * Gets whether matching is case-sensitive.
     *
     * @return {@code true} for case-sensitive matching
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Sets whether matching is case-sensitive.
     *
     * @param caseSensitive
     *            {@code true} to enable case-sensitive matching
     * @return this options object for method chaining
     */
    public SearchMessagesOptions setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }
}
