/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Variant {@code refused} of {@link McpInstallationManagementOutcome}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpInstallationManagementOutcomeRefused(
    /** Specific bounded refusal. */
    @JsonProperty("reason") McpInstallationFailureReason reason,
    /** Installation management outcome discriminator. */
    @JsonProperty("kind") String kind
) implements McpInstallationManagementOutcome {
    public McpInstallationManagementOutcomeRefused {
        kind = "refused";
    }

    public McpInstallationManagementOutcomeRefused(
        McpInstallationFailureReason reason
    ) {
        this(reason, "refused");
    }
}
