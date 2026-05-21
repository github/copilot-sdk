/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Provides UI methods for eliciting information from the user during a session.
 * <p>
 * All methods on this interface throw {@link IllegalStateException} if the host
 * does not report elicitation support via
 * {@link com.github.copilot.sdk.CopilotSession#getCapabilities()}. Check
 * {@code session.getCapabilities().getUi() != null &&
 * Boolean.TRUE.equals(session.getCapabilities().getUi().getElicitation())}
 * before calling.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var caps = session.getCapabilities();
 * if (caps.getUi() != null && Boolean.TRUE.equals(caps.getUi().getElicitation())) {
 * 	boolean confirmed = session.getUi().confirm("Are you sure?").get();
 * }
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotSession#getUi()
 * @since 1.0.0
 */
public interface SessionUiApi {

    /**
     * Shows a generic elicitation dialog with a custom schema.
     *
     * @param params
     *            the elicitation parameters including message and schema
     * @return a future that resolves with the {@link ElicitationResult}
     * @throws IllegalStateException
     *             if the host does not support elicitation
     */
    CompletableFuture<ElicitationResult> elicitation(ElicitationParams params);

    /**
     * Shows a confirmation dialog and returns the user's boolean answer.
     * <p>
     * Returns {@code false} if the user declines or cancels.
     *
     * @param message
     *            the message to display
     * @return a future that resolves to {@code true} if the user confirmed
     * @throws IllegalStateException
     *             if the host does not support elicitation
     */
    CompletableFuture<Boolean> confirm(String message);

    /**
     * Shows a selection dialog with the given options.
     * <p>
     * Returns the selected value, or {@code null} if the user declines/cancels.
     *
     * @param message
     *            the message to display
     * @param options
     *            the options to present
     * @return a future that resolves to the selected string, or {@code null}
     * @throws IllegalStateException
     *             if the host does not support elicitation
     */
    CompletableFuture<String> select(String message, String[] options);

    /**
     * Shows a text input dialog.
     * <p>
     * Returns the entered text, or {@code null} if the user declines/cancels.
     *
     * @param message
     *            the message to display
     * @param options
     *            optional input field options, or {@code null}
     * @return a future that resolves to the entered string, or {@code null}
     * @throws IllegalStateException
     *             if the host does not support elicitation
     */
    CompletableFuture<String> input(String message, InputOptions options);
}
