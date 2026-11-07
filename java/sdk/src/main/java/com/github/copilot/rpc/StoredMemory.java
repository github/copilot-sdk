/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.CopilotExperimental;

/**
 * A memory the agent asked to store, after validation, secret filtering and
 * approval.
 *
 * @param sessionId
 *            ID of the session that stored the memory
 * @param subject
 *            short topic the memory is about
 * @param fact
 *            the fact to remember
 * @param citations
 *            sources supporting the fact, such as file paths with line numbers
 * @param reason
 *            why the agent considers the fact worth remembering
 * @apiNote This API is experimental and may change in a future version.
 * @since 1.6.0
 */
@CopilotExperimental
public record StoredMemory(String sessionId, String subject, String fact, String citations, String reason) {
}
