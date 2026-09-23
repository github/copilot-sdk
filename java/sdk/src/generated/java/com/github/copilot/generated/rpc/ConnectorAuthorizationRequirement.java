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
 * Account-targeted authorization update required by the Connector service. The account ID is an opaque host routing identifier; no credential is included.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorAuthorizationRequirement(
    /** Exact opaque account selection that made the Connector request. */
    @JsonProperty("accountId") String accountId,
    /** Stable OAuth scope the selected account must grant. */
    @JsonProperty("scope") ConnectorAuthorizationScope scope
) {
}
