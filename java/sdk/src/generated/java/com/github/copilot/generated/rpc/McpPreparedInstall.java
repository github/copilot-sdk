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
 * Inert, runtime-owned admission. The operation ID is known before confirmation or effects.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPreparedInstall(
    /** Original connection-owned operation, known before the first confirmation callback. */
    @JsonProperty("operationId") String operationId,
    /** Original plan expiry in Unix epoch milliseconds; preparation does not extend it. */
    @JsonProperty("expiresAtEpochMs") Long expiresAtEpochMs
) {
}
