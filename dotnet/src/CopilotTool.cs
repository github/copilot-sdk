/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;

namespace GitHub.Copilot;

/// <summary>
/// Provides helpers for defining Copilot tools.
/// </summary>
public static class CopilotTool
{
    /// <summary>The key used in <see cref="AITool.AdditionalProperties"/> to indicate that a tool intentionally overrides a built-in Copilot tool with the same name.</summary>
    internal const string OverridesBuiltInToolKey = "is_override";

    /// <summary>The key used in <see cref="AITool.AdditionalProperties"/> to indicate that a tool can execute without a permission prompt.</summary>
    internal const string SkipPermissionKey = "skip_permission";

    /// <summary>
    /// Defines a tool for use in a <see cref="CopilotSession"/>.
    /// </summary>
    /// <param name="method">The delegate to invoke when the tool is called.</param>
    /// <param name="factoryOptions">The Microsoft.Extensions.AI options used to create the function.</param>
    /// <param name="toolOptions">Copilot-specific tool options.</param>
    /// <returns>An <see cref="AIFunction"/> that can be added to <see cref="SessionConfigBase.Tools"/>.</returns>
    /// <remarks>
    /// This is a helper on top of <see cref="AIFunctionFactory.Create(Delegate, AIFunctionFactoryOptions)"/> that applies additional configuration to support
    /// Copilot tools, such as binding a <see cref="ToolInvocation"/> parameter and adding Copilot-specific metadata properties based on the provided
    /// <see cref="CopilotToolOptions"/>. Any <see cref="AIFunction"/> may be used as a Copilot tool; this helper simply provides additional conveniences
    /// for tools that opt in to advanced features.
    /// </remarks>
    public static AIFunction DefineTool(
        Delegate method,
        CopilotToolOptions? toolOptions = null,
        AIFunctionFactoryOptions? factoryOptions = null)
    {
        ArgumentNullException.ThrowIfNull(method);

        factoryOptions ??= new();

        ApplyToolOptions(factoryOptions, toolOptions);
        ApplyToolInvocationBinding(factoryOptions);

        return AIFunctionFactory.Create(method, factoryOptions);

        static void ApplyToolInvocationBinding(AIFunctionFactoryOptions factoryOptions)
        {
            var configureParameterBinding = factoryOptions.ConfigureParameterBinding;
            factoryOptions.ConfigureParameterBinding = pi =>
            {
                var bindingOptions = configureParameterBinding?.Invoke(pi) ?? default;

                if (bindingOptions.BindParameter is null &&
                    !bindingOptions.ExcludeFromSchema &&
                    pi.ParameterType == typeof(ToolInvocation))
                {
                    return new AIFunctionFactoryOptions.ParameterBindingOptions
                    {
                        ExcludeFromSchema = true,
                        BindParameter = static (pi, arguments) =>
                        {
                            // CopilotClient/CopilotSession attach this context object before invoking the AIFunction.
                            if (arguments.Context is not null &&
                                arguments.Context.TryGetValue(typeof(ToolInvocation), out var invocation) &&
                                invocation is ToolInvocation toolInvocation)
                            {
                                return toolInvocation;
                            }

                            if (pi.HasDefaultValue)
                            {
                                return null;
                            }

                            throw new InvalidOperationException($"No {nameof(ToolInvocation)} was provided for the tool call.");
                        }
                    };
                }

                return bindingOptions;
            };
        }

        static void ApplyToolOptions(AIFunctionFactoryOptions factoryOptions, CopilotToolOptions? toolOptions)
        {
            if (toolOptions is not null && (toolOptions.OverridesBuiltInTool || toolOptions.SkipPermission))
            {
                Dictionary<string, object?> additionalProperties = new(StringComparer.Ordinal);
                if (factoryOptions.AdditionalProperties is not null)
                {
                    foreach (var (key, value) in factoryOptions.AdditionalProperties)
                    {
                        additionalProperties[key] = value;
                    }
                }

                if (toolOptions.OverridesBuiltInTool)
                {
                    additionalProperties[OverridesBuiltInToolKey] = true;
                }

                if (toolOptions.SkipPermission)
                {
                    additionalProperties[SkipPermissionKey] = true;
                }

                factoryOptions.AdditionalProperties = additionalProperties;
            }
        }
    }

}

/// <summary>
/// Copilot-specific options for tools defined with <see cref="CopilotTool"/>.
/// </summary>
public sealed class CopilotToolOptions
{
    /// <summary>
    /// Gets or sets a value indicating whether this tool intentionally overrides a built-in Copilot tool with the same name.
    /// </summary>
    /// <remarks>
    /// When a <see cref="CopilotToolOptions"/> with <see cref="OverridesBuiltInTool"/> set to true is used to define a tool, 
    /// the resulting <see cref="AIFunction"/> will include "is_override": true in its <see cref="AITool.AdditionalProperties"/>.
    /// </remarks>
    public bool OverridesBuiltInTool { get; set; }

    /// <summary>
    /// Gets or sets a value indicating whether this tool can execute without a permission prompt.
    /// </summary>
    /// <remarks>
    /// When a <see cref="CopilotToolOptions"/> with <see cref="SkipPermission"/> set to true is used to define a tool, 
    /// the resulting <see cref="AIFunction"/> will include "skip_permission": true in its <see cref="AITool.AdditionalProperties"/>.
    /// </remarks>
    public bool SkipPermission { get; set; }
}
