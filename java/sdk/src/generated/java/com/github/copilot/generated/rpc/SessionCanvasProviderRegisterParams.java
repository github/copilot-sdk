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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Internal canvas provider registration parameters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionCanvasProviderRegisterParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Connection identifier for callback routing */
    @JsonProperty("connectionId") String connectionId,
    /** Provider metadata supplied by the host */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("info") Object info,
    /** Canvas contributions supplied by the provider */
    @JsonProperty("canvases") List<Object> canvases
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionCanvasProviderRegisterParams {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String sessionId;
        private String connectionId;
        private Object info;
        private boolean $presence2;
        private List<Object> canvases;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code connectionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder connectionId(String value) {
            this.connectionId = value;
            return this;
        }

        /**
         * Sets the {@code info} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder info(Object value) {
            this.info = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code canvases} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder canvases(List<Object> value) {
            this.canvases = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionCanvasProviderRegisterParams build() {
            if (connectionId == null) throw new IllegalStateException("Missing required property: connectionId");
            if (!$presence2) throw new IllegalStateException("Missing required property: info");
            if (canvases == null) throw new IllegalStateException("Missing required property: canvases");
            return new SessionCanvasProviderRegisterParams(sessionId, connectionId, info, canvases);
        }
    }
}
