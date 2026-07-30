/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

/**
 * Thrown when the {@code runtime.node} native binary cannot be resolved,
 * extracted, or cached by {@link NativeRuntimeLoader}.
 */
public final class NativeRuntimeLoaderException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message
     *            the detail message
     */
    public NativeRuntimeLoaderException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given detail message and cause.
     *
     * @param message
     *            the detail message
     * @param cause
     *            the cause
     */
    public NativeRuntimeLoaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
