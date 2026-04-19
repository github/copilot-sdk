/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Request parameters for the {@code session.ui.elicitation} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiElicitationParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Message describing what information is needed from the user */
    @JsonProperty("message") String message,
    /** JSON Schema describing the form fields to present to the user */
    @JsonProperty("requestedSchema") SessionUiElicitationParamsRequestedSchema requestedSchema
) {

    /** JSON Schema describing the form fields to present to the user */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionUiElicitationParamsRequestedSchema(
        /** Schema type indicator (always 'object') */
        @JsonProperty("type") String type,
        /** Form field definitions, keyed by field name */
        @JsonProperty("properties") Map<String, Object> properties,
        /** List of required field names */
        @JsonProperty("required") List<String> required
    ) {
    }
}
