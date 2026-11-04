/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Patch of permission policy fields to apply (omit a field to leave it unchanged).
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionPermissionsConfigureRequest {

    /** If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged. */
    @JsonProperty("approveAllToolPermissionRequests")
    private Boolean approveAllToolPermissionRequests;

    /** If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged. */
    @JsonProperty("approveAllReadPermissionRequests")
    private Boolean approveAllReadPermissionRequests;

    /** If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged. */
    @JsonProperty("rules")
    private PermissionRulesSet rules;

    /** If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged. */
    @JsonProperty("paths")
    private PermissionPathsConfig paths;

    /** If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged. */
    @JsonProperty("urls")
    private PermissionUrlsConfig urls;

    /** If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged. */
    @JsonProperty("additionalContentExclusionPolicies")
    private List<PermissionsConfigureAdditionalContentExclusionPolicy> additionalContentExclusionPolicies;

    /** User-authored instructions appended verbatim to the assisted-approval judge's system prompt. Applies to subsequent judge requests in this local session without changing permission mode, pre-judge gates, or the coding agent's prompt. Omit to leave unchanged; an empty or whitespace-only string clears the instructions. Hosts must reapply their configuration after resuming a session. Remote sessions are not supported. */
    @JsonProperty("assistedApprovalInstructions")
    private String assistedApprovalInstructions;

    /**
     * Creates a request with its required inputs.
     */
    public SessionPermissionsConfigureRequest() {
    }

    /**
     * Returns the {@code approveAllToolPermissionRequests} property.
     *
     * @return If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged.
     */
    public Boolean getApproveAllToolPermissionRequests() {
        return approveAllToolPermissionRequests;
    }

    /**
     * Returns the {@code approveAllReadPermissionRequests} property.
     *
     * @return If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged.
     */
    public Boolean getApproveAllReadPermissionRequests() {
        return approveAllReadPermissionRequests;
    }

    /**
     * Returns the {@code rules} property.
     *
     * @return If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
     */
    public PermissionRulesSet getRules() {
        return rules;
    }

    /**
     * Returns the {@code paths} property.
     *
     * @return If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
     */
    public PermissionPathsConfig getPaths() {
        return paths;
    }

    /**
     * Returns the {@code urls} property.
     *
     * @return If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
     */
    public PermissionUrlsConfig getUrls() {
        return urls;
    }

    /**
     * Returns the {@code additionalContentExclusionPolicies} property.
     *
     * @return If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged.
     */
    public List<PermissionsConfigureAdditionalContentExclusionPolicy> getAdditionalContentExclusionPolicies() {
        return additionalContentExclusionPolicies;
    }

    /**
     * Returns the {@code assistedApprovalInstructions} property.
     *
     * @return User-authored instructions appended verbatim to the assisted-approval judge's system prompt. Applies to subsequent judge requests in this local session without changing permission mode, pre-judge gates, or the coding agent's prompt. Omit to leave unchanged; an empty or whitespace-only string clears the instructions. Hosts must reapply their configuration after resuming a session. Remote sessions are not supported.
     */
    public String getAssistedApprovalInstructions() {
        return assistedApprovalInstructions;
    }

    /**
     * Sets the {@code approveAllToolPermissionRequests} property.
     *
     * @param value If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setApproveAllToolPermissionRequests(Boolean value) {
        this.approveAllToolPermissionRequests = value;
        return this;
    }

    /**
     * Sets the {@code approveAllReadPermissionRequests} property.
     *
     * @param value If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setApproveAllReadPermissionRequests(Boolean value) {
        this.approveAllReadPermissionRequests = value;
        return this;
    }

    /**
     * Sets the {@code rules} property.
     *
     * @param value If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setRules(PermissionRulesSet value) {
        this.rules = value;
        return this;
    }

    /**
     * Sets the {@code paths} property.
     *
     * @param value If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setPaths(PermissionPathsConfig value) {
        this.paths = value;
        return this;
    }

    /**
     * Sets the {@code urls} property.
     *
     * @param value If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setUrls(PermissionUrlsConfig value) {
        this.urls = value;
        return this;
    }

    /**
     * Sets the {@code additionalContentExclusionPolicies} property.
     *
     * @param value If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setAdditionalContentExclusionPolicies(List<PermissionsConfigureAdditionalContentExclusionPolicy> value) {
        this.additionalContentExclusionPolicies = value;
        return this;
    }

    /**
     * Sets the {@code assistedApprovalInstructions} property.
     *
     * @param value User-authored instructions appended verbatim to the assisted-approval judge's system prompt. Applies to subsequent judge requests in this local session without changing permission mode, pre-judge gates, or the coding agent's prompt. Omit to leave unchanged; an empty or whitespace-only string clears the instructions. Hosts must reapply their configuration after resuming a session. Remote sessions are not supported.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setAssistedApprovalInstructions(String value) {
        this.assistedApprovalInstructions = value;
        return this;
    }
}
