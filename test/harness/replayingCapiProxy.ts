/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { retrieveAvailableModels } from "@github/copilot/sdk";
import { existsSync } from "fs";
import { mkdir, readFile, writeFile } from "fs/promises";
import type {
  ChatCompletion,
  ChatCompletionChunk,
  ChatCompletionCreateParamsBase,
  ChatCompletionMessageFunctionToolCall,
  ChatCompletionMessageParam,
} from "openai/resources/chat/completions";
import { ChatCompletionStream } from "openai/resources/chat/completions";
import path from "path";
import yaml from "yaml";
import {
  CapturedExchange,
  CapturingHttpProxy,
  PerformRequestOptions,
} from "./capturingHttpProxy";
import { iife, ShellConfig, sleep } from "./util";

export const workingDirPlaceholder = "${workdir}";
const chatCompletionEndpoint = "/chat/completions";
const shellConfig =
  process.platform === "win32" ? ShellConfig.powerShell : ShellConfig.bash;
const normalizedToolNames = {
  [shellConfig.shellToolName]: "${shell}",
  [shellConfig.readShellToolName]: "${read_shell}",
  [shellConfig.writeShellToolName]: "${write_shell}",
};

/**
 * An HTTP proxy that not only captures HTTP exchanges, but also stores them in a file on disk and
 * replays the stored responses on subsequent runs.
 *
 * This only stores and matches CAPI-provided OpenAI chat completions, not arbitrary HTTP traffic, since
 * the core idea is to store and compare in a normalized form that (1) ignores irrelevant differences (like
 * timestamps, or references to your working directory path) and (2) writes data files in a simple,
 * human-readable format where it's easy to reason about diffs when things change.
 *
 * To avoid leaving stale files around as tests are modified, it stores things on a one-file-per-test basis,
 * which is overwritten on each test run. So for as long as a test exists, its data will be kept up-to-date.
 */
export class ReplayingCapiProxy extends CapturingHttpProxy {
  private state: ReplayingCapiProxyState | null = null;
  private startPromise: Promise<string> | null = null;

  /**
   * If true, cached responses are played back slowly (~ 2KiB/sec). Otherwise streaming responses are sent as fast as possible.
   */
  slowStreaming = false;

  constructor(
    targetUrl: string,
    filePath?: string,
    workDir?: string,
    testInfo?: { file: string; line?: number },
  ) {
    super(targetUrl);

    // If the instantiator wants to supply config up front as ctor params, we can
    // skip the need to do a /config POST before other requests. This only makes
    // sense if the config will be static for the lifetime of the proxy.
    if (filePath && workDir) {
      this.state = { filePath, workDir, testInfo, toolResultNormalizers: [] };
    }
  }

  async start(): Promise<string> {
    return (this.startPromise ??= (async () => {
      await this.loadStoredData();
      return super.start();
    })());
  }

  async updateConfig(config: Partial<ReplayingCapiProxyState>): Promise<void> {
    if (!config.filePath || !config.workDir) {
      throw new Error("filePath and workDir must be provided in config");
    }

    // Since we're about to switch to a new file, write out any captured exchanges
    // Note that the final call to stop() will also write out any remaining exchanges
    if (this.state) {
      await writeCapturesToDisk(this.exchanges, this.state);
    }

    this.state = {
      filePath: config.filePath,
      workDir: config.workDir,
      testInfo: config.testInfo,
      toolResultNormalizers: [],
    };

    this.clearExchanges();
    await this.loadStoredData();
  }

  private async loadStoredData(): Promise<void> {
    if (this.state && existsSync(this.state.filePath)) {
      const content = await readFile(this.state.filePath, "utf-8");
      this.state.storedData = yaml.parse(content) as NormalizedData;
    }
  }

  async stop(skipWritingCache?: boolean): Promise<void> {
    await super.stop();

    if (this.state && !skipWritingCache) {
      await writeCapturesToDisk(this.exchanges, this.state);
    }
  }

  addToolResultNormalizer(
    toolName: string,
    normalizer: (result: string) => string,
  ) {
    if (!this.state) {
      throw new Error(
        "ReplayingCapiProxy not yet initialized. Cannot add tool result normalizer.",
      );
    }

    this.state.toolResultNormalizers.push({ toolName, normalizer });
  }

  override performRequest(options: PerformRequestOptions): void {
    void iife(async () => {
      const commonResponseHeaders = {
        "x-github-request-id": "some-request-id",
      };

      try {
        // Handle /config endpoint for updating proxy configuration
        if (
          options.requestOptions.path === "/config" &&
          options.requestOptions.method === "POST"
        ) {
          await this.updateConfig(JSON.parse(options.body!));
          options.onResponseStart(200, {});
          options.onResponseEnd();
          return;
        }

        // Handle /stop endpoint for stopping the proxy
        if (
          options.requestOptions.path?.startsWith("/stop") &&
          options.requestOptions.method === "POST"
        ) {
          const skipWritingCache = options.requestOptions.path.includes("skipWritingCache=true");
          options.onResponseStart(200, {});
          options.onResponseEnd();
          await this.stop(skipWritingCache);
          process.exit(0);
        }

        // Handle /exchanges endpoint for retrieving captured exchanges
        if (
          options.requestOptions.path === "/exchanges" &&
          options.requestOptions.method === "GET"
        ) {
          const chatCompletionExchanges = this.exchanges.filter(
            (e) => e.request.url === chatCompletionEndpoint,
          );
          const parsedExchanges = await Promise.all(
            chatCompletionExchanges.map((e) =>
              parseHttpExchange(e.request.body, e.response?.body),
            ),
          );
          options.onResponseStart(200, {});
          options.onData(Buffer.from(JSON.stringify(parsedExchanges)));
          options.onResponseEnd();
          return;
        }

        const state = this.state;
        if (!state) {
          throw new Error(
            "ReplayingCapiProxy not yet initialized. Either pass filePath and workDir to the constructor, " +
              "or post configuration to /config before making other HTTP requests.",
          );
        }

        // Handle /models endpoint
        if (
          options.requestOptions.path === "/models" &&
          state.storedData?.models.length
        ) {
          const modelsResponse = createGetModelsResponse(
            state.storedData.models,
          );
          const body = JSON.stringify(modelsResponse);
          const headers = {
            "content-type": "application/json",
            ...commonResponseHeaders,
          };
          options.onResponseStart(200, headers);
          options.onData(Buffer.from(body));
          options.onResponseEnd();
          return;
        }

        // Handle /chat/completions endpoint
        if (
          state.storedData &&
          options.requestOptions.path === chatCompletionEndpoint &&
          options.body
        ) {
          const savedResponse = await findSavedChatCompletionResponse(
            state.storedData,
            options.body,
            state.workDir,
            state.toolResultNormalizers,
          );

          if (savedResponse) {
            const streamingIsRequested =
              options.body &&
              (JSON.parse(options.body) as { stream?: boolean }).stream ===
                true;

            if (streamingIsRequested) {
              const headers = {
                "content-type": "text/event-stream",
                ...commonResponseHeaders,
              };
              options.onResponseStart(200, headers);
              for (const chunk of convertToStreamingResponseChunks(
                savedResponse,
              )) {
                options.onData(
                  Buffer.from(`data: ${JSON.stringify(chunk)}\n\n`),
                );
                if (this.slowStreaming) {
                  await sleep(100);
                }
              }
              options.onData(Buffer.from("data: [DONE]\n\n"));
              options.onResponseEnd();
            } else {
              const body = JSON.stringify(savedResponse);
              const headers = {
                "content-type": "application/json",
                ...commonResponseHeaders,
              };
              options.onResponseStart(200, headers);
              options.onData(Buffer.from(body));
              options.onResponseEnd();
            }

            return;
          }
        }

        // Fallback to normal proxying if no cached response found
        // This implicitly captures the new exchange too
        if (process.env.CI === "true") {
          await emitNoMatchingRequestWarning(
            options,
            state.testInfo,
            state.workDir,
            state.toolResultNormalizers,
          );
        }
        super.performRequest(options);
      } catch (err) {
        options.onError(err as Error | string);
      }
    });
  }
}

async function writeCapturesToDisk(
  exchanges: readonly CapturedExchange[],
  state: ReplayingCapiProxyState,
) {
  const data = await transformHttpExchanges(
    exchanges,
    state.workDir,
    state.toolResultNormalizers,
  );
  if (data.conversations.length > 0) {
    let yamlText = yaml.stringify(data, { lineWidth: 120 });

    // We have to normalize line endings explicitly, because yaml.stringify uses Unix-style even on Windows,
    // and Git will restore the files with CRLF on Windows so they will appear to be changed
    if (process.platform === "win32") {
      yamlText = yamlText.replace(/\r?\n/g, "\r\n");
    }

    await mkdir(path.dirname(state.filePath), { recursive: true });
    await writeFileIfDifferent(state.filePath, yamlText);
  }
}

async function emitNoMatchingRequestWarning(
  options: PerformRequestOptions,
  testInfo: { file: string; line?: number } | undefined,
  workDir: string,
  toolResultNormalizers: ToolResultNormalizer[],
) {
  const parts: string[] = [];
  if (testInfo?.file) parts.push(`file=${testInfo.file}`);
  if (typeof testInfo?.line === "number") parts.push(`line=${testInfo.line}`);
  const header = parts.length ? ` ${parts.join(",")}` : "";
  const normalized = await parseAndNormalizeRequest(
    options.body,
    workDir,
    toolResultNormalizers,
  );
  const normalizedMessages = normalized.conversations[0]?.messages ?? [];
  const warningMessage =
    `No cached response found for ${options.requestOptions.method} ${options.requestOptions.path}. ` +
    `Final message: ${JSON.stringify(
      normalizedMessages[normalizedMessages.length - 1],
    )}`;
  process.stderr.write(`::warning${header}::${warningMessage}\n`);
}

async function findSavedChatCompletionResponse(
  storedData: NormalizedData,
  requestBody: string | undefined,
  workDir: string,
  toolResultNormalizers: ToolResultNormalizer[],
): Promise<ChatCompletion | undefined> {
  // Normalize the incoming request the same way we normalize for caching
  const normalized = await parseAndNormalizeRequest(
    requestBody,
    workDir,
    toolResultNormalizers,
  );
  const requestMessages = normalized.conversations[0]?.messages ?? [];
  const requestModel = normalized.models[0];
  if (!requestModel) {
    throw new Error("Unable to determine model from request");
  }

  // Now find a matching cached conversation (i.e., one for which this request is a prefix)
  for (const conversation of storedData.conversations) {
    const replyIndex = findAssistantIndexAfterPrefix(
      requestMessages,
      conversation.messages,
    );
    if (replyIndex !== undefined) {
      return createOpenAIResponse(
        requestModel,
        conversation.messages,
        replyIndex,
        workDir,
      );
    }
  }

  return undefined;
}

async function parseAndNormalizeRequest(
  requestBody: string | undefined,
  workDir: string,
  toolResultNormalizers: ToolResultNormalizer[],
) {
  const fakeRequest = {
    request: { url: chatCompletionEndpoint, body: requestBody },
  } as CapturedExchange;
  return await transformHttpExchanges(
    [fakeRequest],
    workDir,
    toolResultNormalizers,
  );
}

// Takes raw HTTP traffic and turns it into the normalized form that we store on disk
async function transformHttpExchanges(
  httpExchanges: readonly CapturedExchange[],
  workDir: string,
  toolResultNormalizers: ToolResultNormalizer[],
): Promise<NormalizedData> {
  const chatCompletionExchanges = httpExchanges
    .filter((e) => e.request.url === chatCompletionEndpoint)
    .filter(excludeFailedResponses);
  const allTurns = await Promise.all(
    chatCompletionExchanges.map((e) =>
      transformHttpExchange(e.request.body, e.response?.body),
    ),
  );
  const dedupedExchanges = removePrefixConversations(
    allTurns.map((t) => t.conversation),
  );
  const dedupedModels = new Set(
    allTurns.map((t) => t.model ?? "").filter((m) => !!m),
  );

  normalizeToolCalls(dedupedExchanges, toolResultNormalizers);
  normalizeFilenames(dedupedExchanges, workDir);
  return { models: Array.from(dedupedModels), conversations: dedupedExchanges };
}

function normalizeFilenames(
  conversations: NormalizedConversation[],
  workDir: string,
): void {
  // Replace occurrences of the workDir path with workingDirPlaceholder to avoid diffs due to different test run locations
  // We do so case-insensitively and with both / and \ to cover different OSes
  // We also normalize any slashes in the rest of the path (e.g., C:\my\workdir\path\to\file.txt -> ${workdir}/path/to/file.txt)
  workDir = workDir.replace(/\\/g, "/").replace(/\/+$/, "");
  const escaped = workDir.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const workDirPattern = new RegExp(
    escaped.replace(/\//g, "[\\\\/]+") + "([\\\\/]+[^\\s\"'`,]*)?",
    "gi",
  );
  const workDirReplacer = (_: string, rest?: string) =>
    workingDirPlaceholder + (rest?.replace(/[\\/]+/g, "/") ?? "");

  // Match non-rooted Windows paths like abc\def\something.ext and flip slashes to /
  // We don't need to match absolute paths because the only legit ones should be inside workdir which
  // is handled above. Plus there's nothing we could do to normalize them since we don't know their base.
  const windowsFnPattern =
    /(?<![a-zA-Z0-9_\\])([a-zA-Z0-9_.-]+(?:\\[a-zA-Z0-9_.-]+)+)/g;
  const windowsFnReplacer = (_: string, path: string) =>
    path.replace(/\\/g, "/");

  for (const conv of conversations) {
    for (const msg of conv.messages) {
      if (msg.content) {
        msg.content = msg.content.replace(workDirPattern, workDirReplacer);
        msg.content = msg.content.replace(windowsFnPattern, windowsFnReplacer);
      }
      for (const tc of msg.tool_calls ?? []) {
        if (tc.function?.arguments) {
          tc.function.arguments = tc.function.arguments.replace(
            workDirPattern,
            workDirReplacer,
          );
          tc.function.arguments = tc.function.arguments.replace(
            windowsFnPattern,
            windowsFnReplacer,
          );
        }
      }
    }
  }
}

function normalizeToolCalls(
  conversations: NormalizedConversation[],
  resultNormalizers: ToolResultNormalizer[],
) {
  // We normalize:
  //  - Tool call IDs (mapping from tooluse_rjaaFdJRRhqAZevU_1aBSA etc to toolcall_0, toolcall_1, etc)
  //  - Tool names (e.g., bash/powershell -> ${shell})
  //  - Tool call results that may vary between execution environments
  // This is so that we're not storing random or environment-specific data in snapshots, and so we can
  // still match cached responses even if these details change.
  for (const conv of conversations) {
    const idMap = new Map<string, string>();
    const precedingMessages: NormalizedMessage[] = [];
    let counter = 0;
    for (const msg of conv.messages) {
      for (const tc of msg.tool_calls ?? []) {
        // Normalize ID in tool calls
        idMap.set(tc.id, (tc.id = idMap.get(tc.id) ?? `toolcall_${counter++}`));

        // Normalize name
        const originalToolName = tc.function?.name;
        const normalizedToolName =
          originalToolName && normalizedToolNames[originalToolName];
        if (normalizedToolName) {
          tc.function!.name = normalizedToolName;
        }
      }

      if (msg.role === "tool" && msg.tool_call_id) {
        // Normalize ID in tool results
        msg.tool_call_id = idMap.get(msg.tool_call_id) ?? msg.tool_call_id;

        // Normalize result
        if (msg.content) {
          const precedingToolCall = precedingMessages
            .flatMap((m) => m.tool_calls ?? [])
            .find((tc) => tc.id === msg.tool_call_id);
          if (precedingToolCall) {
            for (const normalizer of resultNormalizers) {
              if (precedingToolCall.function?.name === normalizer.toolName) {
                msg.content = normalizer.normalizer(msg.content);
              }
            }
          }
        }
      }

      precedingMessages.push(msg);
    }
  }
}

// As we capture LLM calls, we see:
// - Request A, response AB
// - Request ABC, response ABCD
// - Request ABCDE, response ABCDEF
// Among these, it's only necessary to keep the longest conversation (ABCDEF) since this contains all
// information from the shorter ones. Avoiding duplication makes it reasonable for humans to reason
// about diffs in the stored conversations when things change.
function removePrefixConversations(
  conversations: NormalizedConversation[],
): NormalizedConversation[] {
  const result = [...conversations];
  for (let i = result.length - 1; i >= 0; i--) {
    for (let j = i - 1; j >= 0; j--) {
      if (isPrefix(result[j].messages, result[i].messages)) {
        result.splice(j, 1);
        i--; // adjust index since we removed an element before current position
      }
    }
  }
  return result;
}

function isPrefix(
  shorter: NormalizedMessage[],
  longer: NormalizedMessage[],
): boolean {
  if (shorter.length >= longer.length) {
    return false;
  }
  return shorter.every(
    (msg, idx) => JSON.stringify(msg) === JSON.stringify(longer[idx]),
  );
}

async function parseHttpExchange(
  requestBody: string,
  responseBody: string | undefined,
): Promise<ParsedHttpExchange> {
  const request = JSON.parse(requestBody) as ChatCompletionCreateParamsBase;
  const response = await parseOpenAIResponse(responseBody);
  return { request, response };
}

// Converts a single HTTP exchange (request + response) into a normalized conversation
async function transformHttpExchange(
  requestBody: string,
  responseBody: string | undefined,
): Promise<{ conversation: NormalizedConversation; model?: string }> {
  const { request, response } = await parseHttpExchange(
    requestBody,
    responseBody,
  );
  const messages = request.messages.map(transformOpenAIRequestMessage);

  if (response?.choices?.length) {
    messages.push(...transformOpenAIResponseChoice(response.choices));
  }

  return { conversation: { messages }, model: request.model };
}

// Transforms a single OpenAI-style outbound request message into normalized form
// We use this to look up whether we already have a cached response for it
function transformOpenAIRequestMessage(
  m: ChatCompletionMessageParam,
): NormalizedMessage {
  let content: string | undefined;
  if (m.role === "system") {
    // System message changes too often to include in snapshots - just store placeholder
    content = "${system}";
  } else if (m.role === "user" && typeof m.content === "string") {
    content = normalizeUserMessage(m.content);
  } else if (m.role === "tool" && typeof m.content === "string") {
    // If it's a JSON tool call result, normalize the whitespace and property ordering
    try {
      content = JSON.stringify(sortJsonKeys(JSON.parse(m.content)));
    } catch {
      content = m.content.trim();
    }
  } else if (typeof m.content === "string") {
    content = m.content;
  }

  const msg: NormalizedMessage = { role: m.role };
  if ("tool_call_id" in m && m.tool_call_id) {
    msg.tool_call_id = m.tool_call_id;
  }
  if (content) msg.content = content;
  if ("tool_calls" in m && m.tool_calls?.length) {
    msg.tool_calls = m.tool_calls.map(transformOpenAIToolCall);
  }
  return msg;
}

function normalizeUserMessage(content: string): string {
  return content
    .replace(/<current_datetime>.*?<\/current_datetime>/g, "")
    .trim();
}

// Transforms a single OpenAI-style inbound response message into normalized form
function transformOpenAIResponseChoice(
  choices: ChatCompletion.Choice[],
): NormalizedMessage[] {
  // Maps each choice to a separate assistant message.
  // This is clearly wrong, since choices are meant to be alternatives (from which the client
  // should pick one). However CAPI frequently returns collections of tool calls as separate choices,
  // and our chat-completion-client.ts logic handles this by treating them as sequential messages.
  // So, we have to do the same thing here.
  return choices.map((choice) => {
    const tool_calls =
      choice.message.tool_calls?.map(transformOpenAIToolCall) ?? [];
    const msg: NormalizedMessage = { role: "assistant" };
    msg.content = choice.message.content ?? undefined;
    msg.refusal = choice.message.refusal ?? undefined;
    if (tool_calls.length) msg.tool_calls = tool_calls;
    return msg;
  });
}

function transformOpenAIToolCall(tc: {
  id: string;
  type: string;
  function?: { name: string; arguments: string };
}): NormalizedToolCall {
  return {
    id: tc.id,
    type: tc.type,
    function: tc.function && {
      name: tc.function.name,
      arguments: normalizeToolCallArguments(tc.function.arguments),
    },
  };
}

function normalizeToolCallArguments(args: string): string {
  if (!args || args.trim() === "") {
    return "{}";
  }
  try {
    return JSON.stringify(JSON.parse(args));
  } catch {
    return args;
  }
}

// Takes raw HTTP response data and turns it into an OpenAI ChatCompletion object, regardless of whether
// it's a streaming or non-streaming response
async function parseOpenAIResponse(
  responseBody: string | undefined,
): Promise<ChatCompletion | undefined> {
  // Check if it's a streaming response (Server-Sent Events format)
  if (responseBody?.startsWith("data:")) {
    const lines = responseBody
      .split("\n")
      .filter((line) => line.startsWith("data:") && !line.includes("[DONE]"));
    const chunks = lines.map(
      (line) => JSON.parse(line.slice(5)) as ChatCompletionChunk,
    );

    // Convert the sequence of chunks into a final ChatCompletion object
    // TODO: Do we need to apply fixCAPIStreamingToolCalling normalization here?
    const readableStream = new ReadableStream({
      async start(controller) {
        for (const chunk of chunks) {
          controller.enqueue(
            new TextEncoder().encode(JSON.stringify(chunk) + "\n"),
          );
        }
        controller.close();
      },
    });

    return await ChatCompletionStream.fromReadableStream(
      readableStream,
    ).finalChatCompletion();
  } else if (responseBody) {
    return JSON.parse(responseBody) as ChatCompletion;
  }
}

// Checks if requestMessages is a prefix of savedMessages,
// and returns the index of the next assistant message if found.
function findAssistantIndexAfterPrefix(
  requestMessages: NormalizedMessage[],
  savedMessages: NormalizedMessage[],
): number | undefined {
  if (requestMessages.length >= savedMessages.length) {
    return undefined;
  }

  for (let i = 0; i < requestMessages.length; i++) {
    const reqMsg = JSON.stringify(requestMessages[i]);
    const savedMsg = JSON.stringify(savedMessages[i]);
    if (reqMsg !== savedMsg) {
      return undefined;
    }
  }

  // The next message after the prefix should be an assistant message
  const nextIndex = requestMessages.length;
  if (
    nextIndex < savedMessages.length &&
    savedMessages[nextIndex].role === "assistant"
  ) {
    return nextIndex;
  }

  return undefined;
}

function expandWorkDir(
  content: string | undefined,
  workDir: string,
  jsonEscape: boolean,
): string | undefined {
  if (!content) {
    return content;
  }

  const workDirValue = jsonEscape
    ? JSON.stringify(workDir).replaceAll('"', "")
    : workDir;
  return content.replace(/\$\{workdir\}/g, workDirValue);
}

function expandToolName(name: string): string {
  for (const [fullName, normalized] of Object.entries(normalizedToolNames)) {
    if (name === normalized) {
      return fullName;
    }
  }

  return name;
}

// Turns a normalized message back into a full OpenAI ChatCompletion that we can replay as a response
function createOpenAIResponse(
  model: string,
  messages: NormalizedMessage[],
  responseStartIndex: number,
  workDir: string,
): ChatCompletion {
  // Here we recreate the strange CAPI/productcode behavior of using multiple choices to represent
  // multiple assistant messages in a row. This is the inverse of the logic in transformOpenAIResponseChoice().
  // So, find all successive assistant messages starting from responseStartIndex.
  const choices: ChatCompletion.Choice[] = [];
  for (
    let index = 0;
    messages[responseStartIndex + index]?.role === "assistant";
    index++
  ) {
    const assistantMessage = messages[responseStartIndex + index];
    const toolCalls = assistantMessage.tool_calls?.map((tc, idx) => ({
      id: tc.id || `call_${idx}`,
      type: "function" as const,
      function: {
        name: expandToolName(tc.function?.name ?? ""),
        arguments: expandWorkDir(tc.function?.arguments, workDir, true) ?? "{}",
      },
    }));

    choices.push({
      index,
      message: {
        role: "assistant",
        content:
          expandWorkDir(assistantMessage.content, workDir, false) ?? null,
        refusal: assistantMessage.refusal ?? null,
        tool_calls: toolCalls,
      },
      finish_reason: toolCalls?.length ? "tool_calls" : "stop",
      logprobs: null,
    });
  }

  return {
    id: "cached-completion",
    object: "chat.completion",
    created: Math.floor(Date.now() / 1000),
    model,
    choices,
    usage: {
      prompt_tokens: 0,
      completion_tokens: 0,
      total_tokens: 0,
    },
  };
}

const STREAM_CHUNK_SIZE = 200;

function convertToStreamingResponseChunks(
  completion: ChatCompletion,
): ChatCompletionChunk[] {
  const choice = completion.choices[0];
  const content = choice.message.content ?? "";
  const toolCalls = choice.message.tool_calls?.filter(
    (tc): tc is ChatCompletionMessageFunctionToolCall => tc.type === "function",
  );

  const makeChunk = (
    delta: ChatCompletionChunk.Choice.Delta,
  ): ChatCompletionChunk => ({
    id: completion.id,
    object: "chat.completion.chunk",
    created: completion.created,
    model: completion.model,
    choices: [{ index: 0, delta, finish_reason: null, logprobs: null }],
  });

  const chunks: ChatCompletionChunk[] = [];

  // Content chunks
  for (let i = 0; i < content.length; i += STREAM_CHUNK_SIZE) {
    chunks.push(
      makeChunk({
        role: "assistant",
        content: content.slice(i, i + STREAM_CHUNK_SIZE),
      }),
    );
  }

  // Tool call argument chunks
  for (const [tcIdx, tc] of (toolCalls ?? []).entries()) {
    const args = tc.function.arguments;
    for (let i = 0; i < args.length; i += STREAM_CHUNK_SIZE) {
      chunks.push(
        makeChunk({
          role: "assistant",
          tool_calls: [
            {
              index: tcIdx,
              id: tc.id,
              type: "function",
              function: {
                name: i === 0 ? tc.function.name : "",
                arguments: args.slice(i, i + STREAM_CHUNK_SIZE),
              },
            },
          ],
        }),
      );
    }
  }

  // Set finish_reason on last chunk
  if (chunks.length === 0) {
    chunks.push(makeChunk({ role: "assistant" }));
  }
  chunks[chunks.length - 1].choices[0].finish_reason = choice.finish_reason;

  return chunks;
}

function createGetModelsResponse(modelIds: string[]): {
  data: Awaited<ReturnType<typeof retrieveAvailableModels>>;
} {
  // Obviously the following might not match any given model. We could track the original responses from /models,
  // but that risks invalidating the caches too frequently and making this unmaintainable. If this approximation
  // turns out to be insufficient, we can tweak the logic here based on known model IDs.
  return {
    data: modelIds.map((id) => ({
      id,
      name: id,
      capabilities: {
        supports: { vision: true },
        limits: { max_context_window_tokens: 128000 },
      },
    })),
  };
}

async function writeFileIfDifferent(filePath: string, contents: string) {
  if (existsSync(filePath)) {
    const existingContents = await readFile(filePath, "utf-8");
    if (existingContents === contents) {
      return;
    }
  }

  await writeFile(filePath, contents, "utf-8");
}

function excludeFailedResponses(exchange: CapturedExchange): boolean {
  const status = exchange.response?.statusCode;
  return status === undefined || (status >= 200 && status < 300);
}

export type ToolResultNormalizer = {
  toolName: string;
  normalizer: (result: string) => string;
};

export type ParsedHttpExchange = {
  request: ChatCompletionCreateParamsBase;
  response: ChatCompletion | undefined;
};

// We want to be able to reuse the proxy across multiple tests, so it needs to be reconfigurable
// and resettable on demand. By holding all state in one place it's easy to manage.
type ReplayingCapiProxyState = {
  filePath: string;
  workDir: string;
  testInfo?: { file: string; line?: number };
  storedData?: NormalizedData | undefined;
  toolResultNormalizers: ToolResultNormalizer[];
};

interface NormalizedToolCall {
  id: string;
  type: string;
  function?: {
    name: string;
    arguments: string;
  };
}

interface NormalizedMessage {
  role: string;
  content?: string;
  refusal?: string;
  tool_calls?: NormalizedToolCall[];
  tool_call_id?: string;
}

interface NormalizedConversation {
  messages: NormalizedMessage[];
}

export interface NormalizedData {
  models: string[];
  conversations: NormalizedConversation[];
}

function sortJsonKeys(obj: unknown): unknown {
  if (Array.isArray(obj)) return obj.map(sortJsonKeys);
  if (obj && typeof obj === "object") {
    return Object.keys(obj)
      .sort()
      .reduce(
        (acc, key) => {
          acc[key] = sortJsonKeys((obj as Record<string, unknown>)[key]);
          return acc;
        },
        {} as Record<string, unknown>,
      );
  }
  return obj;
}
