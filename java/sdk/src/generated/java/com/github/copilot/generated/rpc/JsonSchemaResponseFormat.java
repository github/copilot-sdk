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
 * A JSON Schema output contract. OpenAI receives the name, description, schema and strict setting; Anthropic receives the schema in output_config.format and always uses its native strict enforcement.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonSchemaResponseFormat(
    /** Name of the output schema, subject to the provider's naming restrictions. */
    @JsonProperty("name") String name,
    /** JSON Schema passed unchanged to the inference provider. Schemas larger than 32 MiB when JSON-encoded are rejected before admission, using the runtime's existing request-size ceiling. This is not a guarantee that the entire model request fits. Supported keywords and schema restrictions are determined by the provider. */
    @JsonProperty("schema") Object schema,
    /** Optional description passed to OpenAI providers. */
    @JsonProperty("description") String description,
    /** Optional strict enforcement setting for OpenAI providers. Omitted uses the provider default. Anthropic always enforces its supported schema subset. */
    @JsonProperty("strict") Boolean strict
) {
}
