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
 * A secret a transport choice needs, referenced by placeholder. No secret value ever appears in a plan, and the placeholder resolves against the keychain only when a plan is applied.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanSecretPlaceholder(
    /** Key the secret is supplied under. Inert untrusted data. */
    @JsonProperty("key") String key,
    /** The runtime-assigned `${secret:<id>}` placeholder written into configuration in place of the value. */
    @JsonProperty("placeholder") String placeholder,
    /** Human-readable label from the card. Inert untrusted text. */
    @JsonProperty("title") String title
) {
}
