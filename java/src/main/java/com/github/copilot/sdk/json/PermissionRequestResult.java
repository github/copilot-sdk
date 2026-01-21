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
 * <li>"user-approved" - User approved the permission request</li>
 * <li>"user-denied" - User denied the permission request</li>
 * <li>"denied-no-approval-rule-and-could-not-request-from-user" - No handler
 * and couldn't ask user</li>
 * </ul>
 *
 * @see PermissionHandler
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PermissionRequestResult {

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("rules")
    private List<Object> rules;

    /**
     * Gets the result kind.
     *
     * @return the result kind indicating approval or denial
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the result kind.
     *
     * @param kind
     *            the result kind
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
