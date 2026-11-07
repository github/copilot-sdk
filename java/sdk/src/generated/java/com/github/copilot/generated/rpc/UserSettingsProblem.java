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
 * One setting that will not take effect, and why.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsProblem(
    /** Stable identifier for this problem within the reported set, so a caller can track a selection across refreshes. */
    @JsonProperty("id") String id,
    /** What is wrong in machine-readable form, for a caller that wants to branch rather than display. */
    @JsonProperty("kind") String kind,
    /** Coarse grouping for the problem, for a caller that presents problems in sections. */
    @JsonProperty("category") String category,
    /** The settings layer the problem was found in. */
    @JsonProperty("source") UserSettingsProblemLocation source,
    /** The offending key as written in the file, when the problem is attributable to a single key. Absent when the whole file is at fault. */
    @JsonProperty("rawKey") String rawKey,
    /** Why the setting will not take effect. Never contains the setting's value, which may be a token or another secret. */
    @JsonProperty("explanation") String explanation
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsProblem {}

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
        private String id;
        private String kind;
        private String category;
        private UserSettingsProblemLocation source;
        private String rawKey;
        private String explanation;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code category} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder category(String value) {
            this.category = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(UserSettingsProblemLocation value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code rawKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rawKey(String value) {
            this.rawKey = value;
            return this;
        }

        /**
         * Sets the {@code explanation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder explanation(String value) {
            this.explanation = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsProblem build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (category == null) throw new IllegalStateException("Missing required property: category");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (explanation == null) throw new IllegalStateException("Missing required property: explanation");
            return new UserSettingsProblem(id, kind, category, source, rawKey, explanation);
        }
    }
}
