/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Redacted validation and memory-tool settings for a session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSettingsValidationSnapshot(
    @JsonProperty("timeout") Double timeout,
    @JsonProperty("dependabotTimeout") Double dependabotTimeout,
    @JsonProperty("codeqlEnabled") Boolean codeqlEnabled,
    @JsonProperty("codeReviewEnabled") Boolean codeReviewEnabled,
    @JsonProperty("codeReviewModel") String codeReviewModel,
    @JsonProperty("advisoryEnabled") Boolean advisoryEnabled,
    @JsonProperty("secretScanningEnabled") Boolean secretScanningEnabled,
    @JsonProperty("memoryStoreEnabled") Boolean memoryStoreEnabled,
    @JsonProperty("memoryVoteEnabled") Boolean memoryVoteEnabled
) {
}
