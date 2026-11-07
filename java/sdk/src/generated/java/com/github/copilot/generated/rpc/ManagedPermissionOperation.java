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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedPermissionOperation(
    /** Absolute HTTP(S) URL with a host. Invalid URLs reject the entire evaluation batch. */
    @JsonProperty("url") String url,
    /** Operation to evaluate. Currently only url is supported. */
    @JsonProperty("kind") ManagedPermissionOperationKind kind
) {

    /** Operation to evaluate. Currently only url is supported. */
    public enum ManagedPermissionOperationKind {
        /** The {@code url} variant. */
        URL("url");

        private final String value;
        ManagedPermissionOperationKind(String value) { this.value = value; }
        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() { return value; }
        @com.fasterxml.jackson.annotation.JsonCreator
        public static ManagedPermissionOperationKind fromValue(String value) {
            for (ManagedPermissionOperationKind v : values()) {
                if (v.value.equals(value)) return v;
            }
            throw new IllegalArgumentException("Unknown ManagedPermissionOperationKind value: " + value);
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedPermissionOperation {}

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
        private String url;
        private ManagedPermissionOperationKind kind;

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(ManagedPermissionOperationKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedPermissionOperation build() {
            if (url == null) throw new IllegalStateException("Missing required property: url");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            return new ManagedPermissionOperation(url, kind);
        }
    }
}
