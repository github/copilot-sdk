/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.OptionalInt;

/**
 * Options for the {@link SessionUiApi#input(String, InputOptions)} convenience
 * method.
 *
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputOptions {

    private String title;
    private String description;
    @JsonProperty("minLength")
    private Integer minLength;
    @JsonProperty("maxLength")
    private Integer maxLength;
    private String format;
    private String defaultValue;

    /** Gets the title label for the input field. @return the title */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title label for the input field. @param title the title @return this
     */
    public InputOptions setTitle(String title) {
        this.title = title;
        return this;
    }

    /** Gets the descriptive text shown below the field. @return the description */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the descriptive text shown below the field. @param description the
     * description @return this
     */
    public InputOptions setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Gets the minimum character length.
     *
     * @return an {@link java.util.OptionalInt} containing the min length, or
     *         {@link java.util.OptionalInt#empty()} if not set
     */
    @JsonIgnore
    public OptionalInt getMinLength() {
        return minLength == null ? OptionalInt.empty() : OptionalInt.of(minLength);
    }

    /**
     * Sets the minimum character length. @param minLength the min length @return
     * this
     */
    public InputOptions setMinLength(int minLength) {
        this.minLength = minLength;
        return this;
    }

    /**
     * Clears the minLength setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public InputOptions clearMinLength() {
        this.minLength = null;
        return this;
    }

    /**
     * Gets the maximum character length.
     *
     * @return an {@link java.util.OptionalInt} containing the max length, or
     *         {@link java.util.OptionalInt#empty()} if not set
     */
    @JsonIgnore
    public OptionalInt getMaxLength() {
        return maxLength == null ? OptionalInt.empty() : OptionalInt.of(maxLength);
    }

    /**
     * Sets the maximum character length. @param maxLength the max length @return
     * this
     */
    public InputOptions setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    /**
     * Clears the maxLength setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public InputOptions clearMaxLength() {
        this.maxLength = null;
        return this;
    }

    /**
     * Gets the semantic format hint (e.g., {@code "email"}, {@code "uri"},
     * {@code "date"}, {@code "date-time"}).
     *
     * @return the format hint
     */
    public String getFormat() {
        return format;
    }

    /** Sets the semantic format hint. @param format the format @return this */
    public InputOptions setFormat(String format) {
        this.format = format;
        return this;
    }

    /**
     * Gets the default value pre-populated in the field. @return the default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default value pre-populated in the field. @param defaultValue the
     * default value @return this
     */
    public InputOptions setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
}
