/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import type { ChatCompletion } from "openai/resources/chat/completions";
import { afterEach, beforeEach, describe, expect, test } from "vitest";
import yaml from "yaml";
import {
  anthropicMessagesRequestToChatCompletion,
  chatCompletionResponseToAnthropicMessage,
  chatCompletionResponseToAnthropicSseChunks,
} from "./anthropicMessagesAdapter";
import {
  chatCompletionResponseToResponsesApiMessage,
  chatCompletionResponseToResponsesApiSseChunks,
  responsesApiRequestToChatCompletion,
} from "./responsesApiAdapter";
import {
  NormalizedData,
  ReplayBackend,
  ReplayingCapiProxy,
} from "./replayingCapiProxy";

type ByokBackend = Exclude<ReplayBackend, "capi">;

const backends: ReplayBackend[] = [
  "capi",
  "anthropic-messages",
  "openai-responses",
  "openai-completions",
];

const endpoints: Record<ReplayBackend, string> = {
  capi: "/chat/completions",
  "anthropic-messages": "/v1/messages",
  "openai-responses": "/responses",
  "openai-completions": "/chat/completions",
};

const models: Record<ReplayBackend, string> = {
  capi: "gpt-4.1",
  "anthropic-messages": "claude-sonnet-4.5",
  "openai-responses": "gpt-4.1",
  "openai-completions": "gpt-4.1",
};

const completionWithTool: ChatCompletion = {
  id: "completion-1",
  object: "chat.completion",
  created: 123,
  model: "test-model",
  choices: [
    {
      index: 0,
      message: {
        role: "assistant",
        content: "Calling a tool",
        refusal: null,
        tool_calls: [
          {
            id: "call-1",
            type: "function",
            function: { name: "lookup", arguments: '{"value":42}' },
          },
        ],
      },
      logprobs: null,
      finish_reason: "tool_calls",
    },
  ],
  usage: {
    prompt_tokens: 10,
    completion_tokens: 5,
    total_tokens: 15,
  },
};

function requestFor(
  backend: ReplayBackend,
  prompt: string,
): Record<string, unknown> {
  const model = models[backend];
  switch (backend) {
    case "anthropic-messages":
      return {
        model,
        system: "Be helpful",
        messages: [{ role: "user", content: prompt }],
        max_tokens: 128,
      };
    case "openai-responses":
      return {
        model,
        instructions: "Be helpful",
        input: [
          {
            type: "message",
            role: "user",
            content: [{ type: "input_text", text: prompt }],
          },
        ],
      };
    case "capi":
    case "openai-completions":
      return {
        model,
        messages: [
          { role: "system", content: "Be helpful" },
          { role: "user", content: prompt },
        ],
      };
  }
}

async function postJson(
  proxyUrl: string,
  endpoint: string,
  body: unknown,
): Promise<Response> {
  return fetch(`${proxyUrl}${endpoint}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
}

describe("Anthropic Messages adapter", () => {
  test("normalizes messages, binary content, and tools", () => {
    const result = JSON.parse(
      anthropicMessagesRequestToChatCompletion(
        JSON.stringify({
          model: "test-model",
          system: [{ type: "text", text: "Be helpful" }],
          messages: [
            {
              role: "user",
              content: [
                { type: "text", text: "Inspect this" },
                {
                  type: "image",
                  source: {
                    type: "base64",
                    media_type: "image/png",
                    data: "AQID",
                  },
                },
              ],
            },
            {
              role: "assistant",
              content: [
                {
                  type: "tool_use",
                  id: "call-1",
                  name: "lookup",
                  input: { value: 42 },
                },
              ],
            },
            {
              role: "user",
              content: [
                {
                  type: "tool_result",
                  tool_use_id: "call-1",
                  content: "found",
                },
              ],
            },
          ],
          tools: [
            {
              name: "lookup",
              description: "Find a value",
              input_schema: { type: "object" },
            },
          ],
          stream: true,
        }),
      ),
    ) as {
      messages: Array<Record<string, unknown>>;
      tools: Array<Record<string, unknown>>;
      stream: boolean;
    };

    expect(result.messages.map((message) => message.role)).toEqual([
      "system",
      "user",
      "assistant",
      "tool",
    ]);
    expect(result.messages[1].content).toEqual([
      { type: "text", text: "Inspect this" },
      {
        type: "image_url",
        image_url: { url: "data:image/png;base64,AQID" },
      },
    ]);
    expect(result.messages[2].tool_calls).toEqual([
      {
        id: "call-1",
        type: "function",
        function: { name: "lookup", arguments: '{"value":42}' },
      },
    ]);
    expect(result.messages[3]).toMatchObject({
      tool_call_id: "call-1",
      content: "found",
    });
    expect(result.tools).toHaveLength(1);
    expect(result.stream).toBe(true);
  });

  test("renders JSON and streaming tool responses", () => {
    const message =
      chatCompletionResponseToAnthropicMessage(completionWithTool);
    expect(message.stop_reason).toBe("tool_use");
    expect(message.usage).toMatchObject({
      cache_creation_input_tokens: null,
      cache_read_input_tokens: null,
    });
    expect(message.content.map((block) => block.type)).toEqual([
      "text",
      "tool_use",
    ]);

    const stream =
      chatCompletionResponseToAnthropicSseChunks(completionWithTool).join("");
    expect(stream).toContain("event: message_start");
    expect(stream).toContain("event: content_block_delta");
    expect(stream).toContain("event: message_stop");
  });

  test("combines tools from multiple canonical choices", () => {
    const secondChoice = structuredClone(completionWithTool.choices[0]);
    secondChoice.message.content = null;
    secondChoice.message.tool_calls![0] = {
      id: "call-2",
      type: "function",
      function: { name: "inspect", arguments: '{"path":"file.txt"}' },
    };

    const message = chatCompletionResponseToAnthropicMessage({
      ...completionWithTool,
      choices: [completionWithTool.choices[0], secondChoice],
    });
    expect(
      message.content
        .filter((block) => block.type === "tool_use")
        .map((block) => block.name),
    ).toEqual(["lookup", "inspect"]);
  });
});

describe("OpenAI Responses adapter", () => {
  test("normalizes messages, binary content, and tools", () => {
    const result = JSON.parse(
      responsesApiRequestToChatCompletion(
        JSON.stringify({
          model: "test-model",
          instructions: "Be helpful",
          input: [
            {
              type: "message",
              role: "user",
              content: [
                { type: "input_text", text: "Inspect this" },
                {
                  type: "input_image",
                  image_url: "data:image/png;base64,AQID",
                },
              ],
            },
            {
              type: "function_call",
              call_id: "call-1",
              name: "lookup",
              arguments: '{"value":42}',
            },
            {
              type: "function_call_output",
              call_id: "call-1",
              output: "found",
            },
          ],
          tools: [
            {
              type: "function",
              name: "lookup",
              parameters: { type: "object" },
            },
          ],
        }),
      ),
    ) as {
      messages: Array<Record<string, unknown>>;
      tools: Array<Record<string, unknown>>;
    };

    expect(result.messages.map((message) => message.role)).toEqual([
      "system",
      "user",
      "assistant",
      "tool",
    ]);
    expect(result.messages[1].content).toEqual([
      { type: "text", text: "Inspect this" },
      {
        type: "image_url",
        image_url: { url: "data:image/png;base64,AQID" },
      },
    ]);
    expect(result.messages[2].tool_calls).toEqual([
      {
        id: "call-1",
        type: "function",
        function: { name: "lookup", arguments: '{"value":42}' },
      },
    ]);
    expect(result.messages[3]).toMatchObject({
      tool_call_id: "call-1",
      content: "found",
    });
    expect(result.tools).toHaveLength(1);
  });

  test("renders JSON and streaming tool responses", () => {
    const response =
      chatCompletionResponseToResponsesApiMessage(completionWithTool);
    const nextResponse =
      chatCompletionResponseToResponsesApiMessage(completionWithTool);
    expect(response).toMatchObject({
      object: "response",
      created_at: completionWithTool.created,
      status: "completed",
      incomplete_details: null,
      error: null,
    });
    expect(response.output[0].id).not.toBe(nextResponse.output[0].id);
    expect(response.output.map((item) => item.type)).toEqual([
      "message",
      "function_call",
    ]);

    const chunks =
      chatCompletionResponseToResponsesApiSseChunks(completionWithTool);
    const events = chunks.map(
      (chunk) =>
        JSON.parse(chunk.split("\ndata: ")[1]) as Record<string, unknown>,
    );
    const stream = chunks.join("");
    expect(stream).toContain("event: response.created");
    expect(stream).toContain("event: response.in_progress");
    expect(stream).toContain("event: response.output_text.delta");
    expect(stream).toContain('"sequence_number":0');
    expect(stream).toContain("event: response.completed");

    expect(events[0]).toMatchObject({
      type: "response.created",
      response: { status: "in_progress", output: [] },
    });
    expect(events[1]).toMatchObject({
      type: "response.in_progress",
      response: { status: "in_progress", output: [] },
    });

    const addedItems = events.filter(
      (event) => event.type === "response.output_item.added",
    );
    expect(addedItems).toMatchObject([
      {
        item: {
          type: "message",
          status: "in_progress",
          content: [],
        },
      },
      {
        item: {
          type: "function_call",
          status: "in_progress",
          arguments: "",
        },
      },
    ]);
    expect(
      events.find((event) => event.type === "response.content_part.added"),
    ).toMatchObject({
      part: { type: "output_text", text: "" },
    });

    const completedItems = events.filter(
      (event) => event.type === "response.output_item.done",
    );
    expect(completedItems).toMatchObject([
      {
        item: {
          type: "message",
          status: "completed",
          content: [{ type: "output_text", text: "Calling a tool" }],
        },
      },
      {
        item: {
          type: "function_call",
          status: "completed",
          arguments: '{"value":42}',
        },
      },
    ]);
  });
});

describe("protocol-aware replay", () => {
  let tempDir: string;
  let workDir: string;
  let cachePath: string;

  async function writeSnapshot(
    messages: NormalizedData["conversations"][number]["messages"],
  ): Promise<void> {
    await writeFile(
      cachePath,
      yaml.stringify({
        models: ["captured-capi-model"],
        conversations: [{ messages }],
      } satisfies NormalizedData),
    );
  }

  async function withProxy(
    backend: ReplayBackend,
    action: (proxyUrl: string) => Promise<void>,
  ): Promise<void> {
    const proxy = new ReplayingCapiProxy(
      "http://localhost:9999",
      cachePath,
      workDir,
    );
    const proxyUrl = await proxy.start();
    await proxy.updateConfig({ filePath: cachePath, workDir, backend });
    try {
      await action(proxyUrl);
    } finally {
      await proxy.stop(true);
    }
  }

  beforeEach(async () => {
    tempDir = await mkdtemp(path.join(os.tmpdir(), "protocol-replay-"));
    workDir = path.join(tempDir, "work");
    cachePath = path.join(tempDir, "cache.yaml");
    await writeSnapshot([
      { role: "system", content: "${system}" },
      { role: "user", content: "Hello" },
      { role: "assistant", content: "Hi there!" },
    ]);
  });

  afterEach(async () => {
    await rm(tempDir, { recursive: true, force: true });
  });

  test.each(backends)(
    "replays one model-independent snapshot through %s",
    async (backend) => {
      await withProxy(backend, async (proxyUrl) => {
        const response = await postJson(
          proxyUrl,
          endpoints[backend],
          requestFor(backend, "Hello"),
        );
        expect(response.status).toBe(200);
        const body = (await response.json()) as Record<string, unknown>;
        expect(body.model).toBe(models[backend]);

        const exchanges = (await (
          await fetch(`${proxyUrl}/exchanges`)
        ).json()) as Array<{
          request: {
            model: string;
            messages: Array<{ role: string; content: unknown }>;
          };
          response?: unknown;
        }>;
        expect(exchanges).toHaveLength(1);
        expect(exchanges[0].request.model).toBe(models[backend]);
        expect(exchanges[0].request.messages.at(-1)).toEqual({
          role: "user",
          content: "Hello",
        });
        if (
          backend === "anthropic-messages" ||
          backend === "openai-responses"
        ) {
          expect(exchanges[0].response).toBeUndefined();
        }
      });
    },
  );

  test("does not rewrite canonical snapshots after BYOK replay", async () => {
    const original = await readFile(cachePath, "utf8");
    const proxy = new ReplayingCapiProxy(
      "http://localhost:9999",
      cachePath,
      workDir,
    );
    const proxyUrl = await proxy.start();
    await proxy.updateConfig({
      filePath: cachePath,
      workDir,
      backend: "openai-responses",
    });

    let stopped = false;
    try {
      const response = await postJson(
        proxyUrl,
        endpoints["openai-responses"],
        requestFor("openai-responses", "Hello"),
      );
      expect(response.status).toBe(200);
      await proxy.stop();
      stopped = true;
      expect(await readFile(cachePath, "utf8")).toBe(original);
    } finally {
      if (!stopped) await proxy.stop(true);
    }
  });

  test.each(["openai-responses", "openai-completions"] as const)(
    "coalesces adjacent user messages from %s",
    async (backend) => {
      await writeSnapshot([
        { role: "system", content: "${system}" },
        { role: "user", content: "Hook context" },
        { role: "user", content: "Hello" },
        { role: "assistant", content: "Hi there!" },
      ]);
      const request = requestFor(backend, "Hello");
      const hook =
        "Hook context\n\n\n<current_datetime>2026-01-01T00:00:00Z</current_datetime>\n\n";
      if (backend === "openai-responses") {
        (request.input as unknown[]).unshift({
          type: "message",
          role: "user",
          content: [{ type: "input_text", text: hook }],
        });
      } else {
        (request.messages as unknown[]).splice(1, 0, {
          role: "user",
          content: hook,
        });
      }

      await withProxy(backend, async (proxyUrl) => {
        const response = await postJson(
          proxyUrl,
          endpoints[backend],
          request,
        );
        expect(response.status).toBe(200);
      });
    },
  );

  test("normalizes Anthropic spacing between adjacent user turns", async () => {
    await writeSnapshot([
      { role: "system", content: "${system}" },
      { role: "user", content: "First prompt" },
      { role: "user", content: "Recovery prompt" },
      { role: "assistant", content: "Recovered" },
    ]);
    await withProxy("anthropic-messages", async (proxyUrl) => {
      const response = await postJson(
        proxyUrl,
        endpoints["anthropic-messages"],
        requestFor(
          "anthropic-messages",
          "First prompt\n\n\n\n\nRecovery prompt",
        ),
      );
      expect(response.status).toBe(200);
    });
  });

  test.each(backends)(
    "replays compaction responses through %s",
    async (backend) => {
      await writeSnapshot([
        { role: "system", content: "${system}" },
        { role: "user", content: "${compaction_prompt}" },
        {
          role: "assistant",
          content:
            "<overview>Compacted</overview><history>History</history><checkpoint_title>Checkpoint</checkpoint_title>",
        },
      ]);
      await withProxy(backend, async (proxyUrl) => {
        const response = await postJson(
          proxyUrl,
          endpoints[backend],
          requestFor(backend, "${compaction_prompt}"),
        );
        expect(response.status).toBe(200);
        const body = JSON.stringify(await response.json());
        expect(body).toContain("<overview>");
        expect(body).toContain("<history>");
        expect(body).toContain("<checkpoint_title>");
      });
    },
  );

  test("rejects an inference request over the wrong protocol", async () => {
    await withProxy("anthropic-messages", async (proxyUrl) => {
      const response = await postJson(
        proxyUrl,
        endpoints["openai-completions"],
        requestFor("openai-completions", "Hello"),
      );
      expect(response.status).toBe(400);
      await expect(response.text()).resolves.toContain("protocol_mismatch");
    });
  });

  test("keeps foreign model endpoints unavailable in CAPI mode", async () => {
    await withProxy("capi", async (proxyUrl) => {
      const response = await postJson(
        proxyUrl,
        endpoints["openai-responses"],
        requestFor("openai-responses", "Hello"),
      );
      expect(response.status).toBe(404);
    });
  });
});
