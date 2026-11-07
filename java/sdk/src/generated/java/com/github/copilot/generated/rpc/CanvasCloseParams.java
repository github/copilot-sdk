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
 * Canvas close parameters sent to the provider.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CanvasCloseParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Owning provider identifier */
    @JsonProperty("extensionId") String extensionId,
    /** Provider-local canvas identifier */
    @JsonProperty("canvasId") String canvasId,
    /** Canvas instance identifier */
    @JsonProperty("instanceId") String instanceId,
    /** Host context supplied by the runtime. */
    @JsonProperty("host") CanvasHostContext host,
    /** Session context supplied by the runtime. */
    @JsonProperty("session") CanvasSessionContext session
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CanvasCloseParams {}

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
        private String extensionId;
        private String canvasId;
        private String instanceId;
        private CanvasHostContext host;
        private CanvasSessionContext session;

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
         * Sets the {@code extensionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder extensionId(String value) {
            this.extensionId = value;
            return this;
        }

        /**
         * Sets the {@code canvasId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder canvasId(String value) {
            this.canvasId = value;
            return this;
        }

        /**
         * Sets the {@code instanceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder instanceId(String value) {
            this.instanceId = value;
            return this;
        }

        /**
         * Sets the {@code host} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder host(CanvasHostContext value) {
            this.host = value;
            return this;
        }

        /**
         * Sets the {@code session} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder session(CanvasSessionContext value) {
            this.session = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CanvasCloseParams build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (extensionId == null) throw new IllegalStateException("Missing required property: extensionId");
            if (canvasId == null) throw new IllegalStateException("Missing required property: canvasId");
            if (instanceId == null) throw new IllegalStateException("Missing required property: instanceId");
            return new CanvasCloseParams(sessionId, extensionId, canvasId, instanceId, host, session);
        }
    }
}
