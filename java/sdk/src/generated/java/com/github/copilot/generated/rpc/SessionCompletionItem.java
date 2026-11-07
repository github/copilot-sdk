/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * A single host-driven completion. Accepting an item replaces `[rangeStart, rangeEnd)` (UTF-16 code units) in the composer with `insertText`; when the range is absent, the active token around the cursor is replaced.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionCompletionItem(
    /** Text spliced into the composer when the item is accepted. */
    @JsonProperty("insertText") String insertText,
    /** Start of the replacement range in `text`, in UTF-16 code units. */
    @JsonProperty("rangeStart") Long rangeStart,
    /** End (exclusive) of the replacement range in `text`, in UTF-16 code units. */
    @JsonProperty("rangeEnd") Long rangeEnd,
    /** Primary display label for the picker row. Falls back to `insertText` when absent. */
    @JsonProperty("label") String label,
    /** Render-kind hint for the picker row (e.g. `"document"`, `"directory"`), derived from the host's display kind. */
    @JsonProperty("kind") String kind
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionCompletionItem {}

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
        private String insertText;
        private Long rangeStart;
        private Long rangeEnd;
        private String label;
        private String kind;

        /**
         * Sets the {@code insertText} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder insertText(String value) {
            this.insertText = value;
            return this;
        }

        /**
         * Sets the {@code rangeStart} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rangeStart(Long value) {
            this.rangeStart = value;
            return this;
        }

        /**
         * Sets the {@code rangeEnd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rangeEnd(Long value) {
            this.rangeEnd = value;
            return this;
        }

        /**
         * Sets the {@code label} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder label(String value) {
            this.label = value;
            return this;
        }

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionCompletionItem build() {
            if (insertText == null) throw new IllegalStateException("Missing required property: insertText");
            return new SessionCompletionItem(insertText, rangeStart, rangeEnd, label, kind);
        }
    }
}
