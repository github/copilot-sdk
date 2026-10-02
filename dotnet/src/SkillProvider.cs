/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics.CodeAnalysis;

namespace GitHub.Copilot;

/// <summary>
/// Supplies in-memory, text-only skills to the native runtime's built-in <c>skill</c> tool.
/// </summary>
/// <remarks>
/// <para>
/// This experimental API uses internal native runtime callbacks. The runtime requests catalog
/// metadata first and reads a skill's Markdown lazily when it is invoked. No skill files are
/// written, and supporting files, scripts, and other filesystem assets are not provided.
/// </para>
/// <para>
/// Set <see cref="SessionConfigBase.SkillProvider"/> before creating or resuming a session.
/// The binding is not persisted and must be supplied again on resume. Implementations should
/// support concurrent callbacks and honor the supplied cancellation token.
/// </para>
/// </remarks>
[Experimental(Diagnostics.Experimental)]
public abstract class SkillProvider
{
    /// <summary>
    /// Lists skill metadata without loading the skills' Markdown bodies.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token for the runtime callback.</param>
    /// <returns>
    /// The skill catalog, or an empty list when no skills are available. Names must be unique
    /// using a case-insensitive comparison and satisfy <see cref="SkillProviderDescriptor.Name"/>.
    /// The native runtime limits catalogs to 1,024 descriptors and 1 MiB of aggregate metadata.
    /// </returns>
    public abstract Task<IReadOnlyList<SkillProviderDescriptor>> ListAsync(CancellationToken cancellationToken = default);

    /// <summary>
    /// Reads the complete <c>SKILL.md</c>-format Markdown for a catalog entry.
    /// </summary>
    /// <param name="name">The name of the skill to read.</param>
    /// <param name="cancellationToken">Cancellation token for the runtime callback.</param>
    /// <returns>
    /// The complete Markdown, including YAML frontmatter matching the metadata returned by
    /// <see cref="ListAsync"/>. The native runtime rejects inconsistent metadata.
    /// The complete response text must not exceed 1 MiB when encoded as UTF-8.
    /// </returns>
    /// <remarks>Throw if the named skill is unavailable; do not return a file path.</remarks>
    public abstract Task<string> ReadAsync(string name, CancellationToken cancellationToken = default);
}

/// <summary>
/// Catalog metadata for an experimental, text-only native skill supplied by <see cref="SkillProvider"/>.
/// </summary>
/// <remarks>
/// The native runtime validates the catalog and its agreement with each skill's Markdown
/// frontmatter. Provider skills are pathless: the runtime assigns source <c>sdk</c> and an empty path.
/// </remarks>
[Experimental(Diagnostics.Experimental)]
public sealed class SkillProviderDescriptor
{
    /// <summary>
    /// Gets the pathless skill name. Must match <c>^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$</c>
    /// and be unique in the catalog using a case-insensitive comparison.
    /// </summary>
    public required string Name { get; init; }

    /// <summary>Gets the description advertised before the skill's Markdown is read.</summary>
    public required string Description { get; init; }

    /// <summary>
    /// Gets whether the skill is user-invocable. When omitted, the native runtime default applies.
    /// Must agree with the Markdown frontmatter's <c>user-invocable</c> value.
    /// </summary>
    public bool? UserInvocable { get; init; }

    /// <summary>
    /// Gets whether model invocation is disabled. When omitted, the native runtime default applies.
    /// Must agree with the Markdown frontmatter's <c>disable-model-invocation</c> value.
    /// </summary>
    public bool? DisableModelInvocation { get; init; }

    /// <summary>
    /// Gets the optional argument hint. Must agree with the Markdown frontmatter's
    /// <c>argument-hint</c> value.
    /// </summary>
    public string? ArgumentHint { get; init; }
}
