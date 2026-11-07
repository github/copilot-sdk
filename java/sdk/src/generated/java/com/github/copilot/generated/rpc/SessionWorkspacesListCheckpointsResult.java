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
 * Workspace checkpoints in chronological order; empty when the workspace is not enabled.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesListCheckpointsResult(
    /** Workspace checkpoints in chronological order. Empty when workspace is not enabled. */
    @JsonProperty("checkpoints") List<WorkspacesCheckpoints> checkpoints
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkspacesListCheckpointsResult {}

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
        private List<WorkspacesCheckpoints> checkpoints;

        /**
         * Sets the {@code checkpoints} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder checkpoints(List<WorkspacesCheckpoints> value) {
            this.checkpoints = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkspacesListCheckpointsResult build() {
            if (checkpoints == null) throw new IllegalStateException("Missing required property: checkpoints");
            return new SessionWorkspacesListCheckpointsResult(checkpoints);
        }
    }
}
