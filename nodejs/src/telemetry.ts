/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Trace-context helpers.
 *
 * The SDK does not depend on any OpenTelemetry packages.  Instead, users
 * provide an {@link TraceContextProvider} callback via client options.
 *
 * @module telemetry
 */

import type { TraceContext, TraceContextProvider } from "./types.js";

/**
 * Calls the user-provided {@link TraceContextProvider} to obtain the current
 * W3C Trace Context.  Returns `{}` when no provider is configured.
 */
export async function getTraceContext(provider?: TraceContextProvider): Promise<TraceContext> {
    if (!provider) return {};
    try {
        return (await provider()) ?? {};
    } catch {
        return {};
    }
}
