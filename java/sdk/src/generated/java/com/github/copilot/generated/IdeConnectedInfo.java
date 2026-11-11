/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * An IDE connection on the runtime host.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdeConnectedInfo(
    /** IDE display name. */
    @JsonProperty("ideName") String ideName,
    /** Absolute workspace path on the runtime host. */
    @JsonProperty("workspaceFolder") String workspaceFolder,
    /** IDE process identifier on the runtime host. */
    @JsonProperty("pid") Long pid,
    /** IDE lock file path on the runtime host. */
    @JsonProperty("lockFilePath") String lockFilePath,
    /** Registered MCP server name. */
    @JsonProperty("serverName") String serverName
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public IdeConnectedInfo {}

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
        private String ideName;
        private String workspaceFolder;
        private Long pid;
        private String lockFilePath;
        private String serverName;

        /**
         * Sets the {@code ideName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
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
        public Builder lockFilePath(String value) {
            this.lockFilePath = value;
            return this;
        }

        /**
         * Sets the {@code serverName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverName(String value) {
            this.serverName = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public IdeConnectedInfo build() {
            if (ideName == null) throw new IllegalStateException("Missing required property: ideName");
            if (workspaceFolder == null) throw new IllegalStateException("Missing required property: workspaceFolder");
            if (pid == null) throw new IllegalStateException("Missing required property: pid");
            if (lockFilePath == null) throw new IllegalStateException("Missing required property: lockFilePath");
            if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
            return new IdeConnectedInfo(ideName, workspaceFolder, pid, lockFilePath, serverName);
        }
    }
}
