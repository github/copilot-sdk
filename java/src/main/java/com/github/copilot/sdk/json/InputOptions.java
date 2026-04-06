/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Options for the {@link SessionUiApi#input(String, InputOptions)} convenience
 * method.
 *
 * @since 1.0.0
 */
public class InputOptions {

    private String title;
    private String description;
    private Integer minLength;
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

    /** Gets the minimum character length. @return the min length */
    public Integer getMinLength() {
        return minLength;
    }

    /**
     * Sets the minimum character length. @param minLength the min length @return
     * this
     */
    public InputOptions setMinLength(Integer minLength) {
        this.minLength = minLength;
        return this;
    }

    /** Gets the maximum character length. @return the max length */
    public Integer getMaxLength() {
        return maxLength;
    }

    /**
     * Sets the maximum character length. @param maxLength the max length @return
     * this
     */
    public InputOptions setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
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
