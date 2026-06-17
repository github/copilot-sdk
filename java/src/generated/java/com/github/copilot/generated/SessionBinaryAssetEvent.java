/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "session.binary_asset". Canonical bytes for a content-addressed binary asset shared by reference across events
 * @since 1.0.0
 */
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
    }
}
