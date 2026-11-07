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
 * Variant {@code mcp} of {@link InstallationReview}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class InstallationReviewMcp extends InstallationReview {

    @JsonProperty("resource")
    private final String resource = "mcp";

    @Override
    public String getResource() { return resource; }

    /** The exact MCP action and its reviewed changes. */
    @JsonProperty("review")
    private McpInstallationReview review;

    public McpInstallationReview getReview() { return review; }
    public void setReview(McpInstallationReview review) { this.review = review; }


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
        private McpInstallationReview review;

        /**
         * Sets the {@code review} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder review(McpInstallationReview value) {
            this.review = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public InstallationReviewMcp build() {
            if (review == null) throw new IllegalStateException("Missing required property: review");
            InstallationReviewMcp instance = new InstallationReviewMcp();
            instance.setReview(review);
            return instance;
        }
    }
}
