/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal helper for timing-based diagnostic logging.
 */
final class LoggingHelpers {

    private LoggingHelpers() {
        // Utility class
    }

    /**
     * Formats elapsed time as a human-readable duration string.
     *
     * @param startNanos
     *            the start time from {@link System#nanoTime()}
     * @return formatted duration (e.g. "PT0.123S")
     */
    static String formatElapsed(long startNanos) {
        long elapsedNanos = System.nanoTime() - startNanos;
        long millis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        return String.format("PT%d.%03dS", millis / 1000, millis % 1000);
    }

    /**
     * Logs a timing message at the given level if the logger accepts it.
     *
     * @param logger
     *            the logger to use
     * @param level
     *            the log level
     * @param message
     *            the message template
     * @param startNanos
     *            the start time from {@link System#nanoTime()}
     */
    static void logTiming(Logger logger, Level level, String message, long startNanos) {
        if (!logger.isLoggable(level)) {
            return;
        }
        logger.log(level, message.replace("{Elapsed}", formatElapsed(startNanos)));
    }

    /**
     * Logs a timing message at the given level with an exception.
     *
     * @param logger
     *            the logger to use
     * @param level
     *            the log level
     * @param exception
     *            the exception, may be {@code null}
     * @param message
     *            the message template
     * @param startNanos
     *            the start time from {@link System#nanoTime()}
     */
    static void logTiming(Logger logger, Level level, Throwable exception, String message, long startNanos) {
        if (!logger.isLoggable(level)) {
            return;
        }
        String formatted = message.replace("{Elapsed}", formatElapsed(startNanos));
        if (exception != null) {
            logger.log(level, formatted, exception);
        } else {
            logger.log(level, formatted);
        }
    }
}
