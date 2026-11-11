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
 * A validated live IDE selection.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdeSelection(
    /** Selected document text. */
    @JsonProperty("text") String text,
    /** Document path on the runtime host. */
    @JsonProperty("filePath") String filePath,
    /** Document URL supplied by the IDE. */
    @JsonProperty("fileUrl") String fileUrl,
    /** Selection endpoints and empty state. */
    @JsonProperty("selection") IdeSelectionRange selection,
    /** Whether the selection is current. */
    @JsonProperty("current") Boolean current
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public IdeSelection {}

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
        private String text;
        private String filePath;
        private String fileUrl;
        private IdeSelectionRange selection;
        private Boolean current;

        /**
         * Sets the {@code text} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder text(String value) {
            this.text = value;
            return this;
        }

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
        public Builder selection(IdeSelectionRange value) {
            this.selection = value;
            return this;
        }

        /**
         * Sets the {@code current} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder current(Boolean value) {
            this.current = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public IdeSelection build() {
            if (text == null) throw new IllegalStateException("Missing required property: text");
            if (filePath == null) throw new IllegalStateException("Missing required property: filePath");
            if (fileUrl == null) throw new IllegalStateException("Missing required property: fileUrl");
            if (selection == null) throw new IllegalStateException("Missing required property: selection");
            return new IdeSelection(text, filePath, fileUrl, selection, current);
        }
    }
}
