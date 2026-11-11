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
 * A response byte range, or EOF. EOF and transport failures dispose the handle.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FetchReadResult(
    /** True at EOF. Do not read the handle again after EOF. */
    @JsonProperty("done") Boolean done,
    /** Response bytes encoded as standard base64. Empty at EOF. */
    @JsonProperty("body") String body,
    /** At EOF, whether TLS did not attest to the end of a close-delimited event stream. */
    @JsonProperty("endedAtUnverifiedEof") Boolean endedAtUnverifiedEof
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FetchReadResult {}

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
        private Boolean done;
        private String body;
        private Boolean endedAtUnverifiedEof;

        /**
         * Sets the {@code done} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder done(Boolean value) {
            this.done = value;
            return this;
        }

        /**
         * Sets the {@code body} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder body(String value) {
            this.body = value;
            return this;
        }

        /**
         * Sets the {@code endedAtUnverifiedEof} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder endedAtUnverifiedEof(Boolean value) {
            this.endedAtUnverifiedEof = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FetchReadResult build() {
            if (done == null) throw new IllegalStateException("Missing required property: done");
            if (body == null) throw new IllegalStateException("Missing required property: body");
            if (endedAtUnverifiedEof == null) throw new IllegalStateException("Missing required property: endedAtUnverifiedEof");
            return new FetchReadResult(done, body, endedAtUnverifiedEof);
        }
    }
}
