/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The protocol version and capability set the runtime actually honoured for a successful catalog operation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogNegotiatedContract(
    /** Protocol version of the runtime that served the request. */
    @JsonProperty("runtimeProtocolVersion") Long runtimeProtocolVersion,
    /** Wire features the runtime understood for this operation. Always a superset of the caller's required features, because any shortfall is a refusal instead. Operation availability remains a separate typed result. */
    @JsonProperty("grantedCapabilities") List<CatalogCapability> grantedCapabilities
) {
}
