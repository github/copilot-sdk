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
 * Redacted, serializable view of session runtime settings for SDK boundary consumers. Secrets and raw feature flags are intentionally excluded.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionSettingsSnapshot(
    /** Agent runtime version selector copied from the session settings, such as `latest` or a runtime release identifier. */
    @JsonProperty("version") String version,
    /** Name of the SDK client that created the session. */
    @JsonProperty("clientName") String clientName,
    /** Session timeout in milliseconds. */
    @JsonProperty("timeoutMs") Double timeoutMs,
    /** Session start time as Unix epoch milliseconds. */
    @JsonProperty("startTimeMs") Double startTimeMs,
    /** Redacted repository and host settings. */
    @JsonProperty("repo") SessionSettingsRepoSnapshot repo,
    /** Redacted model routing settings. */
    @JsonProperty("model") SessionSettingsModelSnapshot model,
    /** Redacted validation and memory-tool settings. */
    @JsonProperty("validation") SessionSettingsValidationSnapshot validation,
    /** Redacted job settings. */
    @JsonProperty("job") SessionSettingsJobSnapshot job,
    /** Online-evaluation settings safe for SDK consumers. */
    @JsonProperty("onlineEvaluation") SessionSettingsOnlineEvaluationSnapshot onlineEvaluation
) {
}
