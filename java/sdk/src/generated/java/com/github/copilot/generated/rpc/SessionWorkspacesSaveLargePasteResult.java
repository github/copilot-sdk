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
 * Descriptor for the saved paste file, or null when the workspace is unavailable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesSaveLargePasteResult(
    /** Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime, non-infinite sessions, remote sessions) */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("saved") SessionWorkspacesSaveLargePasteResultSaved saved
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWorkspacesSaveLargePasteResultSaved(
        /** Absolute filesystem path to the saved paste file */
        @JsonProperty("filePath") String filePath,
        /** Filename within the workspace files directory */
        @JsonProperty("filename") String filename,
        /** Size of the saved file in bytes */
        @JsonProperty("sizeBytes") Long sizeBytes
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionWorkspacesSaveLargePasteResultSaved {}

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
            private String filePath;
            private String filename;
            private Long sizeBytes;

            /**
             * Sets the {@code filePath} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder filePath(String value) {
                this.filePath = value;
                return this;
            }

            /**
             * Sets the {@code filename} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder filename(String value) {
                this.filename = value;
                return this;
            }

            /**
             * Sets the {@code sizeBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder sizeBytes(Long value) {
                this.sizeBytes = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionWorkspacesSaveLargePasteResultSaved build() {
                if (filePath == null) throw new IllegalStateException("Missing required property: filePath");
                if (filename == null) throw new IllegalStateException("Missing required property: filename");
                if (sizeBytes == null) throw new IllegalStateException("Missing required property: sizeBytes");
                return new SessionWorkspacesSaveLargePasteResultSaved(filePath, filename, sizeBytes);
            }
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkspacesSaveLargePasteResult {}

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
        private SessionWorkspacesSaveLargePasteResultSaved saved;
        private boolean $presence0;

        /**
         * Sets the {@code saved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder saved(SessionWorkspacesSaveLargePasteResultSaved value) {
            this.saved = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkspacesSaveLargePasteResult build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: saved");
            return new SessionWorkspacesSaveLargePasteResult(saved);
        }
    }
}
