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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlashCommandTimelineEntry(
    /** Timeline entry presentation type. */
    @JsonProperty("type") String type,
    /** Text displayed for the timeline entry. */
    @JsonProperty("text") String text,
    /** Optional URL associated with the timeline entry. */
    @JsonProperty("url") String url,
    /** What the user must do to recover, when the entry reports a failure the runtime knows an action for. The `text` never names a client affordance, so a client that offers one renders it from this value. */
    @JsonProperty("remediation") RemediationAction remediation
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SlashCommandTimelineEntry {}

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
        private String type;
        private String text;
        private String url;
        private RemediationAction remediation;

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(String value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code text} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder text(String value) {
            this.text = value;
            return this;
        }

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Sets the {@code remediation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remediation(RemediationAction value) {
            this.remediation = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SlashCommandTimelineEntry build() {
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (text == null) throw new IllegalStateException("Missing required property: text");
            return new SlashCommandTimelineEntry(type, text, url, remediation);
        }
    }
}
