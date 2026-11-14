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
 * An operation supported by a model-provider adapter.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderAdapterOperationDescriptor(
    /** Supported operation name: `discover`, `getStatus`, or `models.list`. Unknown names and duplicate declarations are rejected. */
    @JsonProperty("name") String name,
    /** Optional self-contained JSON Schema Draft 7 for non-null discovery input. Only supported on discover. No external references are resolved. Omitted or null input selects defaults when requiresInput is false. Without a schema, the adapter validates supplied input. */
    @JsonProperty("inputSchema") Object inputSchema
) {
}
