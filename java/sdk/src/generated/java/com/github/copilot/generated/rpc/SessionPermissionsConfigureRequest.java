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

    /** SDK-host advisory JSON for future Assisted Permissions judge requests. The value replaces only its named source; null clears that source. Omit to leave all sources unchanged. The runtime applies strict structural and size limits, renders the source in a runtime-controlled advisory frame, and never treats the value as instructions or authorization. Session-scoped only; hosts must reapply after resume. */
    @JsonProperty("assistedApprovalContext")
    private PermissionsConfigureAssistedApprovalContext assistedApprovalContext;

    /** Optional owner-authenticated per-source update to lifecycle-fenced session authority. Active grants from all sources are rendered together, in source order, to the Assisted Permissions judge, which alone interprets their statements and scope. Only the owning non-extension session connection, bound to the current session generation, may replace or revoke authority. Omit to leave unchanged. Hard runtime controls, mandatory policy, and later human restrictions still take precedence. Session-scoped only; hosts must reapply after resume. */
    @JsonProperty("authenticatedAuthority")
    private AuthenticatedAuthorityUpdate authenticatedAuthority;

    /** Declares whether this connection's registered permission handler applies non-human host policy to the tools this connection registered. When true, unattended Assisted Autopilot sends permission requests for those tools to the handler before runtime Assisted Approval or unattended fallback can settle them. All other requests, including shell commands, file access, URLs, and tools registered by other connections or extensions, keep runtime settlement. Managed policy and other runtime enforcement still take precedence. Only the session's owning non-extension connection may set this field. The declaration ends when that connection detaches or the session is resumed, so hosts must reapply it after resume. Omit to leave unchanged; false clears it. Without a declaration, registered handlers are not consulted during unattended Assisted Autopilot. Remote sessions are not supported. */
    @JsonProperty("hostPolicyPermissionHandler")
    private Boolean hostPolicyPermissionHandler;

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
     * Returns the {@code assistedApprovalContext} property.
     *
     * @return SDK-host advisory JSON for future Assisted Permissions judge requests. The value replaces only its named source; null clears that source. Omit to leave all sources unchanged. The runtime applies strict structural and size limits, renders the source in a runtime-controlled advisory frame, and never treats the value as instructions or authorization. Session-scoped only; hosts must reapply after resume.
     */
    public PermissionsConfigureAssistedApprovalContext getAssistedApprovalContext() {
        return assistedApprovalContext;
    }

    /**
     * Returns the {@code authenticatedAuthority} property.
     *
     * @return Optional owner-authenticated per-source update to lifecycle-fenced session authority. Active grants from all sources are rendered together, in source order, to the Assisted Permissions judge, which alone interprets their statements and scope. Only the owning non-extension session connection, bound to the current session generation, may replace or revoke authority. Omit to leave unchanged. Hard runtime controls, mandatory policy, and later human restrictions still take precedence. Session-scoped only; hosts must reapply after resume.
     */
    public AuthenticatedAuthorityUpdate getAuthenticatedAuthority() {
        return authenticatedAuthority;
    }

    /**
     * Returns the {@code hostPolicyPermissionHandler} property.
     *
     * @return Declares whether this connection's registered permission handler applies non-human host policy to the tools this connection registered. When true, unattended Assisted Autopilot sends permission requests for those tools to the handler before runtime Assisted Approval or unattended fallback can settle them. All other requests, including shell commands, file access, URLs, and tools registered by other connections or extensions, keep runtime settlement. Managed policy and other runtime enforcement still take precedence. Only the session's owning non-extension connection may set this field. The declaration ends when that connection detaches or the session is resumed, so hosts must reapply it after resume. Omit to leave unchanged; false clears it. Without a declaration, registered handlers are not consulted during unattended Assisted Autopilot. Remote sessions are not supported.
     */
    public Boolean getHostPolicyPermissionHandler() {
        return hostPolicyPermissionHandler;
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

    /**
     * Sets the {@code assistedApprovalContext} property.
     *
     * @param value SDK-host advisory JSON for future Assisted Permissions judge requests. The value replaces only its named source; null clears that source. Omit to leave all sources unchanged. The runtime applies strict structural and size limits, renders the source in a runtime-controlled advisory frame, and never treats the value as instructions or authorization. Session-scoped only; hosts must reapply after resume.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setAssistedApprovalContext(PermissionsConfigureAssistedApprovalContext value) {
        this.assistedApprovalContext = value;
        return this;
    }

    /**
     * Sets the {@code authenticatedAuthority} property.
     *
     * @param value Optional owner-authenticated per-source update to lifecycle-fenced session authority. Active grants from all sources are rendered together, in source order, to the Assisted Permissions judge, which alone interprets their statements and scope. Only the owning non-extension session connection, bound to the current session generation, may replace or revoke authority. Omit to leave unchanged. Hard runtime controls, mandatory policy, and later human restrictions still take precedence. Session-scoped only; hosts must reapply after resume.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setAuthenticatedAuthority(AuthenticatedAuthorityUpdate value) {
        this.authenticatedAuthority = value;
        return this;
    }

    /**
     * Sets the {@code hostPolicyPermissionHandler} property.
     *
     * @param value Declares whether this connection's registered permission handler applies non-human host policy to the tools this connection registered. When true, unattended Assisted Autopilot sends permission requests for those tools to the handler before runtime Assisted Approval or unattended fallback can settle them. All other requests, including shell commands, file access, URLs, and tools registered by other connections or extensions, keep runtime settlement. Managed policy and other runtime enforcement still take precedence. Only the session's owning non-extension connection may set this field. The declaration ends when that connection detaches or the session is resumed, so hosts must reapply it after resume. Omit to leave unchanged; false clears it. Without a declaration, registered handlers are not consulted during unattended Assisted Autopilot. Remote sessions are not supported.
     * @return this request
     */
    public SessionPermissionsConfigureRequest setHostPolicyPermissionHandler(Boolean value) {
        this.hostPolicyPermissionHandler = value;
        return this;
    }
}
