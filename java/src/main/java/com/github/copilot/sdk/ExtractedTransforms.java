/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.github.copilot.sdk.json.SystemMessageConfig;

/**
 * Result of extracting transform callbacks from a {@link SystemMessageConfig}.
 * <p>
 * Holds a wire-safe copy of the system message config (with transform callbacks
 * replaced by {@code action="transform"}) alongside the extracted callbacks
 * that must be registered with the session.
 *
 * @param wireSystemMessage
 *            the system message config safe for JSON serialization; may be
 *            {@code null} when the input config was {@code null}
 * @param transformCallbacks
 *            transform callbacks keyed by section identifier; {@code null} when
 *            no transforms were present
 * @see SessionRequestBuilder#extractTransformCallbacks(SystemMessageConfig)
 */
record ExtractedTransforms(SystemMessageConfig wireSystemMessage,
        Map<String, Function<String, CompletableFuture<String>>> transformCallbacks) {
}
