/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Shell-aware path hints for a shell tool's command, captured at start time so consumers can snapshot a file's pre-image before the tool runs.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionStartShellToolInfo(
    /** File paths the command may read or write, derived from the command at start time. Produced by the same shell-aware extractor as PermissionRequestShell.possiblePaths, so it is present even when the command is auto-approved and no permission request fires. */
    @JsonProperty("possiblePaths") List<String> possiblePaths,
    /** Whether the command includes a file write redirection (e.g., > or >>). */
    @JsonProperty("hasWriteFileRedirection") Boolean hasWriteFileRedirection,
    /** The command with a redundant leading `cd` into the working directory removed, present only when there was one to remove. Computed with the same routine the shell driver applies before spawning, so a surface that renders this shows the text that actually runs. Consumers that display it should keep the original tool arguments available on demand. */
    @CopilotExperimental
    @JsonProperty("displayCommand") String displayCommand
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionStartShellToolInfo {}

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
        private List<String> possiblePaths;
        private Boolean hasWriteFileRedirection;
        private String displayCommand;

        /**
         * Sets the {@code possiblePaths} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder possiblePaths(List<String> value) {
            this.possiblePaths = value;
            return this;
        }

        /**
         * Sets the {@code hasWriteFileRedirection} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hasWriteFileRedirection(Boolean value) {
            this.hasWriteFileRedirection = value;
            return this;
        }

        /**
         * Sets the {@code displayCommand} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder displayCommand(String value) {
            this.displayCommand = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolExecutionStartShellToolInfo build() {
            if (possiblePaths == null) throw new IllegalStateException("Missing required property: possiblePaths");
            if (hasWriteFileRedirection == null) throw new IllegalStateException("Missing required property: hasWriteFileRedirection");
            return new ToolExecutionStartShellToolInfo(possiblePaths, hasWriteFileRedirection, displayCommand);
        }
    }
}
