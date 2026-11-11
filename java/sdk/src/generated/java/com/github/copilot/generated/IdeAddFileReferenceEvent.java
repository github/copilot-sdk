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
 * Session event "ide.add_file_reference". An IDE request to attach a file or selection to the composer.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class IdeAddFileReferenceEvent extends SessionEvent {

    @Override
    public String getType() { return "ide.add_file_reference"; }

    @JsonProperty("data")
    private IdeAddFileReferenceEventData data;

    public IdeAddFileReferenceEventData getData() { return data; }
    public void setData(IdeAddFileReferenceEventData data) { this.data = data; }

    /** Data payload for {@link IdeAddFileReferenceEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IdeAddFileReferenceEventData(
        /** Validated file reference or selected range. */
        @JsonProperty("reference") IdeFileReference reference
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public IdeAddFileReferenceEventData {}

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
            private IdeFileReference reference;

            /**
             * Sets the {@code reference} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reference(IdeFileReference value) {
                this.reference = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public IdeAddFileReferenceEventData build() {
                if (reference == null) throw new IllegalStateException("Missing required property: reference");
                return new IdeAddFileReferenceEventData(reference);
            }
        }
    }
}
