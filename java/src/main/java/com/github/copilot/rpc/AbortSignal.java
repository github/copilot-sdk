/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A signal that indicates whether a tool invocation has been aborted.
 * <p>
 * An {@code AbortSignal} is passed to tool handlers via
 * {@link ToolInvocation#getAbortSignal()} and is triggered when
 * {@link com.github.copilot.CopilotSession#abort()} is called while the tool is
 * executing. Tool handlers can use this to implement cooperative cancellation,
 * allowing them to stop long-running work gracefully when the session is
 * aborted.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * ToolHandler handler = invocation -> {
 * 	AbortSignal signal = invocation.getAbortSignal();
 * 	return CompletableFuture.supplyAsync(() -> {
 * 		while (!signal.isAborted()) {
 * 			// do incremental work here
 * 		}
 * 		throw new CancellationException("Tool aborted");
 * 	});
 * };
 * }</pre>
 *
 * <h2>Callback Registration</h2>
 *
 * <pre>{@code
 * ToolHandler handler = invocation -> {
 * 	AbortSignal signal = invocation.getAbortSignal();
 * 	signal.onAborted(() -> System.out.println("Aborting tool!"));
 * 	// ... perform work ...
 * 	return CompletableFuture.completedFuture("done");
 * };
 * }</pre>
 *
 * @see ToolInvocation#getAbortSignal()
 * @see com.github.copilot.CopilotSession#abort()
 * @since 1.6.0
 */
public final class AbortSignal {

    private final AtomicBoolean aborted = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    /**
     * Returns whether this signal has been aborted.
     *
     * @return {@code true} if {@link com.github.copilot.CopilotSession#abort()} was
     *         called while this tool invocation was in progress; {@code false}
     *         otherwise
     */
    public boolean isAborted() {
        return aborted.get();
    }

    /**
     * Registers a callback to be invoked when this signal is aborted.
     * <p>
     * If the signal is already aborted at the time of registration, the callback is
     * invoked immediately on the calling thread.
     * <p>
     * The callback is guaranteed to be invoked at most once, regardless of
     * concurrent calls to {@link #abort()} and {@code onAborted}. Any
     * {@link Throwable} thrown by the callback is silently ignored.
     *
     * @param listener
     *            the callback to invoke on abort
     * @throws NullPointerException
     *             if listener is null
     */
    public void onAborted(Runnable listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        // Wrap in an AtomicBoolean-guarded runnable so the callback fires at most once
        // even if abort() races with this method between listeners.add() and the
        // aborted.get() check below.
        AtomicBoolean fired = new AtomicBoolean(false);
        Runnable once = () -> {
            if (fired.compareAndSet(false, true)) {
                try {
                    listener.run();
                } catch (Throwable ignored) {
                    // Throwables from listeners are silently ignored
                }
            }
        };
        listeners.add(once);
        if (aborted.get()) {
            once.run();
        }
    }

    /**
     * Triggers this abort signal, notifying all registered listeners.
     * <p>
     * <strong>Note:</strong> This method is intended for internal SDK use only. It
     * is called by the SDK when {@link com.github.copilot.CopilotSession#abort()}
     * is invoked while this tool invocation is in progress.
     * <p>
     * Calling this method more than once has no effect — the signal fires exactly
     * once. Any {@link Throwable} thrown by a listener is silently ignored.
     */
    public void abort() {
        if (aborted.compareAndSet(false, true)) {
            for (Runnable listener : listeners) {
                try {
                    listener.run();
                } catch (Throwable ignored) {
                    // Throwables from listeners are silently ignored
                }
            }
        }
    }
}
