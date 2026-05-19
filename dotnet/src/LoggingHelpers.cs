/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.Logging;
using System.Diagnostics;

namespace GitHub.Copilot.SDK;

internal static class LoggingHelpers
{
    internal static void LogTiming(
        ILogger logger,
        LogLevel level,
        Exception? exception,
        string message,
        long startTimestamp)
    {
        if (!logger.IsEnabled(level))
        {
            return;
        }

        LogTimingCore(logger, level, exception, message, Stopwatch.GetElapsedTime(startTimestamp));
    }

    internal static void LogTiming<T1>(
        ILogger logger,
        LogLevel level,
        Exception? exception,
        string message,
        long startTimestamp,
        T1 arg1)
    {
        if (!logger.IsEnabled(level))
        {
            return;
        }

        LogTimingCore(logger, level, exception, message, Stopwatch.GetElapsedTime(startTimestamp), arg1);
    }

    internal static void LogTiming<T1, T2>(
        ILogger logger,
        LogLevel level,
        Exception? exception,
        string message,
        long startTimestamp,
        T1 arg1,
        T2 arg2)
    {
        if (!logger.IsEnabled(level))
        {
            return;
        }

        LogTimingCore(logger, level, exception, message, Stopwatch.GetElapsedTime(startTimestamp), arg1, arg2);
    }

    internal static void LogTiming<T1, T2, T3>(
        ILogger logger,
        LogLevel level,
        Exception? exception,
        string message,
        long startTimestamp,
        T1 arg1,
        T2 arg2,
        T3 arg3)
    {
        if (!logger.IsEnabled(level))
        {
            return;
        }

        LogTimingCore(logger, level, exception, message, Stopwatch.GetElapsedTime(startTimestamp), arg1, arg2, arg3);
    }

    internal static void LogTiming<T1, T2, T3, T4>(
        ILogger logger,
        LogLevel level,
        Exception? exception,
        string message,
        long startTimestamp,
        T1 arg1,
        T2 arg2,
        T3 arg3,
        T4 arg4)
    {
        if (!logger.IsEnabled(level))
        {
            return;
        }

        LogTimingCore(logger, level, exception, message, Stopwatch.GetElapsedTime(startTimestamp), arg1, arg2, arg3, arg4);
    }

    private static void LogTimingCore(
        ILogger logger,
        LogLevel level,
        Exception? exception,
        string message,
        params object?[] args)
    {
#pragma warning disable CA2254 // Timing call sites pass static templates through this helper.
        logger.Log(level, exception, message, args);
#pragma warning restore CA2254
    }
}
