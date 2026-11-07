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
 * Canonical files and directories where custom instructions can be created so the runtime will recognize them.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstructionsGetDiscoveryPathsResult(
    /** Canonical instruction create/discovery files and directories, in priority order */
    @JsonProperty("paths") List<InstructionDiscoveryPath> paths
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public InstructionsGetDiscoveryPathsResult {}

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
        private List<InstructionDiscoveryPath> paths;

        /**
         * Sets the {@code paths} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder paths(List<InstructionDiscoveryPath> value) {
            this.paths = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public InstructionsGetDiscoveryPathsResult build() {
            if (paths == null) throw new IllegalStateException("Missing required property: paths");
            return new InstructionsGetDiscoveryPathsResult(paths);
        }
    }
}
