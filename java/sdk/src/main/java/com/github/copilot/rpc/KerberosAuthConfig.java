/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.github.copilot.generated.rpc.KerberosAuthAcquireRequest;
import com.github.copilot.generated.rpc.KerberosAuthMode;
import com.github.copilot.generated.rpc.KerberosAuthScope;

/**
 * Supplies Kerberos credentials from the SDK host for a requested service
 * principal.
 *
 * @param mode
 *            {@link KerberosAuthMode#FALLBACK} uses the callback only when
 *            native lookup fails; {@link KerberosAuthMode#ALWAYS} never
 *            attempts native lookup
 * @param scope
 *            the traffic the callback authenticates, or {@code null} for the
 *            runtime default, {@link KerberosAuthScope#CONNECTION}, which
 *            covers only requests made for sessions this client creates or
 *            resumes. {@link KerberosAuthScope#RUNTIME} also covers the
 *            runtime's shared background traffic; only one connection at a time
 *            can hold it, so startup fails if another client holds it. The
 *            registration ends when the connection closes.
 * @param callback
 *            returns a future with a fresh, single-use base64 SPNEGO token
 *            without the {@code Negotiate } prefix, or {@code null} when
 *            unavailable. The request's SPN uses the platform-independent
 *            {@code service/host[@REALM]} form; validate it before issuing
 *            credentials. The callback runs on the client executor; if the
 *            runtime cancels the request or the connection closes, a queued
 *            invocation is skipped and a pending returned future is cancelled.
 * @see CopilotClientOptions#setKerberosAuth(KerberosAuthConfig)
 * @since 1.0.0
 */
public record KerberosAuthConfig(KerberosAuthMode mode, KerberosAuthScope scope,
        Function<KerberosAuthAcquireRequest, CompletableFuture<String>> callback) {

    /**
     * Creates a Kerberos authentication configuration.
     *
     * @param mode
     *            the credential lookup mode (must not be {@code null})
     * @param scope
     *            the traffic the callback authenticates, or {@code null} for the
     *            runtime default ({@link KerberosAuthScope#CONNECTION})
     * @param callback
     *            the token callback (must not be {@code null})
     */
    public KerberosAuthConfig {
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(callback, "callback must not be null");
    }

    /**
     * Creates a Kerberos authentication configuration with the runtime's default
     * scope ({@link KerberosAuthScope#CONNECTION}).
     *
     * @param mode
     *            the credential lookup mode (must not be {@code null})
     * @param callback
     *            the token callback (must not be {@code null})
     */
    public KerberosAuthConfig(KerberosAuthMode mode,
            Function<KerberosAuthAcquireRequest, CompletableFuture<String>> callback) {
        this(mode, null, callback);
    }
}
