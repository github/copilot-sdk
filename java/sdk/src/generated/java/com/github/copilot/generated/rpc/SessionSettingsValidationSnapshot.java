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
    /** General validation timeout budget in seconds. */
    @JsonProperty("timeout") Double timeout,
    /** Dependabot validation timeout budget in seconds. */
    @JsonProperty("dependabotTimeout") Double dependabotTimeout,
    /** Whether CodeQL validation is enabled. */
    @JsonProperty("codeqlEnabled") Boolean codeqlEnabled,
    /** Whether code-review validation is enabled. */
    @JsonProperty("codeReviewEnabled") Boolean codeReviewEnabled,
    /** Model used for code-review validation. */
    @JsonProperty("codeReviewModel") String codeReviewModel,
    /** Whether advisory validation is enabled. */
    @JsonProperty("advisoryEnabled") Boolean advisoryEnabled,
    /** Whether secret-scanning validation is enabled. */
    @JsonProperty("secretScanningEnabled") Boolean secretScanningEnabled,
    /** Whether the memory-store tool is enabled. */
    @JsonProperty("memoryStoreEnabled") Boolean memoryStoreEnabled,
    /** Whether the memory-vote tool is enabled. */
    @JsonProperty("memoryVoteEnabled") Boolean memoryVoteEnabled
) {
}
