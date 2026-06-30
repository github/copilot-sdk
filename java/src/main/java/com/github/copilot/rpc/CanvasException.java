/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.CopilotExperimental;

/**
 * Structured exception returned from {@link CanvasHandler} callbacks.
 * <p>
 * Throw (or complete a returned future exceptionally with) a
 * {@code CanvasException} to surface a machine-readable error code to the
 * runtime. Any other exception is wrapped in a generic
 * {@code canvas_handler_error} envelope.
 * <p>
 * <strong>Experimental.</strong> Canvas configuration is part of an
 * experimental wire-protocol surface and may change or be removed in future SDK
 * or CLI releases.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public class CanvasException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    /**
     * Creates a new canvas exception.
     *
     * @param code
     *            the machine-readable error code
     * @param message
     *            the human-readable message
     */
    public CanvasException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Gets the machine-readable error code.
     *
     * @return the error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Creates the default exception returned when a declared action has no handler.
     *
     * @return a {@code CanvasException} with code {@code canvas_action_no_handler}
     */
    public static CanvasException noHandler() {
        return new CanvasException("canvas_action_no_handler", "No handler implemented for this canvas action");
    }
}
