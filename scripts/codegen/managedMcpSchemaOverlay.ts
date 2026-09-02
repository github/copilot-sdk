/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs from "fs";

type SchemaNode = Record<string, unknown>;

interface ManagedMcpApiOverlay {
  ManagedMcpServerConfig: SchemaNode;
  McpHeadersHandlePendingHeadersRefreshRequest: SchemaNode;
  managedMcpServers: SchemaNode;
  McpServerSource: SchemaNode;
  mcpServerDisplayName: SchemaNode;
}

interface ManagedMcpEventOverlay {
  McpServerSource: SchemaNode;
  mcpServersLoadedDisplayName: SchemaNode;
}

interface ManagedMcpSchemaOverlay {
  "api.schema.json": ManagedMcpApiOverlay;
  "session-events.schema.json": ManagedMcpEventOverlay;
  legacy: {
    "api.schema.json": {
      McpHeadersHandlePendingHeadersRefreshRequest: SchemaNode;
      McpServerSource: SchemaNode;
    };
    "session-events.schema.json": {
      McpServerSource: SchemaNode;
    };
  };
}

const overlay = JSON.parse(
  fs.readFileSync(
    new URL("./managed-mcp-schema-overlay.json", import.meta.url),
    "utf8",
  ),
) as ManagedMcpSchemaOverlay;

function stableStringify(value: unknown): string {
  if (Array.isArray(value)) {
    return `[${value.map(stableStringify).join(",")}]`;
  }
  if (value !== null && typeof value === "object") {
    return `{${Object.entries(value as SchemaNode)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([key, entry]) => `${JSON.stringify(key)}:${stableStringify(entry)}`)
      .join(",")}}`;
  }
  return JSON.stringify(value);
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function installNode(
  parent: SchemaNode,
  key: string,
  desired: SchemaNode,
  isKnownLegacy: (current: unknown) => boolean,
  allowMissing: boolean,
): void {
  const current = parent[key];
  if (current === undefined) {
    if (!allowMissing) {
      throw new Error(
        `Managed MCP schema overlay expected an upstream node at ${key}. ` +
          "Update or remove scripts/codegen/managed-mcp-schema-overlay.json.",
      );
    }
    parent[key] = clone(desired);
    return;
  }
  if (stableStringify(current) === stableStringify(desired)) {
    parent[key] = clone(desired);
    return;
  }
  if (!isKnownLegacy(current)) {
    throw new Error(
      `Managed MCP schema overlay conflicts with an unknown upstream shape at ${key}. ` +
        "Update or remove scripts/codegen/managed-mcp-schema-overlay.json.",
    );
  }
  parent[key] = clone(desired);
}

function matches(expected: unknown): (current: unknown) => boolean {
  return (current) => stableStringify(current) === stableStringify(expected);
}

function requireDefinitions(schema: SchemaNode): SchemaNode {
  const definitions = schema.definitions;
  if (
    definitions === null ||
    typeof definitions !== "object" ||
    Array.isArray(definitions)
  ) {
    throw new Error(
      "Managed MCP schema overlay requires a definitions object.",
    );
  }
  return definitions as SchemaNode;
}

function requireProperties(
  definitions: SchemaNode,
  definitionName: string,
): SchemaNode {
  const definition = definitions[definitionName] as SchemaNode | undefined;
  const properties = definition?.properties;
  if (
    properties === null ||
    typeof properties !== "object" ||
    Array.isArray(properties)
  ) {
    throw new Error(
      `Managed MCP schema overlay requires ${definitionName}.properties to be an object.`,
    );
  }
  return properties as SchemaNode;
}

function applyApiOverlay(schema: SchemaNode): void {
  const definitions = requireDefinitions(schema);
  const api = overlay["api.schema.json"];
  const legacy = overlay.legacy["api.schema.json"];
  installNode(
    definitions,
    "ManagedMcpServerConfig",
    api.ManagedMcpServerConfig,
    () => false,
    true,
  );
  installNode(
    definitions,
    "McpHeadersHandlePendingHeadersRefreshRequest",
    api.McpHeadersHandlePendingHeadersRefreshRequest,
    matches(legacy.McpHeadersHandlePendingHeadersRefreshRequest),
    false,
  );
  installNode(
    definitions,
    "McpServerSource",
    api.McpServerSource,
    matches(legacy.McpServerSource),
    false,
  );
  installNode(
    requireProperties(definitions, "SessionOpenOptions"),
    "managedMcpServers",
    api.managedMcpServers,
    () => false,
    true,
  );
  installNode(
    requireProperties(definitions, "McpServer"),
    "displayName",
    api.mcpServerDisplayName,
    () => false,
    true,
  );
}

function applySessionEventsOverlay(schema: SchemaNode): void {
  const definitions = requireDefinitions(schema);
  const events = overlay["session-events.schema.json"];
  const legacy = overlay.legacy["session-events.schema.json"];
  installNode(
    definitions,
    "McpServerSource",
    events.McpServerSource,
    matches(legacy.McpServerSource),
    false,
  );
  installNode(
    requireProperties(definitions, "McpServersLoadedServer"),
    "displayName",
    events.mcpServersLoadedDisplayName,
    () => false,
    true,
  );
}

/**
 * Applies the additive managed MCP contract from copilot-agent-runtime#17210.
 *
 * Remove this overlay after the pinned @github/copilot package publishes the
 * same schema. Unknown upstream shapes fail instead of being silently replaced.
 */
export function applyManagedMcpSchemaOverlay<T>(
  schema: T,
  fileName: string,
): T {
  if (schema === null || typeof schema !== "object" || Array.isArray(schema)) {
    throw new Error(`Managed MCP schema overlay cannot process ${fileName}.`);
  }
  if (fileName === "api.schema.json") {
    applyApiOverlay(schema as SchemaNode);
  } else if (fileName === "session-events.schema.json") {
    applySessionEventsOverlay(schema as SchemaNode);
  }
  return schema;
}
