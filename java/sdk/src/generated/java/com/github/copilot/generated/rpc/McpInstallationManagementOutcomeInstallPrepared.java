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
 * Variant {@code install-prepared} of {@link McpInstallationManagementOutcome}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpInstallationManagementOutcomeInstallPrepared(
    /** Known original operation identity, returned before callback or effects. */
    @JsonProperty("operation") McpPreparedInstall operation,
    /** Installation management outcome discriminator. */
    @JsonProperty("kind") String kind
) implements McpInstallationManagementOutcome {
    public McpInstallationManagementOutcomeInstallPrepared {
        kind = "install-prepared";
    }

    public McpInstallationManagementOutcomeInstallPrepared(
        McpPreparedInstall operation
    ) {
        this(operation, "install-prepared");
    }
}
