/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

export type JsonObject = Record<string, unknown>;

export type CanonicalToolCall = {
  id: string;
  type: "function";
  function: { name: string; arguments: string };
};

export type CanonicalMessage = {
  role: "system" | "user" | "assistant" | "tool";
  content?: string | unknown[] | null;
  tool_call_id?: string;
  tool_calls?: CanonicalToolCall[];
};

export function functionToolCalls(message: unknown): CanonicalToolCall[] {
  if (!isObject(message) || !Array.isArray(message.tool_calls)) return [];
  return message.tool_calls.filter(
    (toolCall): toolCall is CanonicalToolCall =>
      isObject(toolCall) &&
      typeof toolCall.id === "string" &&
      toolCall.type === "function" &&
      isObject(toolCall.function) &&
      typeof toolCall.function.name === "string" &&
      typeof toolCall.function.arguments === "string",
  );
}

export function formatSseEvent(type: string, data: unknown): string {
  return `event: ${type}\ndata: ${JSON.stringify(data)}\n\n`;
}

export function isObject(value: unknown): value is JsonObject {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}
