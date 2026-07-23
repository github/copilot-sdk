/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { ChatCompletion } from "openai/resources/chat/completions";
import {
  CanonicalMessage,
  CanonicalToolCall,
  formatSseEvent,
  functionToolCalls,
  isObject,
  JsonObject,
} from "./modelProtocolAdapterShared";

export const anthropicMessagesEndpoint = "/v1/messages";

type CanonicalContentPart =
  | { type: "text"; text: string }
  | { type: "image_url"; image_url: { url: string } }
  | {
      type: "file";
      file: { file_data: string; filename?: string };
    };

type AnthropicContentBlock =
  | { type: "text"; text: string; citations?: null }
  | {
      type: "image" | "document";
      source?: { type?: string; media_type?: string; data?: string };
    }
  | { type: "tool_use"; id: string; name: string; input: unknown }
  | {
      type: "tool_result";
      tool_use_id?: string;
      content?: string | Array<{ type?: string; text?: string }>;
    };

type AnthropicMessageParam = {
  role: "user" | "assistant";
  content: string | AnthropicContentBlock[];
};

type AnthropicRequest = {
  model: string;
  messages: AnthropicMessageParam[];
  system?: string | Array<{ type?: string; text?: string }>;
  max_tokens?: number;
  temperature?: number;
  top_p?: number;
  stream?: boolean;
  tools?: Array<{
    name: string;
    description?: string;
    input_schema?: JsonObject;
  }>;
  tool_choice?:
    | { type: "auto" | "any" | "none" }
    | { type: "tool"; name: string };
};

type AnthropicStopReason =
  | "end_turn"
  | "max_tokens"
  | "stop_sequence"
  | "tool_use"
  | "refusal";

export type AnthropicMessage = {
  id: string;
  type: "message";
  role: "assistant";
  content: Array<
    | { type: "text"; text: string; citations: null }
    | { type: "tool_use"; id: string; name: string; input: unknown }
  >;
  model: string;
  stop_reason: AnthropicStopReason | null;
  stop_sequence: string | null;
  usage: {
    input_tokens: number;
    output_tokens: number;
    cache_creation_input_tokens: number | null;
    cache_read_input_tokens: number | null;
  };
};

const finishReasonToStopReason: Record<string, AnthropicStopReason> = {
  stop: "end_turn",
  length: "max_tokens",
  tool_calls: "tool_use",
  function_call: "tool_use",
  content_filter: "refusal",
};

export function anthropicMessagesRequestToChatCompletion(
  requestBody: string,
): string {
  const request = JSON.parse(requestBody) as AnthropicRequest;
  const messages: CanonicalMessage[] = [];

  const system = anthropicSystemToString(request.system);
  if (system) messages.push({ role: "system", content: system });

  for (const message of request.messages) {
    messages.push(...convertAnthropicMessage(message));
  }

  return JSON.stringify({
    model: request.model,
    messages,
    ...(request.max_tokens !== undefined
      ? { max_tokens: request.max_tokens }
      : {}),
    ...(request.temperature !== undefined
      ? { temperature: request.temperature }
      : {}),
    ...(request.top_p !== undefined ? { top_p: request.top_p } : {}),
    ...(request.stream !== undefined ? { stream: request.stream } : {}),
    ...(request.tools
      ? {
          tools: request.tools.map((tool) => ({
            type: "function",
            function: {
              name: tool.name,
              ...(tool.description ? { description: tool.description } : {}),
              parameters: tool.input_schema ?? {
                type: "object",
                properties: {},
              },
            },
          })),
        }
      : {}),
    ...(request.tool_choice
      ? { tool_choice: convertToolChoice(request.tool_choice) }
      : {}),
  });
}

function anthropicSystemToString(
  system: AnthropicRequest["system"],
): string | undefined {
  if (typeof system === "string") return system;
  if (!Array.isArray(system)) return undefined;
  return system
    .map((block) => (typeof block.text === "string" ? block.text : ""))
    .filter(Boolean)
    .join("\n");
}

function convertAnthropicMessage(
  message: AnthropicMessageParam,
): CanonicalMessage[] {
  return message.role === "user"
    ? convertAnthropicUserMessage(message)
    : convertAnthropicAssistantMessage(message);
}

function normalizeContent(
  content: AnthropicMessageParam["content"],
): AnthropicContentBlock[] {
  return typeof content === "string"
    ? [{ type: "text", text: content }]
    : content;
}

function convertAnthropicUserMessage(
  message: AnthropicMessageParam,
): CanonicalMessage[] {
  const result: CanonicalMessage[] = [];
  const contentParts: CanonicalContentPart[] = [];

  const flushUserContent = () => {
    if (contentParts.length === 0) return;
    const onlyText = contentParts.every((part) => part.type === "text");
    result.push({
      role: "user",
      content: onlyText
        ? contentParts
            .map((part) => (part.type === "text" ? part.text : ""))
            .join("\n")
        : [...contentParts],
    });
    contentParts.length = 0;
  };

  for (const block of normalizeContent(message.content)) {
    if (block.type === "text") {
      contentParts.push({ type: "text", text: block.text });
    } else if (
      (block.type === "image" || block.type === "document") &&
      block.source?.type === "base64" &&
      block.source.data
    ) {
      const dataUrl = `data:${
        block.source.media_type ??
        (block.type === "image" ? "image/png" : "application/pdf")
      };base64,${block.source.data}`;
      contentParts.push(
        block.type === "image"
          ? { type: "image_url", image_url: { url: dataUrl } }
          : { type: "file", file: { file_data: dataUrl } },
      );
    } else if (block.type === "tool_result") {
      flushUserContent();
      result.push({
        role: "tool",
        tool_call_id: block.tool_use_id ?? "",
        content: anthropicToolResultContent(block.content),
      });
    }
  }

  flushUserContent();
  return result;
}

function convertAnthropicAssistantMessage(
  message: AnthropicMessageParam,
): CanonicalMessage[] {
  const text: string[] = [];
  const toolCalls: CanonicalToolCall[] = [];
  for (const block of normalizeContent(message.content)) {
    if (block.type === "text") {
      text.push(block.text);
    } else if (block.type === "tool_use") {
      toolCalls.push({
        id: block.id,
        type: "function",
        function: {
          name: block.name,
          arguments: JSON.stringify(block.input ?? {}),
        },
      });
    }
  }

  return [
    {
      role: "assistant",
      content: text.length ? text.join("") : null,
      ...(toolCalls.length ? { tool_calls: toolCalls } : {}),
    },
  ];
}

function anthropicToolResultContent(content: unknown): string {
  if (typeof content === "string") return content;
  if (!Array.isArray(content)) return "";
  return content
    .map((part) =>
      isObject(part) && typeof part.text === "string" ? part.text : "",
    )
    .filter(Boolean)
    .join("\n");
}

function convertToolChoice(
  choice: NonNullable<AnthropicRequest["tool_choice"]>,
): unknown {
  switch (choice.type) {
    case "auto":
      return "auto";
    case "any":
      return "required";
    case "none":
      return "none";
    case "tool":
      return { type: "function", function: { name: choice.name } };
  }
}

export function chatCompletionResponseToAnthropicMessage(
  response: ChatCompletion,
): AnthropicMessage {
  const content: AnthropicMessage["content"] = [];
  for (const choice of response.choices) {
    if (choice.message.content) {
      content.push({
        type: "text",
        text: choice.message.content,
        citations: null,
      });
    }
    for (const toolCall of functionToolCalls(choice.message)) {
      content.push({
        type: "tool_use",
        id: toolCall.id,
        name: toolCall.function.name,
        input: safeParseJson(toolCall.function.arguments),
      });
    }
  }

  const finishReason = response.choices.at(-1)?.finish_reason;
  return {
    id: response.id,
    type: "message",
    role: "assistant",
    content,
    model: response.model,
    stop_reason: finishReason
      ? (finishReasonToStopReason[finishReason] ?? null)
      : null,
    stop_sequence: null,
    usage: {
      input_tokens: response.usage?.prompt_tokens ?? 0,
      output_tokens: response.usage?.completion_tokens ?? 0,
      cache_creation_input_tokens: null,
      cache_read_input_tokens: null,
    },
  };
}

export function chatCompletionResponseToAnthropicSseChunks(
  response: ChatCompletion,
): string[] {
  const message = chatCompletionResponseToAnthropicMessage(response);
  const chunks = [
    formatSseEvent("message_start", {
      type: "message_start",
      message: {
        ...message,
        content: [],
        stop_reason: null,
        usage: { ...message.usage, output_tokens: 1 },
      },
    }),
  ];

  for (let index = 0; index < message.content.length; index++) {
    const block = message.content[index];
    if (block.type === "text") {
      chunks.push(
        formatSseEvent("content_block_start", {
          type: "content_block_start",
          index,
          content_block: { type: "text", text: "", citations: null },
        }),
        formatSseEvent("content_block_delta", {
          type: "content_block_delta",
          index,
          delta: { type: "text_delta", text: block.text },
        }),
      );
    } else {
      chunks.push(
        formatSseEvent("content_block_start", {
          type: "content_block_start",
          index,
          content_block: {
            type: "tool_use",
            id: block.id,
            name: block.name,
            input: {},
          },
        }),
        formatSseEvent("content_block_delta", {
          type: "content_block_delta",
          index,
          delta: {
            type: "input_json_delta",
            partial_json: JSON.stringify(block.input ?? {}),
          },
        }),
      );
    }
    chunks.push(
      formatSseEvent("content_block_stop", {
        type: "content_block_stop",
        index,
      }),
    );
  }

  chunks.push(
    formatSseEvent("message_delta", {
      type: "message_delta",
      delta: {
        stop_reason: message.stop_reason,
        stop_sequence: message.stop_sequence,
      },
      usage: { output_tokens: message.usage.output_tokens },
    }),
    formatSseEvent("message_stop", { type: "message_stop" }),
  );
  return chunks;
}

function safeParseJson(value: string): unknown {
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}
