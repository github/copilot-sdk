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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "session.binary_asset". Canonical bytes for a content-addressed binary asset shared by reference across events
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionBinaryAssetEvent extends SessionEvent {

    @Override
    public String getType() { return "session.binary_asset"; }

    @JsonProperty("data")
    private SessionBinaryAssetEventData data;

    public SessionBinaryAssetEventData getData() { return data; }
    public void setData(SessionBinaryAssetEventData data) { this.data = data; }

    /** Data payload for {@link SessionBinaryAssetEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionBinaryAssetEventData(
        /** Content-addressed id for this binary asset (e.g. "sha256:..."). */
        @JsonProperty("assetId") String assetId,
        /** Binary asset type discriminator. Use "image" for images and "resource" otherwise. */
        @JsonProperty("type") BinaryAssetType type,
        /** MIME type of the binary asset */
        @JsonProperty("mimeType") String mimeType,
        /** Decoded byte length of the binary asset */
        @JsonProperty("byteLength") Long byteLength,
        /** Base64-encoded binary data */
        @JsonProperty("data") String data,
        /** Human-readable description of the binary data */
        @JsonProperty("description") String description,
        /** Optional metadata from the producing tool. */
        @JsonProperty("metadata") Map<String, Object> metadata
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionBinaryAssetEventData {}

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
            private String assetId;
            private BinaryAssetType type;
            private String mimeType;
            private Long byteLength;
            private String data;
            private String description;
            private Map<String, Object> metadata;

            /**
             * Sets the {@code assetId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder assetId(String value) {
                this.assetId = value;
                return this;
            }

            /**
             * Sets the {@code type} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder type(BinaryAssetType value) {
                this.type = value;
                return this;
            }

            /**
             * Sets the {@code mimeType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mimeType(String value) {
                this.mimeType = value;
                return this;
            }

            /**
             * Sets the {@code byteLength} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder byteLength(Long value) {
                this.byteLength = value;
                return this;
            }

            /**
             * Sets the {@code data} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder data(String value) {
                this.data = value;
                return this;
            }

            /**
             * Sets the {@code description} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder description(String value) {
                this.description = value;
                return this;
            }

            /**
             * Sets the {@code metadata} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder metadata(Map<String, Object> value) {
                this.metadata = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionBinaryAssetEventData build() {
                if (assetId == null) throw new IllegalStateException("Missing required property: assetId");
                if (type == null) throw new IllegalStateException("Missing required property: type");
                if (mimeType == null) throw new IllegalStateException("Missing required property: mimeType");
                if (byteLength == null) throw new IllegalStateException("Missing required property: byteLength");
                if (data == null) throw new IllegalStateException("Missing required property: data");
                return new SessionBinaryAssetEventData(assetId, type, mimeType, byteLength, data, description, metadata);
            }
        }
    }
}
