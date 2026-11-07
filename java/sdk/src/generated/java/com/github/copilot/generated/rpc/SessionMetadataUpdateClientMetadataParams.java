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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Atomic patch for client-owned session metadata. Operations apply in clear, remove, then set order. The resulting bag must satisfy the ClientMetadata entry and serialized-size limits. Local storage coordinates concurrent runtime processes; custom SessionFs providers must serialize writers that access the same session from multiple processes.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataUpdateClientMetadataParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Remove every existing client metadata entry before applying remove and set. Defaults to false. */
    @JsonProperty("clear") Boolean clear,
    /** Case-sensitive keys to remove. Missing keys are ignored. Each key must be non-empty, at most 256 UTF-8 bytes, and outside the reserved `copilot/` and `github/` namespaces. */
    @JsonProperty("remove") List<String> remove,
    /** String entries to add or replace. Set wins when a key also appears in remove. Each key must be non-empty, at most 256 UTF-8 bytes, and outside the reserved `copilot/` and `github/` namespaces. Each value may contain at most 16 KiB of UTF-8 data. */
    @JsonProperty("set") Map<String, String> set
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMetadataUpdateClientMetadataParams {}

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
        private Boolean clear;
        private List<String> remove;
        private Map<String, String> set;

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
         * Sets the {@code clear} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clear(Boolean value) {
            this.clear = value;
            return this;
        }

        /**
         * Sets the {@code remove} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder remove(List<String> value) {
            this.remove = value;
            return this;
        }

        /**
         * Sets the {@code set} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder set(Map<String, String> value) {
            this.set = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMetadataUpdateClientMetadataParams build() {
            return new SessionMetadataUpdateClientMetadataParams(sessionId, clear, remove, set);
        }
    }
}
