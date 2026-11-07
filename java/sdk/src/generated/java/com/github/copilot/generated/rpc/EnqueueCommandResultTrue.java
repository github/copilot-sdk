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
 * Variant {@code true} of {@link EnqueueCommandResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class EnqueueCommandResultTrue extends EnqueueCommandResult {

    @JsonProperty("queued")
    private final String queued = "true";

    @Override
    public String getQueued() { return queued; }

    /** Stable opaque ID of the queued command. */
    @JsonProperty("queueId")
    private String queueId;

    public String getQueueId() { return queueId; }
    public void setQueueId(String queueId) { this.queueId = queueId; }


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
        private String queueId;

        /**
         * Sets the {@code queueId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder queueId(String value) {
            this.queueId = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public EnqueueCommandResultTrue build() {
            if (queueId == null) throw new IllegalStateException("Missing required property: queueId");
            EnqueueCommandResultTrue instance = new EnqueueCommandResultTrue();
            instance.setQueueId(queueId);
            return instance;
        }
    }
}
