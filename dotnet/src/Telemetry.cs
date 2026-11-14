/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;

namespace GitHub.Copilot;

internal static class TelemetryHelpers
{
    private static readonly ActivitySource s_activitySource = new("github.copilot");

    internal static (string? Traceparent, string? Tracestate) GetTraceContext()
    {
        return Activity.Current is { } activity
            ? (activity.Id, activity.TraceStateString)
            : (null, null);
    }

    /// <summary>
    /// Starts a tool-handler activity under the remote W3C
    /// <paramref name="traceparent"/> / <paramref name="tracestate"/> context.
    /// Dispose the returned <see cref="Activity"/> to restore the previous <see cref="Activity.Current"/>.
    /// </summary>
    /// <remarks>
    /// Listeners for <c>github.copilot</c> can sample and export this activity so child
    /// spans have an observable parent. Without a listener, or when sampling returns
    /// <see cref="ActivitySamplingResult.None"/>, a propagation-only activity preserves
    /// the remote context without recording data.
    /// </remarks>
    internal static Activity? RestoreTraceContext(string? traceparent, string? tracestate)
    {
        if (traceparent is not null &&
            ActivityContext.TryParse(traceparent, tracestate, isRemote: true, out ActivityContext parent))
        {
            Activity? activity = s_activitySource.StartActivity("copilot.tool_handler", ActivityKind.Internal, parent);
            if (activity is not null)
            {
                return activity;
            }

            // ActivitySource returns null when no listener requests an activity.
            activity = new Activity("copilot.tool_handler") { IsAllDataRequested = false };
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
