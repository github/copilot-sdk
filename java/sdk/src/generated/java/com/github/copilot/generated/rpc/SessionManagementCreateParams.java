/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Host callback. The host owns workspace records, UI, approval routing and cleanup.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionManagementCreateParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The originating session; the host must not escalate its permissions. */
    @JsonProperty("parentSessionId") String parentSessionId,
    /** Runtime-allocated identity. The host must create a fresh session with this exact ID. */
    @JsonProperty("newSessionId") String newSessionId,
    /** The new session must use this workspace and configuration directory. */
    @JsonProperty("cwd") String cwd,
    /** Parent's resolved configuration directory, which the host must preserve. */
    @JsonProperty("configDir") String configDir,
    /** Must not be broadened by the creating host. */
    @JsonProperty("permissionMode") PermissionMode permissionMode,
    /** Effective model to apply when creating the child. */
    @JsonProperty("model") String model,
    /** Provider owning the effective model, when specified. */
    @JsonProperty("providerId") String providerId,
    /** Effective reasoning effort, when specified. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Effective model-supported context tier, when specified. */
    @JsonProperty("contextTier") ContextTier contextTier,
    /** The host must preserve the parent's current model restrictions. */
    @JsonProperty("allowedModels") List<String> allowedModels,
    /** Optional short name supplied for the independent session. */
    @JsonProperty("name") String name
) {
}
