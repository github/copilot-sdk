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

interface ContractNode {
  parent: SchemaNode;
  key: string;
  desired: SchemaNode;
  legacy?: SchemaNode;
}

function installContract(nodes: ContractNode[]): void {
  if (
    nodes.every(
      ({ parent, key, desired }) =>
        stableStringify(parent[key]) === stableStringify(desired),
    )
  ) {
    return;
  }
  if (
    !nodes.every(
      ({ parent, key, legacy }) =>
        stableStringify(parent[key]) === stableStringify(legacy),
    )
  ) {
    const missing = nodes.find(
      ({ parent, key, legacy }) =>
        parent[key] === undefined && legacy !== undefined,
    );
    throw new Error(
      (missing
        ? `Managed MCP schema overlay expected an upstream node at ${missing.key}. `
        : "Managed MCP schema overlay conflicts with an unknown upstream shape or partial contract. ") +
        "Update or remove scripts/codegen/managed-mcp-schema-overlay.json.",
    );
  }
  for (const { parent, key, desired } of nodes) {
    parent[key] = clone(desired);
  }
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
  installContract([
    {
      parent: definitions,
      key: "ManagedMcpServerConfig",
      desired: api.ManagedMcpServerConfig,
    },
    {
      parent: definitions,
      key: "McpHeadersHandlePendingHeadersRefreshRequest",
      desired: api.McpHeadersHandlePendingHeadersRefreshRequest,
      legacy: legacy.McpHeadersHandlePendingHeadersRefreshRequest,
    },
    {
      parent: definitions,
      key: "McpServerSource",
      desired: api.McpServerSource,
      legacy: legacy.McpServerSource,
    },
    {
      parent: requireProperties(definitions, "SessionOpenOptions"),
      key: "managedMcpServers",
      desired: api.managedMcpServers,
    },
    {
      parent: requireProperties(definitions, "McpServer"),
      key: "displayName",
      desired: api.mcpServerDisplayName,
    },
  ]);
}

function applySessionEventsOverlay(schema: SchemaNode): void {
  const definitions = requireDefinitions(schema);
  const events = overlay["session-events.schema.json"];
  const legacy = overlay.legacy["session-events.schema.json"];
  installContract([
    {
      parent: definitions,
      key: "McpServerSource",
      desired: events.McpServerSource,
      legacy: legacy.McpServerSource,
    },
    {
      parent: requireProperties(definitions, "McpServersLoadedServer"),
      key: "displayName",
      desired: events.mcpServersLoadedDisplayName,
    },
  ]);
}

/**
 * Applies the additive managed MCP contract from copilot-agent-runtime#17210.
 *
 * Remove this overlay after the pinned @github/copilot package publishes the
 * same schema. Only complete legacy or target contracts are accepted; validation
 * happens before mutation so unknown or partially published shapes fail closed.
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
