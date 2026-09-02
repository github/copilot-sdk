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
import javax.annotation.processing.Generated;

/**
 * An optional catalog authentication exchange did not establish the caller's identity. Anonymous search remains supported; this refusal is reserved for an operation that cannot continue after the attempted exchange. It is distinct from `policy-rejected` and from a network failure, and the reason identifies the recovery action.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogAuthenticationRequiredError extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "authentication-required";

    @Override
    public String getKind() { return kind; }

    /** Why authentication failed. Only an expired credential justifies attempting a silent refresh; an absent or rejected credential requires sign-in. */
    @JsonProperty("reason")
    private CatalogAuthenticationRequiredReason reason;

    /** Human-readable explanation, safe to surface. Never contains a credential or token, nor a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogAuthenticationRequiredReason getReason() { return reason; }
    public void setReason(CatalogAuthenticationRequiredReason reason) { this.reason = reason; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
