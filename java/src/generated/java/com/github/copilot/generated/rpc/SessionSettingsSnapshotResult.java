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
 * Redacted, serializable view of session runtime settings for SDK boundary consumers. Secrets and raw feature flags are intentionally excluded.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSettingsSnapshotResult(
    @JsonProperty("version") String version,
    @JsonProperty("clientName") String clientName,
    @JsonProperty("timeoutMs") Double timeoutMs,
    @JsonProperty("startTimeMs") Double startTimeMs,
    @JsonProperty("repo") SessionSettingsRepoSnapshot repo,
    @JsonProperty("model") SessionSettingsModelSnapshot model,
    @JsonProperty("validation") SessionSettingsValidationSnapshot validation,
    @JsonProperty("job") SessionSettingsJobSnapshot job,
    @JsonProperty("onlineEvaluation") SessionSettingsOnlineEvaluationSnapshot onlineEvaluation
) {
}
