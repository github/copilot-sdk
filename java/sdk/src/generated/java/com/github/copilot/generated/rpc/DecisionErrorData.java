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
 * `data` of a JSON-RPC error returned by a `decisions.*` method.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionErrorData(
    /** Error category to branch on. */
    @JsonProperty("kind") DecisionErrorKind kind,
    /** Every limit or rule the request broke, for `invalidRequest` and `tooLarge`. */
    @JsonProperty("violations") List<DecisionViolation> violations,
    /** HTTP status the backend returned, when the error came from it. */
    @JsonProperty("statusCode") Long statusCode,
    /** How long to wait before retrying, for `rateLimited`. */
    @JsonProperty("retryAfterMs") Long retryAfterMs
) {
}
