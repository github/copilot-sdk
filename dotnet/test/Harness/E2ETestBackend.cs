/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.Test.Harness;

internal enum E2ETestBackend
{
    Capi,
    AnthropicMessages,
    OpenAIResponses,
    OpenAICompletions,
}

internal static class E2ETestBackendConfiguration
{
    internal const string EnvironmentVariable = "COPILOT_SDK_E2E_BACKEND";
    private const string AnthropicDefaultModel = "claude-sonnet-4.5";
    private const string OpenAIDefaultModel = "gpt-4.1";
    private const string FakeCredential = "fake-byok-credential-for-e2e-tests";

    internal static E2ETestBackend Current
        => Parse(Environment.GetEnvironmentVariable(EnvironmentVariable));

    internal static E2ETestBackend Parse(string? value)
        => value?.Trim().ToLowerInvariant() switch
        {
            null or "" or "capi" => E2ETestBackend.Capi,
            "anthropic-messages" => E2ETestBackend.AnthropicMessages,
            "openai-responses" => E2ETestBackend.OpenAIResponses,
            "openai-completions" => E2ETestBackend.OpenAICompletions,
            _ => throw new ArgumentOutOfRangeException(
                nameof(value),
                value,
                $"Unsupported {EnvironmentVariable} value. Expected capi, anthropic-messages, openai-responses, or openai-completions."),
        };

    internal static string ToWireName(this E2ETestBackend backend)
        => backend switch
        {
            E2ETestBackend.Capi => "capi",
            E2ETestBackend.AnthropicMessages => "anthropic-messages",
            E2ETestBackend.OpenAIResponses => "openai-responses",
            E2ETestBackend.OpenAICompletions => "openai-completions",
            _ => throw new ArgumentOutOfRangeException(nameof(backend), backend, null),
        };

    internal static void ApplyProvider(
        this E2ETestBackend backend,
        SessionConfig config,
        string proxyUrl)
    {
        if (backend == E2ETestBackend.Capi
            || config.Provider is not null
            || config.Providers is not null)
        {
            return;
        }

        var model = config.Model ??= backend.GetDefaultModel();
        config.Provider = CreateProvider(backend, proxyUrl, model);
    }

    internal static void ApplyProvider(
        this E2ETestBackend backend,
        ResumeSessionConfig config,
        string proxyUrl)
    {
        if (backend == E2ETestBackend.Capi || config.Provider is not null)
        {
            return;
        }

        var model = config.Model ??= backend.GetDefaultModel();
        config.Provider = CreateProvider(backend, proxyUrl, model);
    }

    private static string GetDefaultModel(this E2ETestBackend backend)
        => backend switch
        {
            E2ETestBackend.AnthropicMessages => AnthropicDefaultModel,
            E2ETestBackend.OpenAIResponses or E2ETestBackend.OpenAICompletions => OpenAIDefaultModel,
            _ => throw new ArgumentOutOfRangeException(nameof(backend), backend, null),
        };

    private static ProviderConfig CreateProvider(
        E2ETestBackend backend,
        string proxyUrl,
        string model)
        => new()
        {
            BaseUrl = proxyUrl,
            Type = backend switch
            {
                E2ETestBackend.AnthropicMessages => "anthropic",
                E2ETestBackend.OpenAIResponses or E2ETestBackend.OpenAICompletions => "openai",
                _ => throw new ArgumentOutOfRangeException(nameof(backend), backend, null),
            },
            WireApi = backend switch
            {
                E2ETestBackend.AnthropicMessages => null,
                E2ETestBackend.OpenAIResponses => "responses",
                E2ETestBackend.OpenAICompletions => "completions",
                _ => throw new ArgumentOutOfRangeException(nameof(backend), backend, null),
            },
            BearerToken = FakeCredential,
            ModelId = model,
            WireModel = model,
        };
}

internal static class E2ETestTraits
{
    // Trait key used by workflow filters to classify backend compatibility.
    internal const string Backend = "E2EBackend";

    // Requires the default CAPI backend and is excluded from BYOK legs.
    internal const string CapiOnly = "CapiOnly";

    // Owns its backend setup and must not inherit the backend selected by the test matrix.
    internal const string SelfConfiguredBackend = "SelfConfiguredBackend";
}
