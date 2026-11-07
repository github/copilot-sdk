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
 * One persisted structured system-message block and its cache intent
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SystemMessageContentBlock(
    /** Text content for this system-message block. */
    @JsonProperty("content") String content,
    /** Diagnostic classification indicating whether the block is stable across equivalent sessions. */
    @JsonProperty("isStatic") Boolean isStatic,
    /** Explicit prompt-cache intent. True places a breakpoint after this block, false suppresses one, and absence preserves the provider's legacy default. */
    @JsonProperty("cacheBreakpoint") Boolean cacheBreakpoint
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SystemMessageContentBlock {}

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
        private String content;
        private Boolean isStatic;
        private Boolean cacheBreakpoint;

        /**
         * Sets the {@code content} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder content(String value) {
            this.content = value;
            return this;
        }

        /**
         * Sets the {@code isStatic} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isStatic(Boolean value) {
            this.isStatic = value;
            return this;
        }

        /**
         * Sets the {@code cacheBreakpoint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cacheBreakpoint(Boolean value) {
            this.cacheBreakpoint = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SystemMessageContentBlock build() {
            if (content == null) throw new IllegalStateException("Missing required property: content");
            return new SystemMessageContentBlock(content, isStatic, cacheBreakpoint);
        }
    }
}
