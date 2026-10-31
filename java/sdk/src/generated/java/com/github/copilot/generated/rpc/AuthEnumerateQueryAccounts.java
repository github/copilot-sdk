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
 * Variant {@code accounts} of {@link AuthEnumerateQuery}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthEnumerateQueryAccounts extends AuthEnumerateQuery {

    @JsonProperty("kind")
    private final String kind = "accounts";

    @Override
    public String getKind() { return kind; }

    /** Whether to discard the cached inventory and re-run credential reads and availability checks for every account before returning the canonical account projection. */
    @JsonProperty("refresh")
    private Boolean refresh;

    public Boolean getRefresh() { return refresh; }
    public void setRefresh(Boolean refresh) { this.refresh = refresh; }
}
