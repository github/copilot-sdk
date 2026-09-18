/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs from "fs";

type SchemaNode = Record<string, unknown>;

interface ConnectorSessionApiOverlay {
  sessionConnectors: SchemaNode;
  definitions: Record<string, SchemaNode>;
}

const overlay = JSON.parse(
  fs.readFileSync(
    new URL("./connector-session-api-overlay.json", import.meta.url),
    "utf8",
  ),
) as ConnectorSessionApiOverlay;

function stableStringify(value: unknown): string {
  if (Array.isArray(value)) {
    return `[${value.map(stableStringify).join(",")}]`;
  }
  if (value !== null && typeof value === "object") {
    return `{${Object.entries(value as SchemaNode)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([key, entry]) => `${JSON.stringify(key)}:${stableStringify(entry)}`)
      .join(",")}}`;
  }
  return JSON.stringify(value) ?? "undefined";
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function requireObject(parent: SchemaNode, key: string): SchemaNode {
  const value = parent[key];
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    throw new Error(
      `Connector session API overlay requires ${key} to be an object.`,
    );
  }
  return value as SchemaNode;
}

/**
 * Applies the public Connector session contract pending schema publication.
 *
 * Remove this overlay after the pinned runtime release publishes the contract.
 * Only the complete pre-contract or exact target shape is accepted.
 */
export function applyConnectorSessionApiOverlay<T>(
  schema: T,
  fileName: string,
): T {
  if (fileName !== "api.schema.json") {
    return schema;
  }
  if (schema === null || typeof schema !== "object" || Array.isArray(schema)) {
    throw new Error(
      `Connector session API overlay cannot process ${fileName}.`,
    );
  }

  const root = schema as SchemaNode;
  const session = requireObject(root, "session");
  const definitions = requireObject(root, "definitions");
  const nodes = [
    {
      parent: session,
      key: "connectors",
      desired: overlay.sessionConnectors,
    },
    ...Object.entries(overlay.definitions).map(([key, desired]) => ({
      parent: definitions,
      key,
      desired,
    })),
  ];

  const isLegacy = nodes.every(({ parent, key }) => parent[key] === undefined);
  const isTarget = nodes.every(
    ({ parent, key, desired }) =>
      stableStringify(parent[key]) === stableStringify(desired),
  );
  if (!isLegacy && !isTarget) {
    throw new Error(
      "Connector session API overlay found a partial or unknown upstream contract. " +
        "Update or remove scripts/codegen/connector-session-api-overlay.json.",
    );
  }
  if (isLegacy) {
    for (const { parent, key, desired } of nodes) {
      parent[key] = clone(desired);
    }
  }
  return schema;
}
