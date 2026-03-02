/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.SDK;

/// <summary>
/// String constants for OpenTelemetry Semantic Conventions for Generative AI systems.
/// </summary>
/// <remarks>
/// Based on the Semantic Conventions for Generative AI systems v1.40,
/// defined at <see href="https://opentelemetry.io/docs/specs/semconv/gen-ai/" />.
/// The specification is still experimental and subject to change.
/// </remarks>
internal static class OpenTelemetryConsts
{
    public const string DefaultSourceName = "github.copilot.sdk";
    public const string DefaultProviderName = "github";
    public const string CaptureMessageContentEnvVar = "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT";
    public const string SecondsUnit = "s";
    public const string TokensUnit = "{token}";

    public const string TokenTypeInput = "input";
    public const string TokenTypeOutput = "output";

    public static class Error
    {
        public const string Type = "error.type";
    }

    public static class Server
    {
        public const string Address = "server.address";
        public const string Port = "server.port";
    }

    public static class GenAI
    {
        public static class OperationNames
        {
            public const string Chat = "chat";
            public const string InvokeAgent = "invoke_agent";
            public const string ExecuteTool = "execute_tool";
        }

        public static class Operation
        {
            public const string Name = "gen_ai.operation.name";
        }

        public static class Provider
        {
            public const string Name = "gen_ai.provider.name";
        }

        public static class Agent
        {
            public const string Id = "gen_ai.agent.id";
            public const string Name = "gen_ai.agent.name";
            public const string Description = "gen_ai.agent.description";
        }

        public static class Conversation
        {
            public const string Id = "gen_ai.conversation.id";
        }

        public static class Request
        {
            public const string Model = "gen_ai.request.model";
        }

        public static class Response
        {
            public const string Id = "gen_ai.response.id";
            public const string Model = "gen_ai.response.model";
            public const string FinishReasons = "gen_ai.response.finish_reasons";
        }

        public static class Usage
        {
            public const string InputTokens = "gen_ai.usage.input_tokens";
            public const string OutputTokens = "gen_ai.usage.output_tokens";
            public const string CacheReadInputTokens = "gen_ai.usage.cache_read.input_tokens";
            public const string CacheCreationInputTokens = "gen_ai.usage.cache_creation.input_tokens";
        }

        public static class Token
        {
            public const string Type = "gen_ai.token.type";
        }

        public static class Input
        {
            public const string Messages = "gen_ai.input.messages";
        }

        public static class Output
        {
            public const string Messages = "gen_ai.output.messages";
        }

        public const string SystemInstructions = "gen_ai.system_instructions";

        public static class Tool
        {
            public const string Definitions = "gen_ai.tool.definitions";
            public const string CallId = "gen_ai.tool.call.id";
            public const string CallArguments = "gen_ai.tool.call.arguments";
            public const string CallResult = "gen_ai.tool.call.result";
            public const string Name = "gen_ai.tool.name";
            public const string Description = "gen_ai.tool.description";
            public const string Type = "gen_ai.tool.type";
        }

        public static class Client
        {
            public static class TokenUsage
            {
                public const string Name = "gen_ai.client.token.usage";
                public const string Description = "Number of input and output tokens used.";
                public static readonly int[] ExplicitBucketBoundaries =
                    [1, 4, 16, 64, 256, 1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216, 67108864];
            }

            public static class OperationDuration
            {
                public const string Name = "gen_ai.client.operation.duration";
                public const string Description = "GenAI operation duration.";
                public static readonly double[] ExplicitBucketBoundaries =
                    [0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64, 1.28, 2.56, 5.12, 10.24, 20.48, 40.96, 81.92];
            }

            public static class TimeToFirstChunk
            {
                public const string Name = "gen_ai.client.operation.time_to_first_chunk";
                public const string Description = "Time to receive the first chunk from a streaming response.";
                public static double[] ExplicitBucketBoundaries =>
                    OperationDuration.ExplicitBucketBoundaries;
            }

            public static class TimePerOutputChunk
            {
                public const string Name = "gen_ai.client.operation.time_per_output_chunk";
                public const string Description = "Time elapsed between streamed output chunks after the first chunk.";
                public static double[] ExplicitBucketBoundaries =>
                    OperationDuration.ExplicitBucketBoundaries;
            }
        }

        // Vendor-prefixed span event names for Copilot-specific lifecycle events.
        // These follow the {vendor}.{domain}.{event} convention.
        public static class CopilotEvent
        {
            public const string SessionTruncation = "github.copilot.session.truncation";
            public const string SessionCompactionStart = "github.copilot.session.compaction_start";
            public const string SessionCompactionComplete = "github.copilot.session.compaction_complete";
            public const string SkillInvoked = "github.copilot.skill.invoked";

            // Attribute keys for custom events (vendor-prefixed).
            public const string Message = "github.copilot.message";
            public const string TokenLimit = "github.copilot.token_limit";
            public const string PreTokens = "github.copilot.pre_tokens";
            public const string PostTokens = "github.copilot.post_tokens";
            public const string PreMessages = "github.copilot.pre_messages";
            public const string PostMessages = "github.copilot.post_messages";
            public const string TokensRemoved = "github.copilot.tokens_removed";
            public const string MessagesRemoved = "github.copilot.messages_removed";
            public const string PerformedBy = "github.copilot.performed_by";
            public const string Success = "github.copilot.success";
            public const string SkillName = "github.copilot.skill.name";
            public const string SkillPath = "github.copilot.skill.path";
            public const string SkillContent = "github.copilot.skill.content";
            public const string SkillPluginName = "github.copilot.skill.plugin_name";
            public const string SkillPluginVersion = "github.copilot.skill.plugin_version";
        }

        // Vendor-prefixed span attributes for Copilot-specific data on standardized spans.
        public static class Copilot
        {
            // High-value: on chat spans (from AssistantUsageData)
            public const string Cost = "github.copilot.cost";
            public const string ServerDuration = "github.copilot.server_duration";
            public const string Initiator = "github.copilot.initiator";
            public const string Aiu = "github.copilot.aiu";

            public const string TurnId = "github.copilot.turn_id";
            public const string InteractionId = "github.copilot.interaction_id";
        }
    }
}
