/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a permission request decision.
 * <p>
 * This object indicates whether a permission request was approved or denied,
 * and may include additional rules for future similar requests.
 *
 * <h2>Common Result Kinds</h2>
 * <ul>
 * <li>{@link PermissionRequestResultKind#APPROVED} — approved</li>
 * <li>{@link PermissionRequestResultKind#DENIED_BY_RULES} — denied by
 * rules</li>
 * <li>{@link PermissionRequestResultKind#DENIED_COULD_NOT_REQUEST_FROM_USER} —
 * no handler and couldn't ask user</li>
 * <li>{@link PermissionRequestResultKind#DENIED_INTERACTIVELY_BY_USER} — denied
 * by the user interactively</li>
 * </ul>
 *
 * @see PermissionHandler
 * @see PermissionRequestResultKind
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PermissionRequestResult {

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("rules")
    private List<Object> rules;

    /**
     * Gets the result kind as a string.
     *
     * @return the result kind indicating approval or denial
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the result kind using a {@link PermissionRequestResultKind} value.
     *
     * @param kind
     *            the result kind
     * @return this result for method chaining
     * @since 1.1.0
     */
    public PermissionRequestResult setKind(PermissionRequestResultKind kind) {
        this.kind = kind != null ? kind.getValue() : null;
        return this;
    }

    /**
     * Sets the result kind using a raw string value.
     *
     * @param kind
     *            the result kind string
     * @return this result for method chaining
     */
    public PermissionRequestResult setKind(String kind) {
        this.kind = kind;
        return this;
    }

    /**
     * Gets the approval rules.
     *
     * @return the list of rules for future similar requests
     */
    public List<Object> getRules() {
        return rules;
    }

    /**
     * Sets approval rules for future similar requests.
     *
     * @param rules
     *            the list of rules
     * @return this result for method chaining
     */
    public PermissionRequestResult setRules(List<Object> rules) {
        this.rules = rules;
        return this;
    }
}
