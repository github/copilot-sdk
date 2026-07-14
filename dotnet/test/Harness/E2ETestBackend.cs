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
    private const string DefaultModel = "claude-sonnet-4.5";
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
            _ => throw new InvalidOperationException(
                $"Unsupported {EnvironmentVariable} value '{value}'. Expected capi, anthropic-messages, openai-responses, or openai-completions."),
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

        config.Model ??= DefaultModel;
        config.Provider = CreateProvider(backend, proxyUrl);
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

        config.Model ??= DefaultModel;
        config.Provider = CreateProvider(backend, proxyUrl);
    }

    private static ProviderConfig CreateProvider(E2ETestBackend backend, string proxyUrl)
        => new()
        {
            BaseUrl = proxyUrl,
            Type = backend == E2ETestBackend.AnthropicMessages ? "anthropic" : "openai",
            WireApi = backend switch
            {
                E2ETestBackend.OpenAIResponses => "responses",
                E2ETestBackend.OpenAICompletions => "completions",
                _ => null,
            },
            BearerToken = FakeCredential,
            ModelId = DefaultModel,
            WireModel = DefaultModel,
        };
}

internal static class E2ETestTraits
{
    internal const string Backend = "E2EBackend";
    internal const string CapiOnly = "CapiOnly";
    internal const string SelfConfiguredProvider = "SelfConfiguredProvider";
}
