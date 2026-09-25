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
 * Variant {@code accounts} of {@link AuthEnumerateValue}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthEnumerateValueAccounts extends AuthEnumerateValue {

    @JsonProperty("kind")
    private final String kind = "accounts";

    @Override
    public String getKind() { return kind; }

    /** The signed-in account forest; empty when not logged in. */
    @JsonProperty("items")
    private List<AccountStatus> items;

    public List<AccountStatus> getItems() { return items; }
    public void setItems(List<AccountStatus> items) { this.items = items; }
}
