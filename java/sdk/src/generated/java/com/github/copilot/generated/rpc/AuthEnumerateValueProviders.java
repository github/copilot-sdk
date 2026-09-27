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
 * Variant {@code providers} of {@link AuthEnumerateValue}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthEnumerateValueProviders extends AuthEnumerateValue {

    @JsonProperty("kind")
    private final String kind = "providers";

    @Override
    public String getKind() { return kind; }

    /** The providers offered for interactive login. */
    @JsonProperty("items")
    private List<ProviderDescriptor> items;

    public List<ProviderDescriptor> getItems() { return items; }
    public void setItems(List<ProviderDescriptor> items) { this.items = items; }
}
