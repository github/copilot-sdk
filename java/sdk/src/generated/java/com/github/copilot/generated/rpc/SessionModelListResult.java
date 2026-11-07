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
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The list of models available to this session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelListResult(
    /** Available models, ordered with the most preferred default first. Includes both Copilot (CAPI) models and any registry BYOK models; a BYOK model appears under its provider-qualified selection id (`provider/id`). */
    @JsonProperty("list") List<Object> list,
    /** The model providers available to this session, in ordering order, resolved from the account roster; empty when no provider is entitled (logged out / seatless). Each model in `list` carries its own provider reference; this roster gives the deterministic provider sequence and lets a consumer group by provider without deriving ordering from the model list. Central code never branches on a provider kind. */
    @JsonProperty("providers") List<ModelProviderDescriptor> providers,
    /** Cost categories for the full CAPI catalog, including picker-disabled models that Auto may select. Metadata only; entries absent from `list` are not manually selectable. */
    @JsonProperty("modelPriceCategories") List<SessionModelPriceCategory> modelPriceCategories,
    /** Per-quota snapshots returned alongside the model list, keyed by quota type. */
    @JsonProperty("quotaSnapshots") Map<String, Object> quotaSnapshots,
    /** Ordered Auto routing preferences discovered for this session's account. */
    @JsonProperty("auto") AutoTierMetadata auto
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelListResult {}

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
        private List<Object> list;
        private List<ModelProviderDescriptor> providers;
        private List<SessionModelPriceCategory> modelPriceCategories;
        private Map<String, Object> quotaSnapshots;
        private AutoTierMetadata auto;

        /**
         * Sets the {@code list} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder list(List<Object> value) {
            this.list = value;
            return this;
        }

        /**
         * Sets the {@code providers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder providers(List<ModelProviderDescriptor> value) {
            this.providers = value;
            return this;
        }

        /**
         * Sets the {@code modelPriceCategories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelPriceCategories(List<SessionModelPriceCategory> value) {
            this.modelPriceCategories = value;
            return this;
        }

        /**
         * Sets the {@code quotaSnapshots} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder quotaSnapshots(Map<String, Object> value) {
            this.quotaSnapshots = value;
            return this;
        }

        /**
         * Sets the {@code auto} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder auto(AutoTierMetadata value) {
            this.auto = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModelListResult build() {
            if (list == null) throw new IllegalStateException("Missing required property: list");
            return new SessionModelListResult(list, providers, modelPriceCategories, quotaSnapshots, auto);
        }
    }
}
