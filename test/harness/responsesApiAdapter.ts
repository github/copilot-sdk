/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import type { ChatCompletion } from "openai/resources/chat/completions";
import { parseSseEvents } from "./sseParser";

export const responsesEndpoint = "/responses";

type JsonObject = Record<string, unknown>;

type CanonicalToolCall = {
  id: string;
  type: "function";
  function: { name: string; arguments: string };
};

type CanonicalMessage = {
  role: "system" | "user" | "assistant" | "tool";
  content?: string | unknown[] | null;
  tool_call_id?: string;
  tool_calls?: CanonicalToolCall[];
};

type ResponsesRequest = {
  model: string;
  instructions?: string;
  input?: string | JsonObject[];
  stream?: boolean;
  tools?: JsonObject[];
  tool_choice?: unknown;
  temperature?: number | null;
  top_p?: number | null;
  parallel_tool_calls?: boolean | null;
};

export type ResponsesApiResponse = {
  id: string;
  object: "response";
  created_at: number;
  model: string;
  status: "completed";
  output: JsonObject[];
  output_text: string;
  incomplete_details: JsonObject | null;
  error: null;
  instructions: null;
  metadata: null;
  parallel_tool_calls: boolean;
  temperature: null;
  tool_choice: "auto";
  tools: [];
  top_p: null;
  usage: {
    input_tokens: number;
    output_tokens: number;
    total_tokens: number;
    input_tokens_details: { cached_tokens: number };
    output_tokens_details: { reasoning_tokens: number };
  };
};

export function responsesApiRequestToChatCompletion(
  requestBody: string,
): string {
  const request = JSON.parse(requestBody) as ResponsesRequest;
  const messages: CanonicalMessage[] = [];
  if (request.instructions) {
    messages.push({ role: "system", content: request.instructions });
  }

  if (typeof request.input === "string") {
    messages.push({ role: "user", content: request.input });
  } else {
    for (const item of request.input ?? []) {
      const converted = responseInputItemToCanonicalMessages(item);
      messages.push(...converted);
    }
  }

  return JSON.stringify({
    model: request.model,
    messages: coalesceAssistantMessages(messages),
    ...(request.tools ? { tools: convertResponsesTools(request.tools) } : {}),
    ...(request.tool_choice !== undefined
      ? { tool_choice: convertResponsesToolChoice(request.tool_choice) }
      : {}),
    ...(request.stream !== undefined ? { stream: request.stream } : {}),
    ...(request.temperature !== undefined && request.temperature !== null
      ? { temperature: request.temperature }
      : {}),
    ...(request.top_p !== undefined && request.top_p !== null
      ? { top_p: request.top_p }
      : {}),
    ...(request.parallel_tool_calls !== undefined &&
    request.parallel_tool_calls !== null
      ? { parallel_tool_calls: request.parallel_tool_calls }
      : {}),
  });
}

function responseInputItemToCanonicalMessages(
  item: JsonObject,
): CanonicalMessage[] {
  if (item.type === "function_call") {
    const callId =
      typeof item.call_id === "string"
        ? item.call_id
        : typeof item.id === "string"
          ? item.id
          : "";
    return [
      {
        role: "assistant",
        content: null,
        tool_calls: [
          {
            id: callId,
            type: "function",
            function: {
              name: typeof item.name === "string" ? item.name : "",
              arguments:
                typeof item.arguments === "string" ? item.arguments : "{}",
            },
          },
        ],
      },
    ];
  }

  if (item.type === "function_call_output") {
    return [
      {
        role: "tool",
        tool_call_id: typeof item.call_id === "string" ? item.call_id : "",
        content:
          typeof item.output === "string"
            ? item.output
            : JSON.stringify(item.output ?? ""),
      },
    ];
  }

  if (item.type === "reasoning") return [];

  if (
    item.type !== "message" &&
    item.role !== "user" &&
    item.role !== "assistant" &&
    item.role !== "system"
  ) {
    return [];
  }

  const role =
    item.role === "assistant" || item.role === "system" ? item.role : "user";
  if (typeof item.content === "string") {
    return [{ role, content: item.content }];
  }
  if (!Array.isArray(item.content)) return [{ role, content: "" }];

  const parts: unknown[] = [];
  for (const part of item.content) {
    if (!isObject(part)) continue;
    if (
      (part.type === "input_text" || part.type === "output_text") &&
      typeof part.text === "string"
    ) {
      parts.push({ type: "text", text: part.text });
    } else if (
      part.type === "input_image" &&
      typeof part.image_url === "string"
    ) {
      parts.push({
        type: "image_url",
        image_url: {
          url: part.image_url,
          ...(typeof part.detail === "string" ? { detail: part.detail } : {}),
        },
      });
    } else if (
      part.type === "input_file" &&
      typeof part.file_data === "string"
    ) {
      parts.push({
        type: "file",
        file: {
          file_data: part.file_data,
          ...(typeof part.filename === "string"
            ? { filename: part.filename }
            : {}),
        },
      });
    }
  }

  const onlyText = parts.every(
    (part) => isObject(part) && part.type === "text",
  );
  return [
    {
      role,
      content: onlyText
        ? parts
            .map((part) =>
              isObject(part) && typeof part.text === "string" ? part.text : "",
            )
            .join("")
        : parts,
    },
  ];
}

function coalesceAssistantMessages(
  messages: CanonicalMessage[],
): CanonicalMessage[] {
  const result: CanonicalMessage[] = [];
  for (const message of messages) {
    const previous = result[result.length - 1];
    if (message.role === "assistant" && previous?.role === "assistant") {
      const previousText =
        typeof previous.content === "string" ? previous.content : "";
      const currentText =
        typeof message.content === "string" ? message.content : "";
      previous.content = `${previousText}${currentText}` || null;
      const toolCalls = [
        ...(previous.tool_calls ?? []),
        ...(message.tool_calls ?? []),
      ];
      if (toolCalls.length) previous.tool_calls = toolCalls;
    } else {
      result.push(message);
    }
  }
  return result;
}

function convertResponsesTools(tools: JsonObject[]): JsonObject[] {
  return tools
    .filter((tool) => tool.type === "function" && typeof tool.name === "string")
    .map((tool) => ({
      type: "function",
      function: {
        name: tool.name,
        ...(typeof tool.description === "string"
          ? { description: tool.description }
          : {}),
        ...(isObject(tool.parameters) ? { parameters: tool.parameters } : {}),
        ...(typeof tool.strict === "boolean" ? { strict: tool.strict } : {}),
      },
    }));
}

function convertResponsesToolChoice(toolChoice: unknown): unknown {
  if (
    toolChoice === "auto" ||
    toolChoice === "none" ||
    toolChoice === "required"
  ) {
    return toolChoice;
  }
  if (
    isObject(toolChoice) &&
    toolChoice.type === "function" &&
    typeof toolChoice.name === "string"
  ) {
    return {
      type: "function",
      function: { name: toolChoice.name },
    };
  }
  return undefined;
}

export function chatCompletionResponseToResponsesApiMessage(
  response: ChatCompletion,
): ResponsesApiResponse {
  const output: JsonObject[] = [];
  const outputText: string[] = [];

  for (const choice of response.choices) {
    if (choice.message.content) {
      const text = choice.message.content;
      outputText.push(text);
      output.push({
        type: "message",
        id: `msg_${randomUUID()}`,
        role: "assistant",
        status: "completed",
        content: [
          {
            type: "output_text",
            text,
            annotations: [],
          },
        ],
      });
    }
    for (const toolCall of functionToolCalls(choice.message)) {
      output.push({
        type: "function_call",
        id: `fc_${toolCall.id}`,
        call_id: toolCall.id,
        name: toolCall.function.name,
        arguments: toolCall.function.arguments,
        status: "completed",
      });
    }
  }

  const finishReason = response.choices[0]?.finish_reason;
  return {
    id: response.id,
    object: "response",
    created_at: response.created,
    model: response.model,
    status: "completed",
    output,
    output_text: outputText.join(""),
    incomplete_details:
      finishReason === "length"
        ? { reason: "max_output_tokens" }
        : finishReason === "content_filter"
          ? { reason: "content_filter" }
          : null,
    error: null,
    instructions: null,
    metadata: null,
    parallel_tool_calls: false,
    temperature: null,
    tool_choice: "auto",
    tools: [],
    top_p: null,
    usage: {
      input_tokens: response.usage?.prompt_tokens ?? 0,
      output_tokens: response.usage?.completion_tokens ?? 0,
      total_tokens: response.usage?.total_tokens ?? 0,
      input_tokens_details: {
        cached_tokens:
          response.usage?.prompt_tokens_details?.cached_tokens ?? 0,
      },
      output_tokens_details: {
        reasoning_tokens:
          response.usage?.completion_tokens_details?.reasoning_tokens ?? 0,
      },
    },
  };
}

export function chatCompletionResponseToResponsesApiSseChunks(
  response: ChatCompletion,
): string[] {
  const fullResponse = chatCompletionResponseToResponsesApiMessage(response);
  const skeleton = {
    ...fullResponse,
    output: [],
    output_text: "",
    usage: undefined,
  };
  const chunks: string[] = [];
  let sequenceNumber = 0;
  const event = (type: string, data: JsonObject) =>
    formatSseEvent(type, {
      type,
      sequence_number: sequenceNumber++,
      ...data,
    });

  chunks.push(
    event("response.created", { response: skeleton }),
    event("response.in_progress", { response: skeleton }),
  );

  for (
    let outputIndex = 0;
    outputIndex < fullResponse.output.length;
    outputIndex++
  ) {
    const item = fullResponse.output[outputIndex];
    chunks.push(
      event("response.output_item.added", {
        output_index: outputIndex,
        item,
      }),
    );

    if (item.type === "message" && Array.isArray(item.content)) {
      for (
        let contentIndex = 0;
        contentIndex < item.content.length;
        contentIndex++
      ) {
        const part = item.content[contentIndex];
        chunks.push(
          event("response.content_part.added", {
            item_id: item.id,
            output_index: outputIndex,
            content_index: contentIndex,
            part,
          }),
        );
        if (
          isObject(part) &&
          part.type === "output_text" &&
          typeof part.text === "string"
        ) {
          chunks.push(
            event("response.output_text.delta", {
              item_id: item.id,
              output_index: outputIndex,
              content_index: contentIndex,
              delta: part.text,
              logprobs: [],
            }),
            event("response.output_text.done", {
              item_id: item.id,
              output_index: outputIndex,
              content_index: contentIndex,
              text: part.text,
              logprobs: [],
            }),
          );
        }
        chunks.push(
          event("response.content_part.done", {
            item_id: item.id,
            output_index: outputIndex,
            content_index: contentIndex,
            part,
          }),
        );
      }
    } else if (item.type === "function_call") {
      chunks.push(
        event("response.function_call_arguments.delta", {
          item_id: item.id,
          output_index: outputIndex,
          delta: item.arguments,
        }),
        event("response.function_call_arguments.done", {
          item_id: item.id,
          output_index: outputIndex,
          arguments: item.arguments,
        }),
      );
    }

    chunks.push(
      event("response.output_item.done", {
        output_index: outputIndex,
        item,
      }),
    );
  }

  chunks.push(event("response.completed", { response: fullResponse }));
  return chunks;
}

export function responsesApiResponseToChatCompletion(
  requestBody: string,
  responseBody: string,
): ChatCompletion {
  const request = JSON.parse(requestBody) as ResponsesRequest;
  const response = JSON.parse(responseBody) as ResponsesApiResponse;
  const text: string[] = [];
  const toolCalls: CanonicalToolCall[] = [];

  for (const item of response.output) {
    if (item.type === "message" && Array.isArray(item.content)) {
      for (const part of item.content) {
        if (
          isObject(part) &&
          part.type === "output_text" &&
          typeof part.text === "string"
        ) {
          text.push(part.text);
        }
      }
    } else if (
      item.type === "function_call" &&
      typeof item.call_id === "string"
    ) {
      toolCalls.push({
        id: item.call_id,
        type: "function",
        function: {
          name: typeof item.name === "string" ? item.name : "",
          arguments: typeof item.arguments === "string" ? item.arguments : "{}",
        },
      });
    }
  }

  const incompleteReason = response.incomplete_details?.reason;
  return {
    id: response.id,
    object: "chat.completion",
    created: response.created_at,
    model: request.model,
    choices: [
      {
        index: 0,
        message: {
          role: "assistant",
          content: text.length ? text.join("") : null,
          refusal: null,
          ...(toolCalls.length ? { tool_calls: toolCalls } : {}),
        },
        logprobs: null,
        finish_reason:
          incompleteReason === "max_output_tokens"
            ? "length"
            : incompleteReason === "content_filter"
              ? "content_filter"
              : toolCalls.length
                ? "tool_calls"
                : "stop",
      },
    ],
    usage: {
      prompt_tokens: response.usage?.input_tokens ?? 0,
      completion_tokens: response.usage?.output_tokens ?? 0,
      total_tokens: response.usage?.total_tokens ?? 0,
    },
  } as ChatCompletion;
}

export function aggregateResponsesApiSseToResponse(
  body: string,
): ResponsesApiResponse | null {
  let snapshot: ResponsesApiResponse | null = null;
  const output: JsonObject[] = [];
  for (const event of parseSseEvents(body)) {
    if (event.type === "response.completed" && isObject(event.response)) {
      return event.response as ResponsesApiResponse;
    }
    if (event.type === "response.created" && isObject(event.response)) {
      snapshot = event.response as ResponsesApiResponse;
    }
    if (event.type === "response.output_item.done" && isObject(event.item)) {
      output.push(event.item);
    }
  }
  return snapshot ? { ...snapshot, output } : null;
}

function functionToolCalls(message: unknown): CanonicalToolCall[] {
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

function formatSseEvent(type: string, data: unknown): string {
  return `event: ${type}\ndata: ${JSON.stringify(data)}\n\n`;
}

function isObject(value: unknown): value is JsonObject {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}
