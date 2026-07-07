/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using Xunit.Sdk;

namespace GitHub.Copilot.Test.Harness;

// Because many of the tests mutate global environment variables, we have to snapshot the original
// state and restore it after each test. Otherwise tests influence each other depending on run order.
// This is especially important for the in-process transport because the runtime is inside the test
// host process and will be reading/writing its environment variables directly.
internal static class InProcessEnvIsolation
{
    // Unset because CI sets them but the replay snapshots expect Bearer/OAuth.
    private static readonly string[] SuppressEnvVars = ["COPILOT_HMAC_KEY", "CAPI_HMAC_KEY"];

    // Captured at load, before any fixture/test mutates env.
    private static readonly Dictionary<string, string?> s_ambient = CaptureEnvironment();

    // Runs at assembly load so the ambient env is snapshotted before the shared
    // fixture mirrors per-test env onto the process. Justifies suppressing CA2255.
#pragma warning disable CA2255 // ModuleInitializer discouraged in libraries; intentional in this test harness.
    [ModuleInitializer]
    internal static void CaptureAtLoad() => _ = s_ambient;
#pragma warning restore CA2255

    [DllImport("libc", EntryPoint = "setenv", CharSet = CharSet.Ansi,
        BestFitMapping = false, ThrowOnUnmappableChar = true)]
    private static extern int NativeSetEnv(string name, string value, int overwrite);

    [DllImport("libc", EntryPoint = "unsetenv", CharSet = CharSet.Ansi,
        BestFitMapping = false, ThrowOnUnmappableChar = true)]
    private static extern int NativeUnsetEnv(string name);

    // Sets/unsets on the managed cache and, on Unix, the libc block so native
    // readers in the loaded cdylib observe it.
    public static void Apply(string name, string? value)
    {
        Environment.SetEnvironmentVariable(name, value);
        if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
            _ = value is null ? NativeUnsetEnv(name) : NativeSetEnv(name, value, 1);
        }
    }

    public static void NeutralizeAmbientCredentials()
    {
        foreach (var name in SuppressEnvVars)
        {
            Apply(name, null);
        }
    }

    public static void RestoreAmbient()
    {
        foreach (DictionaryEntry entry in Environment.GetEnvironmentVariables())
        {
            var name = (string)entry.Key;
            if (!s_ambient.ContainsKey(name))
            {
                Apply(name, null);
            }
        }

        foreach (var (name, value) in s_ambient)
        {
            if (!string.Equals(Environment.GetEnvironmentVariable(name), value, StringComparison.Ordinal))
            {
                Apply(name, value);
            }
        }
    }

    private static Dictionary<string, string?> CaptureEnvironment() =>
        Environment.GetEnvironmentVariables()
            .Cast<DictionaryEntry>()
            .ToDictionary(e => (string)e.Key, e => e.Value?.ToString(), StringComparer.Ordinal);
}

[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = false)]
public sealed class InProcessEnvIsolationAttribute : BeforeAfterTestAttribute
{
    public override void Before(MethodInfo methodUnderTest) =>
        InProcessEnvIsolation.NeutralizeAmbientCredentials();

    public override void After(MethodInfo methodUnderTest) =>
        InProcessEnvIsolation.RestoreAmbient();
}
