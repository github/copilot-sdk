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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Variant {@code recovered} of {@link McpInstallationManagementOutcome}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpInstallationManagementOutcomeRecovered(
    /** Freshly inspected receipts after successful durable reconciliation. */
    @JsonProperty("installations") List<McpInstallationSummary> installations,
    /** Installation management outcome discriminator. */
    @JsonProperty("kind") String kind
) implements McpInstallationManagementOutcome {
    public McpInstallationManagementOutcomeRecovered {
        kind = "recovered";
    }

    public McpInstallationManagementOutcomeRecovered(
        List<McpInstallationSummary> installations
    ) {
        this(installations, "recovered");
    }
}
