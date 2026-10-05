/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Variant {@code ask_user} of {@link HumanResponseRecordedResponse}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HumanResponseRecordedResponseAskUser extends HumanResponseRecordedResponse {

    @JsonProperty("responseKind")
    private final String responseKind = "ask_user";

    @Override
    public String getResponseKind() { return responseKind; }

    /** Exact answer content accepted from the user. */
    @JsonProperty("content")
    private Map<String, Object> content;

    /** Exact question displayed to the user. */
    @JsonProperty("message")
    private String message;

    /** Exact response schema displayed to the user. */
    @JsonProperty("requestedSchema")
    private ElicitationRequestedSchema requestedSchema;

    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ElicitationRequestedSchema getRequestedSchema() { return requestedSchema; }
    public void setRequestedSchema(ElicitationRequestedSchema requestedSchema) { this.requestedSchema = requestedSchema; }
}
