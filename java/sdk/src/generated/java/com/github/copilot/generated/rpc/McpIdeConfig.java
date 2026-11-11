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
 * IDE metadata for an existing local MCP endpoint on the runtime host.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpIdeConfig(
    /** IDE display name. */
    @JsonProperty("ideName") String ideName,
    /** Absolute workspace path on the runtime host. */
    @JsonProperty("workspaceFolder") String workspaceFolder,
    /** IDE process identifier. */
    @JsonProperty("pid") Long pid,
    /** Lock file whose removal ends the live connection. */
    @JsonProperty("lockFilePath") String lockFilePath,
    /** Client package version used by the MCP initialize request. */
    @JsonProperty("clientVersion") String clientVersion,
    /** Client process identifier sent to the IDE. */
    @JsonProperty("clientPid") Long clientPid,
    /** Client parent process identifier sent to the IDE. */
    @JsonProperty("parentPid") Long parentPid
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpIdeConfig {}

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
        private String ideName;
        private String workspaceFolder;
        private Long pid;
        private String lockFilePath;
        private String clientVersion;
        private Long clientPid;
        private Long parentPid;

        /**
         * Sets the {@code ideName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ideName(String value) {
            this.ideName = value;
            return this;
        }

        /**
         * Sets the {@code workspaceFolder} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder workspaceFolder(String value) {
            this.workspaceFolder = value;
            return this;
        }

        /**
         * Sets the {@code pid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pid(Long value) {
            this.pid = value;
            return this;
        }

        /**
         * Sets the {@code lockFilePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder lockFilePath(String value) {
            this.lockFilePath = value;
            return this;
        }

        /**
         * Sets the {@code clientVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientVersion(String value) {
            this.clientVersion = value;
            return this;
        }

        /**
         * Sets the {@code clientPid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientPid(Long value) {
            this.clientPid = value;
            return this;
        }

        /**
         * Sets the {@code parentPid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder parentPid(Long value) {
            this.parentPid = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpIdeConfig build() {
            if (ideName == null) throw new IllegalStateException("Missing required property: ideName");
            if (workspaceFolder == null) throw new IllegalStateException("Missing required property: workspaceFolder");
            if (pid == null) throw new IllegalStateException("Missing required property: pid");
            if (lockFilePath == null) throw new IllegalStateException("Missing required property: lockFilePath");
            if (clientVersion == null) throw new IllegalStateException("Missing required property: clientVersion");
            if (clientPid == null) throw new IllegalStateException("Missing required property: clientPid");
            if (parentPid == null) throw new IllegalStateException("Missing required property: parentPid");
            return new McpIdeConfig(ideName, workspaceFolder, pid, lockFilePath, clientVersion, clientPid, parentPid);
        }
    }
}
