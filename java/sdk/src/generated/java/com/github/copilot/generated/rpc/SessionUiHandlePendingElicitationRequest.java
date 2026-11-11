/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionUiHandlePendingElicitationRequest {

    /** The unique request ID from the elicitation.requested event */
    @JsonProperty("requestId")
    private final String requestId;

    /** The elicitation response (accept with form values, decline, or cancel) */
    @JsonProperty("result")
    private final UIElicitationResponse result;

    /** Who answered and how. When it says a person answered interactively (`source` `human_response` with `responseCapability` `interactive`), the runtime records the answer as the person's own authority for Assisted Permissions, but only from the session's authenticated owning connection or the in-process host; an answer from any other caller settles without it. The runtime decides which answers carry authority. Omit for automated answers. */
    @JsonProperty("decisionContext")
    private PermissionDecisionContext decisionContext;

    /**
     * Creates a request with its required inputs.
     *
     * @param requestId The unique request ID from the elicitation.requested event
     * @param result The elicitation response (accept with form values, decline, or cancel)
     */
    public SessionUiHandlePendingElicitationRequest(String requestId, UIElicitationResponse result) {
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.result = Objects.requireNonNull(result, "result");
    }

    /**
     * Returns the {@code requestId} property.
     *
     * @return The unique request ID from the elicitation.requested event
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Returns the {@code result} property.
     *
     * @return The elicitation response (accept with form values, decline, or cancel)
     */
    public UIElicitationResponse getResult() {
        return result;
    }

    /**
     * Returns the {@code decisionContext} property.
     *
     * @return Who answered and how. When it says a person answered interactively (`source` `human_response` with `responseCapability` `interactive`), the runtime records the answer as the person's own authority for Assisted Permissions, but only from the session's authenticated owning connection or the in-process host; an answer from any other caller settles without it. The runtime decides which answers carry authority. Omit for automated answers.
     */
    public PermissionDecisionContext getDecisionContext() {
        return decisionContext;
    }

    /**
     * Sets the {@code decisionContext} property.
     *
     * @param value Who answered and how. When it says a person answered interactively (`source` `human_response` with `responseCapability` `interactive`), the runtime records the answer as the person's own authority for Assisted Permissions, but only from the session's authenticated owning connection or the in-process host; an answer from any other caller settles without it. The runtime decides which answers carry authority. Omit for automated answers.
     * @return this request
     */
    public SessionUiHandlePendingElicitationRequest setDecisionContext(PermissionDecisionContext value) {
        this.decisionContext = value;
        return this;
    }
}
