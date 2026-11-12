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
 * The runtime-selected image model and its account availability. Availability is independent of per-session image-generation enablement and does not override session policy or permissions.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageToolModel(
    /** Image model identifier selected by the runtime. */
    @JsonProperty("id") String id,
    /** Human-readable model name, supplied by the runtime when unavailable from the catalog. */
    @JsonProperty("name") String name,
    /** Image operations supported by the model's available versioned endpoints. */
    @JsonProperty("supportedOperations") List<ImageToolModelOperation> supportedOperations,
    /** Whether this is the model the runtime currently uses for its native image tool. */
    @JsonProperty("isDefault") Boolean isDefault,
    /** Whether the resolved account can use this image model. This is not the session's enabled setting. */
    @JsonProperty("availability") ImageToolModelAvailability availability,
    /** Catalog-reported token prices. Absent means prices were not reported. */
    @JsonProperty("pricing") ImageToolModelPricing pricing
) {
}
