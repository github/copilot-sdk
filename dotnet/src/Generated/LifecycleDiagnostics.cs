/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: lifecycle-diagnostics.schema.json

using System.Diagnostics.CodeAnalysis;
using System.Diagnostics;
using System.ComponentModel;
using System.ComponentModel.DataAnnotations;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot;

/// <summary>Base class for experimental runtime lifecycle diagnostic events.</summary>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
[JsonPolymorphic(TypeDiscriminatorPropertyName = "type")]
[JsonDerivedType(typeof(RuntimeLifecycleSystemStateDiagnosticEvent), "runtime.lifecycle.system_state")]
[JsonDerivedType(typeof(RuntimeLifecycleProgressStateDiagnosticEvent), "runtime.lifecycle.progress_state")]
internal abstract partial class LifecycleDiagnosticEvent
{
    /// <summary>The event type discriminator.</summary>
    [JsonIgnore]
    public abstract string Type { get; }

    /// <summary>When the runtime projected this diagnostic event.</summary>
    [JsonPropertyName("timestamp")]
    public required DateTimeOffset Timestamp { get; set; }
}

/// <summary>Experimental runtime lifecycle diagnostic event.</summary>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleSystemStateDiagnosticEvent : LifecycleDiagnosticEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "runtime.lifecycle.system_state";

    /// <summary>The runtime.lifecycle.system_state payload.</summary>
    [JsonPropertyName("data")]
    public required RuntimeLifecycleSystemStateData Data { get; set; }
}

/// <summary>Experimental runtime lifecycle diagnostic event.</summary>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleProgressStateDiagnosticEvent : LifecycleDiagnosticEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "runtime.lifecycle.progress_state";

    /// <summary>The runtime.lifecycle.progress_state payload.</summary>
    [JsonPropertyName("data")]
    public required RuntimeLifecycleProgressStateData Data { get; set; }
}

/// <summary>Event payload for <see cref="RuntimeLifecycleSystemStateData"/>.</summary>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleSystemStateData
{
    /// <summary>Complete active operation set after applying the change.</summary>
    [JsonPropertyName("activeOperations")]
    public required RuntimeLifecycleOperation[] ActiveOperations { get; set; }

    /// <summary>Operation transition that produced this system revision.</summary>
    [JsonPropertyName("changedOperation")]
    public required RuntimeLifecycleOperation ChangedOperation { get; set; }

    /// <summary>Authoritative condition retained after this change. Initial shared values are `current`, `disabled`, `degraded`, and `quiescent`; integrations can add bounded values as the contract matures.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("condition")]
    public string? Condition { get; set; }

    /// <summary>Opaque identity for one session registry generation.</summary>
    [JsonPropertyName("incarnationId")]
    public required string IncarnationId { get; set; }

    /// <summary>Session whose lifecycle owns this system.</summary>
    [JsonPropertyName("sessionId")]
    public required string SessionId { get; set; }

    /// <summary>Stable subsystem identifier.</summary>
    [JsonPropertyName("system")]
    public required string System { get; set; }

    /// <summary>Orders every observed change to this system in the incarnation.</summary>
    [JsonPropertyName("systemRevision")]
    public required long SystemRevision { get; set; }
}

/// <summary>Event payload for <see cref="RuntimeLifecycleProgressStateData"/>.</summary>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleProgressStateData
{
    /// <summary>Complete dependency blocker set after this progress change.</summary>
    [JsonPropertyName("blockers")]
    public required RuntimeLifecycleBlocker[] Blockers { get; set; }

    /// <summary>How the progress record ended. Absent while it remains active.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("disposition")]
    public RuntimeLifecycleDisposition? Disposition { get; set; }

    /// <summary>Opaque identity for one session registry generation.</summary>
    [JsonPropertyName("incarnationId")]
    public required string IncarnationId { get; set; }

    /// <summary>Progress-owner-defined kind.</summary>
    [JsonPropertyName("kind")]
    public required string Kind { get; set; }

    /// <summary>Immutable causal origin, when another lifecycle progress record caused this one.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("origin")]
    public RuntimeLifecycleProgressOrigin? Origin { get; set; }

    /// <summary>Progress-owner-defined phase.</summary>
    [JsonPropertyName("phase")]
    public required string Phase { get; set; }

    /// <summary>Opaque progress identifier, unique within the incarnation and kind.</summary>
    [JsonPropertyName("progressId")]
    public required string ProgressId { get; set; }

    /// <summary>Orders changes to this progress record.</summary>
    [JsonPropertyName("revision")]
    public required long Revision { get; set; }

    /// <summary>Session whose lifecycle owns this progress.</summary>
    [JsonPropertyName("sessionId")]
    public required string SessionId { get; set; }

    /// <summary>Timestamps recorded by the lifecycle service for this progress revision.</summary>
    [JsonPropertyName("timing")]
    public required RuntimeLifecycleProgressTiming Timing { get; set; }
}

/// <summary>Fixed timestamps for one observed operation revision.</summary>
/// <remarks>Nested data type for <c>RuntimeLifecycleOperationTiming</c>.</remarks>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleOperationTiming
{
    /// <summary>ISO 8601 timestamp when the current phase began.</summary>
    [JsonPropertyName("phaseStartedAt")]
    public required DateTimeOffset PhaseStartedAt { get; set; }

    /// <summary>ISO 8601 timestamp when the operation began.</summary>
    [JsonPropertyName("startedAt")]
    public required DateTimeOffset StartedAt { get; set; }

    /// <summary>ISO 8601 timestamp when this operation revision was recorded.</summary>
    [JsonPropertyName("transitionedAt")]
    public required DateTimeOffset TransitionedAt { get; set; }
}

/// <summary>Observed state of one readiness or convergence operation.</summary>
/// <remarks>Nested data type for <c>RuntimeLifecycleOperation</c>.</remarks>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleOperation
{
    /// <summary>How the operation ended. Absent while the operation remains active.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("disposition")]
    public RuntimeLifecycleDisposition? Disposition { get; set; }

    /// <summary>Opaque operation identifier, unique within the lifecycle incarnation and system.</summary>
    [JsonPropertyName("operationId")]
    public required string OperationId { get; set; }

    /// <summary>Orders transitions of this operation.</summary>
    [JsonPropertyName("operationRevision")]
    public required long OperationRevision { get; set; }

    /// <summary>System-owned lifecycle stage.</summary>
    [JsonPropertyName("phase")]
    public required string Phase { get; set; }

    /// <summary>System-owned production boundary within the phase.</summary>
    [JsonPropertyName("step")]
    public required string Step { get; set; }

    /// <summary>Timestamps recorded by the lifecycle service for this operation revision.</summary>
    [JsonPropertyName("timing")]
    public required RuntimeLifecycleOperationTiming Timing { get; set; }

    /// <summary>System-owned reason the operation started.</summary>
    [JsonPropertyName("trigger")]
    public required string Trigger { get; set; }
}

/// <summary>Timing of one real dependency wait.</summary>
/// <remarks>Nested data type for <c>RuntimeLifecycleBlockerTiming</c>.</remarks>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleBlockerTiming
{
    /// <summary>ISO 8601 timestamp when this dependency began blocking progress.</summary>
    [JsonPropertyName("blockingSince")]
    public required DateTimeOffset BlockingSince { get; set; }
}

/// <summary>Exact lifecycle operation awaited by a real dependency wait.</summary>
/// <remarks>Nested data type for <c>RuntimeLifecycleBlocker</c>.</remarks>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleBlocker
{
    /// <summary>Current phase of the awaited operation.</summary>
    [JsonPropertyName("currentPhase")]
    public required string CurrentPhase { get; set; }

    /// <summary>Current production boundary of the awaited operation.</summary>
    [JsonPropertyName("currentStep")]
    public required string CurrentStep { get; set; }

    /// <summary>How the awaited operation ended when the real wait remains active.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("operationDisposition")]
    public RuntimeLifecycleDisposition? OperationDisposition { get; set; }

    /// <summary>Opaque awaited operation identifier.</summary>
    [JsonPropertyName("operationId")]
    public required string OperationId { get; set; }

    /// <summary>Awaited operation revision observed by this blocker.</summary>
    [JsonPropertyName("operationRevision")]
    public required long OperationRevision { get; set; }

    /// <summary>System-owned capability required by the progress owner.</summary>
    [JsonPropertyName("requirement")]
    public required string Requirement { get; set; }

    /// <summary>System that owns the awaited operation.</summary>
    [JsonPropertyName("system")]
    public required string System { get; set; }

    /// <summary>Timing of this dependency wait.</summary>
    [JsonPropertyName("timing")]
    public required RuntimeLifecycleBlockerTiming Timing { get; set; }
}

/// <summary>Immutable causal origin of a progress record with independent identity and settlement.</summary>
/// <remarks>Nested data type for <c>RuntimeLifecycleProgressOrigin</c>.</remarks>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleProgressOrigin
{
    /// <summary>Kind of the originating progress record.</summary>
    [JsonPropertyName("kind")]
    public required string Kind { get; set; }

    /// <summary>Identifier of the originating progress record.</summary>
    [JsonPropertyName("progressId")]
    public required string ProgressId { get; set; }
}

/// <summary>Fixed timestamps for one observed progress revision.</summary>
/// <remarks>Nested data type for <c>RuntimeLifecycleProgressTiming</c>.</remarks>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
internal sealed partial class RuntimeLifecycleProgressTiming
{
    /// <summary>ISO 8601 timestamp when the current phase began.</summary>
    [JsonPropertyName("phaseStartedAt")]
    public required DateTimeOffset PhaseStartedAt { get; set; }

    /// <summary>ISO 8601 timestamp when the progress record began.</summary>
    [JsonPropertyName("startedAt")]
    public required DateTimeOffset StartedAt { get; set; }

    /// <summary>ISO 8601 timestamp when this progress revision was recorded.</summary>
    [JsonPropertyName("transitionedAt")]
    public required DateTimeOffset TransitionedAt { get; set; }
}

/// <summary>How a settled lifecycle operation or progress record ended.</summary>
[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
internal readonly struct RuntimeLifecycleDisposition : IEquatable<RuntimeLifecycleDisposition>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="RuntimeLifecycleDisposition"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="RuntimeLifecycleDisposition"/>.</param>
    [JsonConstructor]
    public RuntimeLifecycleDisposition(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="RuntimeLifecycleDisposition"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The work completed.</summary>
    public static RuntimeLifecycleDisposition Succeeded { get; } = new("succeeded");

    /// <summary>The work ended without reaching its intended boundary.</summary>
    public static RuntimeLifecycleDisposition Failed { get; } = new("failed");

    /// <summary>Cancellation ended the work.</summary>
    public static RuntimeLifecycleDisposition Cancelled { get; } = new("cancelled");

    /// <summary>A deadline ended the work.</summary>
    public static RuntimeLifecycleDisposition TimedOut { get; } = new("timed_out");

    /// <summary>Newer work replaced this work.</summary>
    public static RuntimeLifecycleDisposition Superseded { get; } = new("superseded");

    /// <summary>The owner stopped observing the work without settling it.</summary>
    public static RuntimeLifecycleDisposition Abandoned { get; } = new("abandoned");

    /// <summary>Returns a value indicating whether two <see cref="RuntimeLifecycleDisposition"/> instances are equivalent.</summary>
    public static bool operator ==(RuntimeLifecycleDisposition left, RuntimeLifecycleDisposition right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="RuntimeLifecycleDisposition"/> instances are not equivalent.</summary>
    public static bool operator !=(RuntimeLifecycleDisposition left, RuntimeLifecycleDisposition right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is RuntimeLifecycleDisposition other && Equals(other);

    /// <inheritdoc />
    public bool Equals(RuntimeLifecycleDisposition other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{RuntimeLifecycleDisposition}"/> for serializing <see cref="RuntimeLifecycleDisposition"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<RuntimeLifecycleDisposition>
    {
        /// <inheritdoc />
        public override RuntimeLifecycleDisposition Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, RuntimeLifecycleDisposition value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(RuntimeLifecycleDisposition));
        }
    }
}