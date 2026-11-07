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
 * Request parameters for the {@code models.listTokenPriceUnits} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelsListTokenPriceUnitsParams(
    /** Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used. */
    @JsonProperty("selectionId") String selectionId,
    /** GitHub token accepted for compatibility with existing SDK clients. When provided, resolves this token instead of using the current account. */
    @JsonProperty("gitHubToken") String gitHubToken,
    /** When true, replaces the display names of preview and hidden models with `Hidden Model N`, and masks their promotion and data-retention text. Model ids are unchanged. */
    @JsonProperty("streamerMode") Boolean streamerMode
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelsListTokenPriceUnitsParams {}

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
        private String selectionId;
        private String gitHubToken;
        private Boolean streamerMode;

        /**
         * Sets the {@code selectionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectionId(String value) {
            this.selectionId = value;
            return this;
        }

        /**
         * Sets the {@code gitHubToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder gitHubToken(String value) {
            this.gitHubToken = value;
            return this;
        }

        /**
         * Sets the {@code streamerMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder streamerMode(Boolean value) {
            this.streamerMode = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelsListTokenPriceUnitsParams build() {
            return new ModelsListTokenPriceUnitsParams(selectionId, gitHubToken, streamerMode);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param selectionId Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used.
     * @param gitHubToken GitHub token accepted for compatibility with existing SDK clients. When provided, resolves this token instead of using the current account.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelsListTokenPriceUnitsParams(
        String selectionId,
        String gitHubToken
    ) {
        this(selectionId, gitHubToken, null);
    }
}
