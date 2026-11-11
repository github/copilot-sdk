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
 * A validated IDE request to attach a file or selected text.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdeFileReference(
    /** Document path on the runtime host. */
    @JsonProperty("filePath") String filePath,
    /** Document URL supplied by the IDE. */
    @JsonProperty("fileUrl") String fileUrl,
    /** Selected range, or null for a whole file. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("selection") IdeRange selection,
    /** Selected text, or null for a whole file. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("selectedText") String selectedText
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public IdeFileReference {}

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
        private String fileUrl;
        private IdeRange selection;
        private boolean $presence2;
        private String selectedText;
        private boolean $presence3;

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
         * Sets the {@code fileUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder fileUrl(String value) {
            this.fileUrl = value;
            return this;
        }

        /**
         * Sets the {@code selection} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selection(IdeRange value) {
            this.selection = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code selectedText} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectedText(String value) {
            this.selectedText = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public IdeFileReference build() {
            if (filePath == null) throw new IllegalStateException("Missing required property: filePath");
            if (fileUrl == null) throw new IllegalStateException("Missing required property: fileUrl");
            if (!$presence2) throw new IllegalStateException("Missing required property: selection");
            if (!$presence3) throw new IllegalStateException("Missing required property: selectedText");
            return new IdeFileReference(filePath, fileUrl, selection, selectedText);
        }
    }
}
