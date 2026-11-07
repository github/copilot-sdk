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
 * Parameters for creating a new local session.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionsOpenCreate extends SessionsOpenParams {

    @JsonProperty("kind")
    private final String kind = "create";

    @Override
    public String getKind() { return kind; }

    /** Session construction options. */
    @JsonProperty("options")
    private SessionOpenOptions options;

    /** Whether to emit session.start during creation. Defaults to true. */
    @JsonProperty("emitStart")
    private Boolean emitStart;

    public SessionOpenOptions getOptions() { return options; }
    public void setOptions(SessionOpenOptions options) { this.options = options; }

    public Boolean getEmitStart() { return emitStart; }
    public void setEmitStart(Boolean emitStart) { this.emitStart = emitStart; }


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
        private SessionOpenOptions options;
        private Boolean emitStart;

        /**
         * Sets the {@code options} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder options(SessionOpenOptions value) {
            this.options = value;
            return this;
        }

        /**
         * Sets the {@code emitStart} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder emitStart(Boolean value) {
            this.emitStart = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsOpenCreate build() {
            SessionsOpenCreate instance = new SessionsOpenCreate();
            instance.setOptions(options);
            instance.setEmitStart(emitStart);
            return instance;
        }
    }
}
