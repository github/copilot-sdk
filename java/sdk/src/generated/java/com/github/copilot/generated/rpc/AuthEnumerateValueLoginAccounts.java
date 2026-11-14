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
 * Variant {@code loginAccounts} of {@link AuthEnumerateValue}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthEnumerateValueLoginAccounts extends AuthEnumerateValue {

    @JsonProperty("kind")
    private final String kind = "loginAccounts";

    @Override
    public String getKind() { return kind; }

    /** Cached base identities reusable by the selected login provider, active first. */
    @JsonProperty("items")
    private List<AuthLoginIdentity> items;

    public List<AuthLoginIdentity> getItems() { return items; }
    public void setItems(List<AuthLoginIdentity> items) { this.items = items; }
}
