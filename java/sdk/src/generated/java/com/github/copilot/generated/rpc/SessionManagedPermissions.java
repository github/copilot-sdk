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
 * Enterprise permission policy expressed with the runtime's managed permission-rule syntax.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionManagedPermissions(
    /** When set to `disable`, prevents bypass/allow-all permission modes. */
    @JsonProperty("disableBypassPermissionsMode") DisableBypassPermissionsMode disableBypassPermissionsMode,
    /** Permission rules that block matching operations. Deny has highest precedence. */
    @JsonProperty("deny") List<String> deny,
    /** Permission rules that require explicit human approval. */
    @JsonProperty("ask") List<String> ask,
    /** Permission rules that allow matching operations unless another managed source, deny, or ask rule restricts them. */
    @JsonProperty("allow") List<String> allow
) {
}
