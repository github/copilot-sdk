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
 * A saved feed's identity. Its version changes only when the feed's definition is replaced, not when a referenced resource changes.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSavedFeedDescription(
    /** Opaque feed ID. */
    @JsonProperty("id") String id,
    /** Feed name as saved by its creator. Inert untrusted text. */
    @JsonProperty("name") String name,
    /** Version of the saved feed definition. */
    @JsonProperty("version") Long version
) {
}
