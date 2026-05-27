/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

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

    @JsonProperty("feedback")
    private String feedback;

    /**
     * Creates a result that approves this single request.
     *
     * @return a new approved result
     * @since 1.3.0
     */
    public static PermissionRequestResult approveOnce() {
        return new PermissionRequestResult().setKind(PermissionRequestResultKind.APPROVED);
    }

    /**
     * Creates a result that rejects the request, optionally forwarding feedback to
     * the LLM.
     *
     * @param feedback
     *            optional feedback message, or {@code null}
     * @return a new rejected result
     * @since 1.3.0
     */
    public static PermissionRequestResult reject(String feedback) {
        var result = new PermissionRequestResult().setKind(PermissionRequestResultKind.REJECTED);
        result.setFeedback(feedback);
        return result;
    }

    /**
     * Creates a result denying the request because no user is available to confirm
     * it.
     *
     * @return a new user-not-available result
     * @since 1.3.0
     */
    public static PermissionRequestResult userNotAvailable() {
        return new PermissionRequestResult().setKind(PermissionRequestResultKind.USER_NOT_AVAILABLE);
    }

    /**
     * Creates a result that declines to respond to this permission request,
     * allowing another connected client to answer instead.
     *
     * @return a new no-result result
     * @since 1.3.0
     */
    public static PermissionRequestResult noResult() {
        return new PermissionRequestResult().setKind(PermissionRequestResultKind.NO_RESULT);
    }

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

    /**
     * Gets optional human-readable feedback to forward to the LLM along with the
     * decision.
     *
     * @return the feedback message, or {@code null}
     * @since 1.3.0
     */
    public String getFeedback() {
        return feedback;
    }

    /**
     * Sets optional human-readable feedback to forward to the LLM along with the
     * decision.
     *
     * @param feedback
     *            the feedback message
     * @return this result for method chaining
     * @since 1.3.0
     */
    public PermissionRequestResult setFeedback(String feedback) {
        this.feedback = feedback;
        return this;
    }
}
