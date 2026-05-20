/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json.Serialization;

namespace GitHub.Copilot.SDK.Test;

[JsonSourceGenerationOptions(System.Text.Json.JsonSerializerDefaults.Web)]
[JsonSerializable(typeof(string[]))]
[JsonSerializable(typeof(Dictionary<string, object>))]
[JsonSerializable(typeof(Dictionary<string, string>))]
[JsonSerializable(typeof(McpServerConfig))]
[JsonSerializable(typeof(McpHttpServerConfig))]
[JsonSerializable(typeof(McpStdioServerConfig))]
internal partial class TestSharedJsonContext : JsonSerializerContext;
