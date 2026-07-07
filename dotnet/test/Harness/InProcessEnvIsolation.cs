/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Reflection;
using System.Runtime.InteropServices;
using Xunit.Sdk;

namespace GitHub.Copilot.Test.Harness;

// =============================================================================
// TEMPORARY in-process env-var isolation. DELETE THIS ENTIRE FILE (and its two
// references in E2ETestContext plus the [assembly: InProcessEnvIsolation] in
// AssemblyInfo.cs) once the runtime stops reading the ambient process
// environment host-side.
//
// Why this exists
// ---------------
// Over the in-process FFI transport several runtime code paths run host-side in
// THIS process (the loaded cdylib) and read the ambient process environment
// rather than the environment passed to copilot_runtime_host_start — e.g.
// native fetch_copilot_user reads COPILOT_DEBUG_GITHUB_API_URL via
// std::env::var, the gh-CLI fallback spawns `gh auth token` (inheriting this
// process's GH_TOKEN / GITHUB_TOKEN / GH_CONFIG_DIR), auth-method selection
// reads the HMAC keys, and session state/config reads COPILOT_HOME / XDG_*.
// So the per-test environment we hand to CopilotClient is invisible to them.
//
// Two problems follow, both handled here:
//  1. CreateClient-routed tests must mirror their per-test environment onto this
//     process so host-side reads observe the replay proxy, isolated home, and
//     cleared credentials. See Mirror/Restore below.
//  2. Tests that construct CopilotClient directly (e.g. ClientE2ETests) never go
//     through CreateClient, so nothing clears the ambient CI HMAC credential;
//     in the in-process job they would authenticate for real and hit the live
//     api.githubcopilot.com. The assembly-level BeforeAfterTest attribute below
//     neutralizes those ambient credentials around EVERY test, independent of
//     how the client is constructed.
//
// Everything here is gated to the in-process job (COPILOT_SDK_DEFAULT_CONNECTION
// == "inprocess") and is a no-op otherwise.
// =============================================================================

/// <summary>
/// Owns all process-wide environment mutation used to make the in-process FFI
/// transport hermetic in tests. Consolidated in one file so it can be deleted
/// wholesale once the runtime no longer reads the ambient process environment.
/// </summary>
internal static class InProcessEnvIsolation
{
    // Ambient credentials that would otherwise let a directly-constructed client
    // authenticate for real (and reach the live API) in the in-process job. CI
    // sets COPILOT_HMAC_KEY at the job level; the replay snapshots are captured
    // against Bearer/OAuth requests, so real HMAC auth must be disabled. An empty
    // value disables the method (the runtime filters out empty HMAC keys).
    private static readonly string[] LeakyCredentialVars = ["COPILOT_HMAC_KEY", "CAPI_HMAC_KEY"];

    private static readonly object s_lock = new();

    // Process-wide, permanent record of the PRISTINE (pre-any-mirror) value of
    // every variable we have ever overwritten. A null entry means the variable
    // was originally unset. Captured once per name and never overwritten, so it
    // is immune to a cascade in which a skipped restore would otherwise let a
    // later test back up an already-polluted value as its baseline. Static
    // because the shared process env is itself process-global and E2E tests run
    // serially (DisableTestParallelization).
    private static readonly Dictionary<string, string?> s_pristineByName = new();

    [DllImport("libc", EntryPoint = "setenv", CharSet = CharSet.Ansi,
        BestFitMapping = false, ThrowOnUnmappableChar = true)]
    private static extern int NativeSetEnv(string name, string value, int overwrite);

    [DllImport("libc", EntryPoint = "unsetenv", CharSet = CharSet.Ansi,
        BestFitMapping = false, ThrowOnUnmappableChar = true)]
    private static extern int NativeUnsetEnv(string name);

    /// <summary>
    /// Whether the in-process FFI transport is the default for this run, honoring
    /// COPILOT_SDK_DEFAULT_CONNECTION from the supplied per-test environment (if
    /// any) else the process environment. Mirrors CopilotClient's own resolution.
    /// </summary>
    public static bool IsActive(IReadOnlyDictionary<string, string>? environment = null)
    {
        var value = environment is not null
            && environment.TryGetValue("COPILOT_SDK_DEFAULT_CONNECTION", out var fromOptions)
                ? fromOptions
                : Environment.GetEnvironmentVariable("COPILOT_SDK_DEFAULT_CONNECTION");
        return string.Equals(value, "inprocess", StringComparison.OrdinalIgnoreCase);
    }

    /// <summary>
    /// Mirrors a variable onto the shared process environment for the in-process
    /// host-side runtime to observe, recording the pristine value once so
    /// <see cref="Restore"/> can always revert to the true original.
    /// </summary>
    public static void Mirror(string name, string value)
    {
        lock (s_lock)
        {
            if (!s_pristineByName.ContainsKey(name))
            {
                s_pristineByName[name] = Environment.GetEnvironmentVariable(name);
            }
        }

        SetProcessEnvironmentVariable(name, value);
    }

    /// <summary>
    /// Reverts every variable ever touched by <see cref="Mirror"/> back to its
    /// permanently-recorded pristine value (or unsets it). Idempotent and
    /// cascade-proof: because pristine values are never overwritten, calling this
    /// always restores the true ambient environment even if a previous restore
    /// was skipped.
    /// </summary>
    public static void Restore()
    {
        KeyValuePair<string, string?>[] pristine;
        lock (s_lock)
        {
            if (s_pristineByName.Count == 0)
            {
                return;
            }

            pristine = [.. s_pristineByName];
        }

        foreach (var (name, value) in pristine)
        {
            RestoreProcessEnvironmentVariable(name, value);
        }
    }

    /// <summary>
    /// Neutralizes ambient credentials that would otherwise let a directly
    /// constructed client authenticate for real in the in-process job. Recorded
    /// via <see cref="Mirror"/> so <see cref="Restore"/> reverts them.
    /// </summary>
    public static void NeutralizeAmbientCredentials()
    {
        foreach (var name in LeakyCredentialVars)
        {
            Mirror(name, "");
        }
    }

    // Sets an environment variable on both the managed cache and (on Unix) the
    // libc environment block, so native getenv/std::env::var readers in the
    // loaded cdylib observe it. On Windows the managed setter already reaches
    // native GetEnvironmentVariableW, so setenv is not needed.
    private static void SetProcessEnvironmentVariable(string name, string value)
    {
        Environment.SetEnvironmentVariable(name, value);
        if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
            _ = NativeSetEnv(name, value, 1);
        }
    }

    // Restores (or unsets) an environment variable on both the managed cache and
    // (on Unix) the libc environment block.
    private static void RestoreProcessEnvironmentVariable(string name, string? value)
    {
        Environment.SetEnvironmentVariable(name, value);
        if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
            _ = value is null ? NativeUnsetEnv(name) : NativeSetEnv(name, value, 1);
        }
    }
}

/// <summary>
/// Assembly-level xUnit hook that isolates the ambient process environment around
/// every test in the in-process job, independent of how the CopilotClient is
/// constructed. No-op outside the in-process job. TEMPORARY — see
/// <see cref="InProcessEnvIsolation"/>.
/// </summary>
[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = false)]
public sealed class InProcessEnvIsolationAttribute : BeforeAfterTestAttribute
{
    public override void Before(MethodInfo methodUnderTest)
    {
        if (InProcessEnvIsolation.IsActive())
        {
            InProcessEnvIsolation.NeutralizeAmbientCredentials();
        }
    }

    public override void After(MethodInfo methodUnderTest)
    {
        if (InProcessEnvIsolation.IsActive())
        {
            InProcessEnvIsolation.Restore();
        }
    }
}
