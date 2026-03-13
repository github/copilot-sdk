/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;

namespace GitHub.Copilot.SDK;

internal static class TelemetryHelpers
{
    internal static (string? Traceparent, string? Tracestate) GetTraceContext()
    {
        return Activity.Current is { } activity
            ? (activity.Id, activity.TraceStateString)
            : (null, null);
    }

    /// <summary>
    /// Sets <see cref="Activity.Current"/> to reflect the trace context from the given
    /// W3C <paramref name="traceparent"/> / <paramref name="tracestate"/> headers.
    /// The runtime already owns the <c>execute_tool</c> span; this just ensures
    /// user code runs under the correct parent so any child activities are properly parented.
    /// Dispose the returned <see cref="Activity"/> to restore the previous <see cref="Activity.Current"/>.
    /// </summary>
    /// <remarks>
    /// Because this Activity is not created via an <see cref="ActivitySource"/>, it will not
    /// be sampled or exported by any standard OpenTelemetry exporter — it is invisible in
    /// trace backends.  It exists only to carry the remote parent context through
    /// <see cref="Activity.Current"/> so that child activities created by user tool
    /// handlers are parented to the CLI's span.
    /// </remarks>
    internal static Activity? RestoreTraceContext(string? traceparent, string? tracestate)
    {
        if (traceparent is not null &&
            ActivityContext.TryParse(traceparent, tracestate, out ActivityContext parent))
        {
            Activity activity = new("copilot.tool_handler");
            activity.SetParentId(parent.TraceId, parent.SpanId, parent.TraceFlags);
            if (tracestate is not null)
            {
                activity.TraceStateString = tracestate;
            }

            activity.Start();

            return activity;
        }

        return null;
    }
}
