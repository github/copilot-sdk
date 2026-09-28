/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Read-only or recovery management result, never permission to activate or replay.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpInstallationManagementOutcomeRecoveryRequired.class, name = "recovery-required"),
    @JsonSubTypes.Type(value = McpInstallationManagementOutcomeInstallPrepared.class, name = "install-prepared"),
    @JsonSubTypes.Type(value = McpInstallationManagementOutcomeListed.class, name = "listed"),
    @JsonSubTypes.Type(value = McpInstallationManagementOutcomeRecovered.class, name = "recovered"),
    @JsonSubTypes.Type(value = McpInstallationManagementOutcomeUninstallPlanned.class, name = "uninstall-planned"),
    @JsonSubTypes.Type(value = McpInstallationManagementOutcomeOperation.class, name = "operation"),
    @JsonSubTypes.Type(value = McpInstallationManagementOutcomeRefused.class, name = "refused")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public sealed interface McpInstallationManagementOutcome permits McpInstallationManagementOutcomeRecoveryRequired, McpInstallationManagementOutcomeInstallPrepared, McpInstallationManagementOutcomeListed, McpInstallationManagementOutcomeRecovered, McpInstallationManagementOutcomeUninstallPlanned, McpInstallationManagementOutcomeOperation, McpInstallationManagementOutcomeRefused {
    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    @JsonProperty("kind")
    String kind();
}
