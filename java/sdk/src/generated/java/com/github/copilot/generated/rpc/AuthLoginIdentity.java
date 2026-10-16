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
 * A cached base identity a provider can reuse for login without asking the user to choose an account again.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthLoginIdentity(
    /** Opaque provider-owned identity id. Pass it back to accounts.login.begin; consumers must not parse it. */
    @JsonProperty("accountId") String accountId,
    /** Human-readable sign-in label owned by the runtime. */
    @JsonProperty("label") String label,
    /** Whether this base identity owns the session's active derived account. */
    @JsonProperty("active") Boolean active
) {
}
