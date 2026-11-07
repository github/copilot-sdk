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
import javax.annotation.processing.Generated;

/**
 * Extensions discovered from persisted Copilot home state and their effective loading mode. Launch-scoped additional plugins are not included.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtensionsDiscoverResult(
    /** Discovered user and enabled installed-plugin extensions from persisted Copilot home state */
    @JsonProperty("extensions") List<DiscoveredExtension> extensions,
    /** Effective extension loading mode. Defaults to load_and_augment when unset. */
    @JsonProperty("mode") DiscoveredExtensionMode mode
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ExtensionsDiscoverResult {}

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
        private List<DiscoveredExtension> extensions;
        private DiscoveredExtensionMode mode;

        /**
         * Sets the {@code extensions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder extensions(List<DiscoveredExtension> value) {
            this.extensions = value;
            return this;
        }

        /**
         * Sets the {@code mode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mode(DiscoveredExtensionMode value) {
            this.mode = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ExtensionsDiscoverResult build() {
            if (extensions == null) throw new IllegalStateException("Missing required property: extensions");
            if (mode == null) throw new IllegalStateException("Missing required property: mode");
            return new ExtensionsDiscoverResult(extensions, mode);
        }
    }
}
