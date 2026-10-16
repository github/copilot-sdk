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
import java.time.OffsetDateTime;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A model offered for agent conversations. Missing capability metadata does not disqualify a candidate. Models known to be incompatible, such as embedding-only models, are excluded by the adapter.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveredModel(
    /** Provider-native model identifier. */
    @JsonProperty("id") String id,
    /** Provider-reported display name. */
    @JsonProperty("name") String name,
    /** Attribution for the adapter that produced this model row. */
    @JsonProperty("provenance") ModelProviderProvenance provenance,
    /** Provider-reported artifact digest. */
    @JsonProperty("digest") String digest,
    /** Provider-reported last-modified timestamp. */
    @JsonProperty("modifiedAt") OffsetDateTime modifiedAt,
    /** Provider-reported artifact size in bytes. */
    @JsonProperty("sizeBytes") Long sizeBytes,
    /** Provider-reported model artifact details. */
    @JsonProperty("details") ModelArtifactDetails details,
    /** Provider-reported model capabilities. Omitted capability fields are unknown; explicit false values are preserved. */
    @JsonProperty("capabilities") ModelCapabilities capabilities,
    /** Non-fatal warnings encountered while enriching this model. */
    @JsonProperty("warnings") List<ModelProviderWarning> warnings
) {
}
