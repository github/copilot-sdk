/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.generated.AssistedApprovalAssessment;

/**
 * Context information for a permission request invocation.
 * <p>
 * This object provides context about the session where the permission request
 * originated.
 *
 * @see PermissionHandler
 * @since 1.0.0
 */
public final class PermissionInvocation {

    private String sessionId;
    private boolean managedSettingsEnabled;
    private AssistedApprovalAssessment assistedApprovalAssessment;
    private PermissionRequest promptRequest;

    /**
     * Gets the session ID where the permission was requested.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session ID.
     *
     * @param sessionId
     *            the session ID
     * @return this invocation for method chaining
     */
    public PermissionInvocation setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Gets whether managed settings are enabled for this session.
     *
     * @return whether managed settings are enabled
     */
    public boolean isManagedSettingsEnabled() {
        return managedSettingsEnabled;
    }

    /**
     * Sets whether managed settings are enabled for this session.
     *
     * @param managedSettingsEnabled
     *            whether managed settings are enabled
     * @return this invocation for method chaining
     */
    public PermissionInvocation setManagedSettingsEnabled(boolean managedSettingsEnabled) {
        this.managedSettingsEnabled = managedSettingsEnabled;
        return this;
    }

    /**
     * Gets the canonical runtime-owned Assisted Permissions assessment.
     * <p>
     * This explains the assisted review only; {@code permission.completed} remains
     * authoritative for the final permission result. It is {@code null} with older
     * runtimes and legacy direct permission callbacks.
     *
     * @return the assisted approval assessment, or {@code null} when unavailable
     */
    @CopilotExperimental
    public AssistedApprovalAssessment getAssistedApprovalAssessment() {
        return assistedApprovalAssessment;
    }

    /**
     * Sets the canonical runtime-owned Assisted Permissions assessment.
     *
     * @param assistedApprovalAssessment
     *            the assessment, or {@code null}
     * @return this invocation for method chaining
     */
    @CopilotExperimental
    public PermissionInvocation setAssistedApprovalAssessment(AssistedApprovalAssessment assistedApprovalAssessment) {
        this.assistedApprovalAssessment = assistedApprovalAssessment;
        return this;
    }

    /**
     * Gets the derived user-facing permission prompt supplied by newer runtimes.
     * <p>
     * The raw request retains managed-policy provenance. Only {@code commands},
     * {@code read}, {@code write}, {@code url}, and {@code workflow} prompts can
     * clear that human requirement after Assisted approval. For missing or other
     * prompt kinds, keep the raw marker authoritative. Prompt-only fields such as
     * {@code assistedApproval} are available through
     * {@link PermissionRequest#getExtensionData()}.
     *
     * @return the derived prompt, or {@code null} when unavailable
     */
    public PermissionRequest getPromptRequest() {
        return promptRequest;
    }

    /**
     * Sets the derived user-facing permission prompt.
     *
     * @param promptRequest
     *            the derived prompt, or {@code null}
     * @return this invocation for method chaining
     */
    public PermissionInvocation setPromptRequest(PermissionRequest promptRequest) {
        this.promptRequest = promptRequest;
        return this;
    }
}
