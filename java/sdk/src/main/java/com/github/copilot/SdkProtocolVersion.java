/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Maintained by hand: no generator writes this file. Keep LATEST in sync with
// sdk-protocol-version.json — the Codegen Check workflow enforces the match.

package com.github.copilot;

/**
 * Provides the SDK protocol version. This must match the version expected by
 * the copilot-agent-runtime server.
 *
 * @since 1.0.0
 */
public enum SdkProtocolVersion {

    LATEST(3);

    private int versionNumber;

    private SdkProtocolVersion(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public int getVersionNumber() {
        return this.versionNumber;
    }

    /**
     * Gets the SDK protocol version.
     *
     * @return the protocol version
     */
    public static int get() {
        return LATEST.getVersionNumber();
    }
}
