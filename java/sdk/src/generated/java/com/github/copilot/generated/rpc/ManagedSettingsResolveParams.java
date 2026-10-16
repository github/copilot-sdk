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
import javax.annotation.processing.Generated;

/**
 * Request parameters for the {@code managedSettings.resolve} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsResolveParams(
    /** Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used, or device policy only when no account is signed in. */
    @JsonProperty("selectionId") String selectionId,
    /** GitHub token to resolve instead of the current account. The call fails when the token cannot be resolved. */
    @JsonProperty("gitHubToken") String gitHubToken,
    /** Embedding client identity for server policy requests, as in session creation. Omit for the CLI identity. */
    @JsonProperty("clientName") String clientName,
    /** Working directory used to run an organization policy helper. When omitted, sessionless resolution does not run the helper. */
    @JsonProperty("workingDirectory") String workingDirectory
) {

    /**
     * Creates managed-settings resolution parameters without a working directory.
     *
     * @param selectionId Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used, or device policy only when no account is signed in.
     * @param gitHubToken GitHub token to resolve instead of the current account. The call fails when the token cannot be resolved.
     * @param clientName Embedding client identity for server policy requests, as in session creation. Omit for the CLI identity.
     */
    public ManagedSettingsResolveParams(
        String selectionId,
        String gitHubToken,
        String clientName
    ) {
        this(selectionId, gitHubToken, clientName, null);
    }
}
