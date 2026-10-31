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
 * Variant {@code switchActive} of {@link AuthWrite}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthWriteSwitchActive extends AuthWrite {

    @JsonProperty("kind")
    private final String kind = "switchActive";

    @Override
    public String getKind() { return kind; }

    /** Opaque selection id of the account to make active. */
    @JsonProperty("selectionId")
    private String selectionId;

    public String getSelectionId() { return selectionId; }
    public void setSelectionId(String selectionId) { this.selectionId = selectionId; }
}
