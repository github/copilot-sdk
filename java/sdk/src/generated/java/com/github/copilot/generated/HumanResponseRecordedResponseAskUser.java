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


    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private Map<String, Object> content;
        private String message;
        private ElicitationRequestedSchema requestedSchema;

        /**
         * Sets the {@code content} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder content(Map<String, Object> value) {
            this.content = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Sets the {@code requestedSchema} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestedSchema(ElicitationRequestedSchema value) {
            this.requestedSchema = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HumanResponseRecordedResponseAskUser build() {
            if (content == null) throw new IllegalStateException("Missing required property: content");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            if (requestedSchema == null) throw new IllegalStateException("Missing required property: requestedSchema");
            HumanResponseRecordedResponseAskUser instance = new HumanResponseRecordedResponseAskUser();
            instance.setContent(content);
            instance.setMessage(message);
            instance.setRequestedSchema(requestedSchema);
            return instance;
        }
    }
}
