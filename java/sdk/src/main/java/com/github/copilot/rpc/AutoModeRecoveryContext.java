/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Context for an offer to recover the active request using Auto, without
 * resending or rewinding it.
 *
 * @param requestId
 *            the one-use offer identifier
 * @param originalModel
 *            the original explicit model selection
 * @param allowAlways
 *            whether this offer permits remembering consent; currently true
 *            only for per-model rate limits
 * @param interactionId
 *            the retained interaction identifier, or {@code null}
 * @param originatingMessageId
 *            the original user-message identifier, or {@code null}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutoModeRecoveryContext(String requestId, String originalModel, boolean allowAlways, String interactionId,
        String originatingMessageId) {
}
