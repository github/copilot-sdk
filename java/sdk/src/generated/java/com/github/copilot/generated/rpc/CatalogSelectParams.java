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
 * Terminates one retained catalog selection group through an opaque reference previously returned by the model-safe search projection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSelectParams(
    /** Protocol version and capabilities the caller requires. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Locally owned root session whose retained search state is being resolved. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque runtime-instance scoped reference to one visible candidate. For a non-selected outcome, any candidate reference from the same search closes that search's retained group. */
    @JsonProperty("selectionRef") String selectionRef,
    /** The terminal outcome declared by the caller. Timed-out means the host's live interaction deadline elapsed; a reference whose runtime TTL elapsed is rejected separately as stale. */
    @JsonProperty("outcome") CatalogSelectionDecision outcome
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogSelectParams {}

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
        private CatalogClientContract contract;
        private String sessionId;
        private String selectionRef;
        private CatalogSelectionDecision outcome;

        /**
         * Sets the {@code contract} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contract(CatalogClientContract value) {
            this.contract = value;
            return this;
        }

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code selectionRef} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectionRef(String value) {
            this.selectionRef = value;
            return this;
        }

        /**
         * Sets the {@code outcome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outcome(CatalogSelectionDecision value) {
            this.outcome = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogSelectParams build() {
            if (contract == null) throw new IllegalStateException("Missing required property: contract");
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (selectionRef == null) throw new IllegalStateException("Missing required property: selectionRef");
            if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
            return new CatalogSelectParams(contract, sessionId, selectionRef, outcome);
        }
    }
}
