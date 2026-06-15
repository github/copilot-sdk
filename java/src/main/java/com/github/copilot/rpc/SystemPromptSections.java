/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

/**
 * Deprecated: use {@link SystemMessageSections} instead.
 * <p>
 * This class is retained for backward compatibility. All constants are
 * inherited from {@link SystemMessageSections}.
 *
 * @deprecated Use {@link SystemMessageSections} — this class will be removed in
 *             a future major version.
 * @see SystemMessageSections
 * @since 1.2.0
 */
@Deprecated(since = "1.0.2", forRemoval = true)
public final class SystemPromptSections extends SystemMessageSections {

    private SystemPromptSections() {
    }
}
