/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Override operation for a single system prompt section in
 * {@link SystemMessageMode#CUSTOMIZE} mode.
 * <p>
 * Each {@code SectionOverride} describes how one named section of the default
 * system prompt should be modified. The section name keys come from
 * {@link SystemPromptSections}.
 *
 * <h2>Static override example</h2>
 *
 * <pre>{@code
 * var config = new SystemMessageConfig().setMode(SystemMessageMode.CUSTOMIZE).setSections(Map.of(
 * 		SystemPromptSections.TONE,
 * 		new SectionOverride().setAction(SectionOverrideAction.REPLACE).setContent("Be concise and formal."),
 * 		SystemPromptSections.CODE_CHANGE_RULES, new SectionOverride().setAction(SectionOverrideAction.REMOVE)));
 * }</pre>
 *
 * <h2>Transform callback example</h2>
 *
 * <pre>{@code
 * var config = new SystemMessageConfig().setMode(SystemMessageMode.CUSTOMIZE)
 * 		.setSections(Map.of(SystemPromptSections.IDENTITY, new SectionOverride().setTransform(
 * 				content -> CompletableFuture.completedFuture(content + "\nAlways end replies with DONE."))));
 * }</pre>
 *
 * @see SystemMessageConfig
 * @see SectionOverrideAction
 * @see SystemPromptSections
 * @since 1.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SectionOverride {

    @JsonProperty("action")
    private SectionOverrideAction action;

    @JsonProperty("content")
    private String content;

    /**
     * Transform callback invoked by the SDK when the CLI requests a
     * {@code systemMessage.transform} RPC call.
     * <p>
     * The function receives the current section content and returns the transformed
     * content wrapped in a {@link CompletableFuture}. When a transform is set, it
     * takes precedence over {@link #action}; the wire representation uses
     * {@link SectionOverrideAction#TRANSFORM} automatically.
     * <p>
     * This field is not serialized — it is handled entirely by the SDK.
     */
    @JsonIgnore
    private Function<String, CompletableFuture<String>> transform;

    /**
     * Gets the override action.
     *
     * @return the action, or {@code null} if a transform callback is set
     */
    public SectionOverrideAction getAction() {
        return action;
    }

    /**
     * Sets the override action.
     *
     * @param action
     *            the action to perform on this section
     * @return this override for method chaining
     */
    public SectionOverride setAction(SectionOverrideAction action) {
        this.action = action;
        return this;
    }

    /**
     * Gets the content for the override.
     *
     * @return the content, or {@code null}
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content for the override.
     * <p>
     * Used for {@link SectionOverrideAction#REPLACE},
     * {@link SectionOverrideAction#APPEND}, and
     * {@link SectionOverrideAction#PREPEND}. Ignored for
     * {@link SectionOverrideAction#REMOVE}.
     *
     * @param content
     *            the content string
     * @return this override for method chaining
     */
    public SectionOverride setContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * Gets the transform callback.
     *
     * @return the transform function, or {@code null} if not set
     */
    public Function<String, CompletableFuture<String>> getTransform() {
        return transform;
    }

    /**
     * Sets the transform callback for this section.
     * <p>
     * The function receives the current section content as a {@code String} and
     * returns the transformed content via a {@link CompletableFuture}. When set,
     * this takes precedence over {@link #action}.
     *
     * @param transform
     *            a function that transforms the section content
     * @return this override for method chaining
     */
    public SectionOverride setTransform(Function<String, CompletableFuture<String>> transform) {
        this.transform = transform;
        return this;
    }
}
