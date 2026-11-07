/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Current host context
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppsHostContextDetails(
    /** UI theme preference per SEP-1865 */
    @JsonProperty("theme") McpAppsHostContextDetailsTheme theme,
    /** BCP-47 locale, e.g. 'en-US' */
    @JsonProperty("locale") String locale,
    /** IANA timezone, e.g. 'America/New_York' */
    @JsonProperty("timeZone") String timeZone,
    /** Current display mode (SEP-1865) */
    @JsonProperty("displayMode") McpAppsHostContextDetailsDisplayMode displayMode,
    /** Display modes the host supports */
    @JsonProperty("availableDisplayModes") List<McpAppsHostContextDetailsAvailableDisplayMode> availableDisplayModes,
    /** Platform type for responsive design */
    @JsonProperty("platform") McpAppsHostContextDetailsPlatform platform,
    /** Host application identifier */
    @JsonProperty("userAgent") String userAgent
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpAppsHostContextDetails {}

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
        private McpAppsHostContextDetailsTheme theme;
        private String locale;
        private String timeZone;
        private McpAppsHostContextDetailsDisplayMode displayMode;
        private List<McpAppsHostContextDetailsAvailableDisplayMode> availableDisplayModes;
        private McpAppsHostContextDetailsPlatform platform;
        private String userAgent;

        /**
         * Sets the {@code theme} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder theme(McpAppsHostContextDetailsTheme value) {
            this.theme = value;
            return this;
        }

        /**
         * Sets the {@code locale} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder locale(String value) {
            this.locale = value;
            return this;
        }

        /**
         * Sets the {@code timeZone} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timeZone(String value) {
            this.timeZone = value;
            return this;
        }

        /**
         * Sets the {@code displayMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayMode(McpAppsHostContextDetailsDisplayMode value) {
            this.displayMode = value;
            return this;
        }

        /**
         * Sets the {@code availableDisplayModes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder availableDisplayModes(List<McpAppsHostContextDetailsAvailableDisplayMode> value) {
            this.availableDisplayModes = value;
            return this;
        }

        /**
         * Sets the {@code platform} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder platform(McpAppsHostContextDetailsPlatform value) {
            this.platform = value;
            return this;
        }

        /**
         * Sets the {@code userAgent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userAgent(String value) {
            this.userAgent = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpAppsHostContextDetails build() {
            return new McpAppsHostContextDetails(theme, locale, timeZone, displayMode, availableDisplayModes, platform, userAgent);
        }
    }
}
